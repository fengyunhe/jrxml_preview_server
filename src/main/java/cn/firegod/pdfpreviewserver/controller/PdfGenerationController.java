package cn.firegod.pdfpreviewserver.controller;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.components.table.TableComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.firegod.pdfpreviewserver.model.PdfGenerationRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfGenerationController {

    @PostMapping(value = "/generateForm")
    public ResponseEntity<Object> generatePdfFromJrxmlForm(
            String jrxml, String parameters, String dataSource, String subDataSources) {
        PdfGenerationRequest request = new PdfGenerationRequest();
        request.setJrxmlContent(jrxml);

        // 解析parameters参数
        if (parameters != null && !parameters.isEmpty()) {
            try {
                org.json.JSONObject jsonObject = new org.json.JSONObject(parameters);
                Map<String, Object> paramsMap = new HashMap<>();
                for (String key : jsonObject.keySet()) {
                    paramsMap.put(key, jsonObject.get(key));
                }
                request.setParameters(paramsMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 解析dataSource参数
        if (dataSource != null && !dataSource.isEmpty()) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(dataSource);
                List<Map<String, Object>> dataList = new java.util.ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Map<String, Object> dataMap = new HashMap<>();
                    for (String key : jsonObject.keySet()) {
                        dataMap.put(key, jsonObject.get(key));
                    }
                    dataList.add(dataMap);
                }
                request.setDataSource(dataList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 解析subDataSources参数（表格数据源）
        if (subDataSources != null && !subDataSources.isEmpty()) {
            try {
                org.json.JSONObject sdsObj = new org.json.JSONObject(subDataSources);
                Map<String, List<Map<String, Object>>> subDataSourcesMap = new HashMap<>();
                for (String key : sdsObj.keySet()) {
                    org.json.JSONArray arr = sdsObj.getJSONArray(key);
                    List<Map<String, Object>> rows = new java.util.ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject rowObj = arr.getJSONObject(i);
                        Map<String, Object> row = new HashMap<>();
                        for (String col : rowObj.keySet()) {
                            row.put(col, rowObj.get(col));
                        }
                        rows.add(row);
                    }
                    subDataSourcesMap.put(key, rows);
                }
                request.setSubDataSources(subDataSourcesMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.generatePdfFromJrxmlWithRequest(request);
    }

    @PostMapping(value = "/generate", consumes = "text/plain")
    public ResponseEntity<Object> generatePdfFromJrxml(@RequestBody String jrxmlContent) {
        PdfGenerationRequest request = new PdfGenerationRequest();
        request.setJrxmlContent(jrxmlContent);
        return this.generatePdfFromJrxmlWithRequest(request);
    }

    @PostMapping(value = "/generate", consumes = "application/json")
    public ResponseEntity<Object> generatePdfFromJrxml(@RequestBody PdfGenerationRequest request) {
        return this.generatePdfFromJrxmlWithRequest(request);
    }

    private ResponseEntity<Object> generatePdfFromJrxmlWithRequest(PdfGenerationRequest request) {
        try {
            // 1. 加载JRXML内容，确保使用UTF-8编码
            JasperDesign jasperDesign = JRXmlLoader.load(new ByteArrayInputStream(request.getJrxmlContent().getBytes("UTF-8")));

            // 2. 准备参数和数据源
            Map<String, Object> parameters = request.getParameters() != null ? request.getParameters() : new HashMap<>();

            // 3. 为表格设置子数据源（在编译前修改JasperDesign）
            Map<String, List<Map<String, Object>>> subDataSources = request.getSubDataSources();
            if (subDataSources != null && !subDataSources.isEmpty()) {
                applySubDataSources(jasperDesign, parameters, subDataSources);
            }

            // 4. 编译JRXML为JasperReport
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            // 5. 准备报告级数据源
            JRDataSource dataSource;
            List<Map<String, Object>> dataList = request.getDataSource();
            if (dataList != null && !dataList.isEmpty()) {
                dataSource = new JRBeanCollectionDataSource(dataList);
            } else {
                dataSource = new JREmptyDataSource();
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // 6. 生成PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);

            // 7. 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=report.pdf");
            headers.setContentLength(outputStream.size());

            headers.add("X-Frame-Options", "SAMEORIGIN");
            headers.add("Content-Security-Policy", "frame-ancestors *");
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "*");

            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "PDF生成失败");
            errorResponse.put("message", e.getMessage());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 为表格元素设置子数据源。
     * 遍历所有band中的表格组件，如果其dataset名称匹配subDataSources中的key，
     * 则设置dataSourceExpression引用对应的参数。
     */
    private void applySubDataSources(JasperDesign jasperDesign, Map<String, Object> parameters,
                                      Map<String, List<Map<String, Object>>> subDataSources) {
        System.out.println("=== 开始处理子数据源 ===");
        System.out.println("subDataSources keys: " + subDataSources.keySet());

        // 遍历所有band
        for (JRBand band : jasperDesign.getAllBands()) {
            if (band == null) continue;
            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignComponentElement) {
                    JRDesignComponentElement compElement = (JRDesignComponentElement) element;
                    Component component = compElement.getComponent();
                    if (component instanceof TableComponent) {
                        TableComponent table = (TableComponent) component;
                        JRDatasetRun datasetRun = table.getDatasetRun();
                        System.out.println("找到表格组件，datasetRun: " + datasetRun);
                        if (datasetRun != null) {
                            String datasetName = datasetRun.getDatasetName();
                            System.out.println("datasetName: " + datasetName);
                            System.out.println("是否在subDataSources中: " + subDataSources.containsKey(datasetName));
                            if (datasetName != null && subDataSources.containsKey(datasetName)) {
                                // 创建参数名
                                String paramName = "subDS_" + datasetName;

                                // 创建数据源并放入参数
                                List<Map<String, Object>> rows = subDataSources.get(datasetName);
                                JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
                                parameters.put(paramName, ds);
                                System.out.println("设置参数 " + paramName + "，数据行数: " + rows.size());

                                // 设置dataSourceExpression引用该参数
                                // datasetRun是JRDatasetRun接口，在design阶段实际是JRDesignDatasetRun
                                if (datasetRun instanceof net.sf.jasperreports.engine.design.JRDesignDatasetRun) {
                                    net.sf.jasperreports.engine.design.JRDesignDatasetRun designRun =
                                        (net.sf.jasperreports.engine.design.JRDesignDatasetRun) datasetRun;
                                    JRDesignExpression expr = new JRDesignExpression("$P{" + paramName + "}");
                                    expr.setValueClassName("net.sf.jasperreports.engine.JRDataSource");
                                    designRun.setDataSourceExpression(expr);
                                    // 移除connectionExpression，确保使用dataSourceExpression
                                    designRun.setConnectionExpression(null);
                                    System.out.println("已设置dataSourceExpression，移除connectionExpression");
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("=== 子数据源处理完成 ===");
    }
}
