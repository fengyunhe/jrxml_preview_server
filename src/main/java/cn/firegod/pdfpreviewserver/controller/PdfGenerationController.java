package cn.firegod.pdfpreviewserver.controller;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.component.Component;
import net.sf.jasperreports.components.table.TableComponent;
import net.sf.jasperreports.components.table.BaseColumn;
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
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/pdf")
public class PdfGenerationController {

    // 静态数据源存储，供JRXML表达式直接引用（必须public，因为表达式编译为独立类）
    public static final Map<String, JRDataSource> SUB_DATASOURCES = new ConcurrentHashMap<>();
    private static int dsCounter = 0;

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
            // 0. 预处理JRXML内容，修复常见格式问题
            String jrxmlContent = fixJrxmlContent(request.getJrxmlContent());

            // 1. 加载JRXML内容，确保使用UTF-8编码
            JasperDesign jasperDesign = JRXmlLoader.load(new ByteArrayInputStream(jrxmlContent.getBytes("UTF-8")));

            // 2. 准备参数和数据源
            Map<String, Object> parameters = request.getParameters() != null ? request.getParameters() : new HashMap<>();

            // 3. 为表格设置子数据源（在编译前修改JasperDesign）
            Map<String, List<Map<String, Object>>> subDataSources = request.getSubDataSources();
            System.out.println("=== subDataSources接收情况 ===");
            System.out.println("subDataSources是否为null: " + (subDataSources == null));
            if (subDataSources != null) {
                System.out.println("subDataSources是否为空: " + subDataSources.isEmpty());
                System.out.println("subDataSources keys: " + subDataSources.keySet());
                for (Map.Entry<String, List<Map<String, Object>>> e : subDataSources.entrySet()) {
                    System.out.println("  " + e.getKey() + " -> " + e.getValue().size() + "行");
                    if (!e.getValue().isEmpty()) {
                        System.out.println("  第一行数据: " + e.getValue().get(0));
                    }
                }
            }
            if (subDataSources != null && !subDataSources.isEmpty()) {
                applySubDataSources(jasperDesign, parameters, subDataSources);
            }

            // 打印applySubDataSources后的parameters中subDS_开头的参数
            System.out.println("=== applySubDataSources后的subDS参数 ===");
            for (Map.Entry<String, Object> pe : parameters.entrySet()) {
                if (pe.getKey().startsWith("subDS_")) {
                    System.out.println("  参数 " + pe.getKey() + " = " + pe.getValue().getClass().getSimpleName());
                }
            }

            // 4. 修复JRXML中可能存在的null表达式，避免编译时NPE
            sanitizeNullExpressions(jasperDesign);

            // 诊断：打印所有dataset的参数/字段/变量，以及所有表达式引用
            dumpCompilationContext(jasperDesign);

            // 诊断：模拟编译器过滤逻辑，检查哪些参数/字段/变量在过滤后不可用
            simulateCompilationFilter(jasperDesign);

            // 诊断：打印JRXML内容前500字符便于分析
            System.out.println("=== JRXML内容(前500字符) ===");
            System.out.println(jrxmlContent.substring(0, Math.min(500, jrxmlContent.length())));
            System.out.println("=== JRXML内容结束 ===");

            // 5. 编译JRXML为JasperReport
            JasperReport jasperReport;
            try {
                jasperReport = JasperCompileManager.compileReport(jasperDesign);
            } catch (NullPointerException npe) {
                // 捕获JRClassGenerator内部NPE，提供更多上下文
                System.err.println("=== 编译时NPE，诊断信息 ===");
                System.err.println("Report名称: " + jasperDesign.getName());
                System.err.println("Report参数数: " + jasperDesign.getParametersMap().size());
                System.err.println("Report参数列表: " + jasperDesign.getParametersMap().keySet());
                // 打印NPE的完整堆栈
                npe.printStackTrace(System.err);
                // 遍历所有表达式找出问题
                dumpAllExpressions(jasperDesign);
                throw new JRException("编译失败：JRXML中存在引用了不存在的参数/字段/变量的表达式，请检查JRXML内容", npe);
            } catch (net.sf.jasperreports.engine.JRException compileEx) {
                // 检查是否是因为invalidReport导致的编译失败
                String designName = jasperDesign.getName();
                if ("invalidReport".equals(designName) || "null".equals(designName)) {
                    throw new JRException("JRXML解析失败：内容包含无效的XML结构，请检查JRXML格式是否正确", compileEx);
                }
                throw compileEx;
            }

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
     * 预处理JRXML内容，修复常见的格式问题。
     * 常见问题：rotation属性放在了reportElement上（应该在textField/image等具体元素上）。
     */
    private String fixJrxmlContent(String jrxmlContent) {
        if (jrxmlContent == null) return jrxmlContent;
        String fixed = jrxmlContent;

        // 修复1: rotation属性在reportElement上（JasperReports不允许）
        // rotation是textField/image等元素的属性，不是reportElement的属性
        // 从<reportElement ... rotation="..." ...>中移除rotation属性
        int originalLength = fixed.length();
        fixed = fixed.replaceAll("(<reportElement\\b[^>]*?)\\s+rotation\\s*=\\s*\"[^\"]*\"", "$1");
        fixed = fixed.replaceAll("(<reportElement\\b[^>]*?)\\s*rotation\\s*=\\s*'[^']*'", "$1");
        // 也处理rotation在最前面的情况
        fixed = fixed.replaceAll("(<reportElement\\b[^>]*?)rotation\\s*=\\s*\"[^\"]*\"\\s*", "$1");

        if (fixed.length() != originalLength) {
            System.out.println("[fixJrxml] 修复了reportElement上的rotation属性");
        }

        return fixed;
    }

    /**
     * 修复JasperDesign中所有null表达式，防止编译时JRClassGenerator NPE。
     * Jaspersoft Studio生成的JRXML中，某些表达式元素的text属性可能为null，
     * 导致JasperCompileManager.compileReport()在JRClassGenerator.generateExpression()处NPE。
     */
    private void sanitizeNullExpressions(JasperDesign jasperDesign) {
        // 修复所有dataset中的表达式
        for (JRDataset dataset : jasperDesign.getDatasets()) {
            if (dataset instanceof JRDesignDataset) {
                JRDesignDataset ds = (JRDesignDataset) dataset;
                // 修复variables中的表达式
                if (ds.getVariablesMap() != null) {
                    for (JRVariable variable : ds.getVariablesMap().values()) {
                        sanitizeExpression(variable.getExpression());
                        sanitizeExpression(variable.getInitialValueExpression());
                    }
                }
                // 修复dataset级别的filterExpression
                sanitizeExpression(ds.getFilterExpression());
            }
        }

        // getAllBands()返回所有band（title, pageHeader, columnHeader, detail,
        // columnFooter, pageFooter, summary, noData），无需单独处理
        for (JRBand band : jasperDesign.getAllBands()) {
            if (band == null) continue;
            sanitizeBandExpressions(band);
        }

        System.out.println("[sanitize] null表达式修复完成");
    }

    /**
     * 修复单个band中所有元素的表达式。
     */
    private void sanitizeBandExpressions(JRBand band) {
        if (band == null) return;
        for (JRElement element : band.getElements()) {
            // 修复所有元素的printWhenExpression和styleExpression
            sanitizeExpression(element.getPrintWhenExpression());
            sanitizeExpression(element.getStyleExpression());

            // 修复文本字段的表达式
            if (element instanceof JRDesignTextField) {
                JRDesignTextField tf = (JRDesignTextField) element;
                sanitizeExpression(tf.getExpression());
                sanitizeExpression(tf.getPatternExpression());
            }
            // 修复图像的表达式
            if (element instanceof JRDesignImage) {
                JRDesignImage img = (JRDesignImage) element;
                sanitizeExpression(img.getExpression());
                sanitizeExpression(img.getAnchorNameExpression());
            }
            // 修复图表的表达式
            if (element instanceof JRDesignChart) {
                JRDesignChart chart = (JRDesignChart) element;
                sanitizeExpression(chart.getTitleExpression());
                sanitizeExpression(chart.getSubtitleExpression());
                sanitizeExpression(chart.getAnchorNameExpression());
                sanitizeExpression(chart.getBookmarkLevelExpression());
            }
            // 处理componentElement（如TableComponent、ListComponent等）
            if (element instanceof JRDesignComponentElement) {
                Component comp = ((JRDesignComponentElement) element).getComponent();
                if (comp instanceof TableComponent) {
                    TableComponent table = (TableComponent) comp;
                    JRDatasetRun run = table.getDatasetRun();
                    if (run instanceof JRDesignDatasetRun) {
                        JRDesignDatasetRun designRun = (JRDesignDatasetRun) run;
                        sanitizeExpression(designRun.getConnectionExpression());
                        sanitizeExpression(designRun.getDataSourceExpression());
                    }
                }
            }
            // 处理subreport元素
            if (element instanceof JRDesignSubreport) {
                JRDesignSubreport sub = (JRDesignSubreport) element;
                sanitizeExpression(sub.getConnectionExpression());
                sanitizeExpression(sub.getDataSourceExpression());
                sanitizeExpression(sub.getExpression());
            }
        }
    }

    /**
     * 如果表达式对象存在但text为null，设置为空字符串。
     */
    private void sanitizeExpression(JRExpression expression) {
        if (expression == null) return;
        if (expression instanceof JRDesignExpression) {
            JRDesignExpression expr = (JRDesignExpression) expression;
            if (expr.getText() == null) {
                expr.setText("");
                System.out.println("[sanitize] 修复了null表达式 (valueClass=" + expr.getValueClassName() + ")");
            }
        }
    }

    /**
     * NPE时调用：遍历report的所有表达式，检查每个chunk的引用是否有效。
     * 包括report-level bands（title/detail/pageHeader等）和所有dataset。
     */
    private void dumpAllExpressions(JasperDesign jasperDesign) {
        System.out.println("=== 全量表达式检查 ===");
        Map<String, JRParameter> reportParams = jasperDesign.getParametersMap();

        // 主dataset的字段和变量
        JRDesignDataset mainDs = jasperDesign.getMainDesignDataset();
        Map<String, JRField> mainFields = new HashMap<>();
        Map<String, JRVariable> mainVars = new HashMap<>();
        Map<String, JRParameter> mainParams = new HashMap<>(reportParams);
        if (mainDs != null) {
            for (JRField f : mainDs.getFieldsList()) {
                mainFields.put(f.getName(), f);
            }
            mainVars = mainDs.getVariablesMap() != null ? mainDs.getVariablesMap() : new HashMap<>();
            for (JRParameter p : mainDs.getParametersList()) {
                mainParams.put(p.getName(), p);
            }
        }

        System.out.println("[check] 主Dataset可用参数: " + mainParams.keySet());
        System.out.println("[check] 主Dataset可用字段: " + mainFields.keySet());
        System.out.println("[check] 主Dataset可用变量: " + mainVars.keySet());

        // 检查report-level bands（title, pageHeader, columnHeader, detail, columnFooter, pageFooter, summary, noData）
        checkBandExpressions(jasperDesign.getTitle(), mainParams, mainFields, mainVars, "title");
        checkBandExpressions(jasperDesign.getPageHeader(), mainParams, mainFields, mainVars, "pageHeader");
        checkBandExpressions(jasperDesign.getColumnHeader(), mainParams, mainFields, mainVars, "columnHeader");
        checkBandExpressions(jasperDesign.getDetailSection() != null ? jasperDesign.getDetailSection().getBands() : null, mainParams, mainFields, mainVars, "detail");
        checkBandExpressions(jasperDesign.getColumnFooter(), mainParams, mainFields, mainVars, "columnFooter");
        checkBandExpressions(jasperDesign.getPageFooter(), mainParams, mainFields, mainVars, "pageFooter");
        checkBandExpressions(jasperDesign.getSummary(), mainParams, mainFields, mainVars, "summary");
        checkBandExpressions(jasperDesign.getNoData(), mainParams, mainFields, mainVars, "noData");

        // 检查主dataset的group header/footer
        if (mainDs != null) {
            for (JRGroup group : mainDs.getGroups()) {
                checkSectionExpressions(group.getGroupHeaderSection(), mainParams, mainFields, mainVars, "groupHeader:" + group.getName());
                checkSectionExpressions(group.getGroupFooterSection(), mainParams, mainFields, mainVars, "groupFooter:" + group.getName());
            }
        }

        // 检查所有TableComponent中的cell表达式（这些表达式编译在dataset上下文中）
        for (JRBand band : jasperDesign.getAllBands()) {
            if (band == null) continue;
            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignComponentElement) {
                    Component comp = ((JRDesignComponentElement) element).getComponent();
                    if (comp instanceof TableComponent) {
                        TableComponent table = (TableComponent) comp;
                        JRDatasetRun run = table.getDatasetRun();
                        String dsName = run != null ? run.getDatasetName() : "unknown";

                        // 获取该dataset的参数/字段/变量
                        Map<String, JRParameter> tableParams = new HashMap<>(reportParams);
                        Map<String, JRField> tableFields = new HashMap<>();
                        Map<String, JRVariable> tableVars = new HashMap<>();
                        for (JRDataset ds : jasperDesign.getDatasets()) {
                            if (ds.getName() != null && ds.getName().equals(dsName)) {
                                for (JRParameter p : ds.getParameters()) {
                                    tableParams.put(p.getName(), p);
                                }
                                for (JRField f : ds.getFields()) {
                                    tableFields.put(f.getName(), f);
                                }
                                if (ds.getVariables() != null) {
                                    for (JRVariable v : ds.getVariables()) {
                                        tableVars.put(v.getName(), v);
                                    }
                                }
                                break;
                            }
                        }

                        System.out.println("[check] TableComponent细胞表达式 (dataset=" + dsName + "):");
                        System.out.println("  可用参数: " + tableParams.keySet());
                        System.out.println("  可用字段: " + tableFields.keySet());
                        System.out.println("  可用变量: " + tableVars.keySet());

                        // 检查所有column的cell
                        checkTableExpressions(table, tableParams, tableFields, tableVars, dsName);
                    }
                }
            }
        }

        // 检查其他dataset
        for (JRDataset dataset : jasperDesign.getDatasets()) {
            if (!(dataset instanceof JRDesignDataset)) continue;
            JRDesignDataset ds = (JRDesignDataset) dataset;
            if (mainDs != null && ds.getName().equals(mainDs.getName())) continue;

            Map<String, JRField> dsFields = new HashMap<>();
            for (JRField f : ds.getFieldsList()) {
                dsFields.put(f.getName(), f);
            }
            Map<String, JRVariable> dsVars = ds.getVariablesMap() != null ? ds.getVariablesMap() : new HashMap<>();
            Map<String, JRParameter> dsAllParams = new HashMap<>(reportParams);
            for (JRParameter p : ds.getParametersList()) {
                dsAllParams.put(p.getName(), p);
            }

            System.out.println("[check] Dataset [" + ds.getName() + "] 参数: " + dsAllParams.keySet());
            System.out.println("[check] Dataset [" + ds.getName() + "] 字段: " + dsFields.keySet());
            System.out.println("[check] Dataset [" + ds.getName() + "] 变量: " + dsVars.keySet());

            if (ds.getVariablesMap() != null) {
                for (JRVariable var : ds.getVariablesMap().values()) {
                    validateExpression(var.getExpression(), dsAllParams, dsFields, dsVars, ds.getName(), "var:" + var.getName());
                    validateExpression(var.getInitialValueExpression(), dsAllParams, dsFields, dsVars, ds.getName(), "varInit:" + var.getName());
                }
            }
            validateExpression(ds.getFilterExpression(), dsAllParams, dsFields, dsVars, ds.getName(), "filter");

            for (JRGroup group : ds.getGroups()) {
                checkSectionExpressions(group.getGroupHeaderSection(), dsAllParams, dsFields, dsVars, ds.getName() + "/groupHeader:" + group.getName());
                checkSectionExpressions(group.getGroupFooterSection(), dsAllParams, dsFields, dsVars, ds.getName() + "/groupFooter:" + group.getName());
            }
        }

        System.out.println("=== 全量表达式检查完成 ===");
    }

    /**
     * 检查单个band的表达式。
     */
    private void checkBandExpressions(JRBand band, Map<String, JRParameter> params,
                                      Map<String, JRField> fields, Map<String, JRVariable> vars,
                                      String location) {
        if (band == null) return;
        for (JRElement element : band.getElements()) {
            validateElementExpressions(element, params, fields, vars, location);
        }
    }

    /**
     * 检查band数组的表达式（用于detailSection等）。
     */
    private void checkBandExpressions(JRBand[] bands, Map<String, JRParameter> params,
                                      Map<String, JRField> fields, Map<String, JRVariable> vars,
                                      String location) {
        if (bands == null) return;
        for (JRBand band : bands) {
            checkBandExpressions(band, params, fields, vars, location);
        }
    }

    /**
     * 检查TableComponent中所有cell的表达式。
     * Table cell的表达式编译在dataset上下文中，需要使用dataset的参数/字段/变量。
     */
    private void checkTableExpressions(TableComponent table, Map<String, JRParameter> params,
                                       Map<String, JRField> fields, Map<String, JRVariable> vars,
                                       String dsName) {
        String loc = "table/" + dsName;
        // 检查datasetRun表达式
        JRDatasetRun run = table.getDatasetRun();
        if (run instanceof JRDesignDatasetRun) {
            JRDesignDatasetRun dr = (JRDesignDatasetRun) run;
            validateExpression(dr.getConnectionExpression(), params, fields, vars, dsName, loc + "/connExpr");
            validateExpression(dr.getDataSourceExpression(), params, fields, vars, dsName, loc + "/dsExpr");
        }
        // 检查Row的printWhenExpression
        checkTableRow(table.getTableHeader(), params, fields, vars, dsName, loc + "/tableHeader");
        checkTableRow(table.getTableFooter(), params, fields, vars, dsName, loc + "/tableFooter");
        checkTableRow(table.getColumnHeader(), params, fields, vars, dsName, loc + "/columnHeader");
        checkTableRow(table.getColumnFooter(), params, fields, vars, dsName, loc + "/columnFooter");
        checkTableRow(table.getDetail(), params, fields, vars, dsName, loc + "/detail");
        checkTableGroupRows(table.getGroupHeaders(), params, fields, vars, dsName, loc + "/groupHeaders");
        checkTableGroupRows(table.getGroupFooters(), params, fields, vars, dsName, loc + "/groupFooters");
        checkTableCell(table.getNoData(), params, fields, vars, dsName, loc + "/noData");
        // 检查所有column的cell
        for (BaseColumn column : table.getColumns()) {
            validateExpression(column.getPrintWhenExpression(), params, fields, vars, dsName, loc + "/colPrintWhen");
            checkTableCell(column.getTableHeader(), params, fields, vars, dsName, loc + "/colTableHeader");
            checkTableCell(column.getTableFooter(), params, fields, vars, dsName, loc + "/colTableFooter");
            checkTableCell(column.getColumnHeader(), params, fields, vars, dsName, loc + "/colColumnHeader");
            checkTableCell(column.getColumnFooter(), params, fields, vars, dsName, loc + "/colColumnFooter");
            // detail cell
            if (column instanceof net.sf.jasperreports.components.table.Column) {
                checkTableCell(((net.sf.jasperreports.components.table.Column) column).getDetailCell(),
                    params, fields, vars, dsName, loc + "/colDetailCell");
            }
            // group cells
            if (column.getGroupHeaders() != null) {
                for (net.sf.jasperreports.components.table.GroupCell gc : column.getGroupHeaders()) {
                    checkTableCell(gc.getCell(), params, fields, vars, dsName, loc + "/colGroupHeader");
                }
            }
            if (column.getGroupFooters() != null) {
                for (net.sf.jasperreports.components.table.GroupCell gc : column.getGroupFooters()) {
                    checkTableCell(gc.getCell(), params, fields, vars, dsName, loc + "/colGroupFooter");
                }
            }
        }
    }

    private void checkTableRow(net.sf.jasperreports.components.table.Row row,
                               Map<String, JRParameter> params, Map<String, JRField> fields,
                               Map<String, JRVariable> vars, String dsName, String location) {
        if (row == null) return;
        validateExpression(row.getPrintWhenExpression(), params, fields, vars, dsName, location);
    }

    private void checkTableGroupRows(java.util.List<net.sf.jasperreports.components.table.GroupRow> groupRows,
                                     Map<String, JRParameter> params, Map<String, JRField> fields,
                                     Map<String, JRVariable> vars, String dsName, String location) {
        if (groupRows == null) return;
        for (net.sf.jasperreports.components.table.GroupRow gr : groupRows) {
            checkTableRow(gr.getRow(), params, fields, vars, dsName, location + "/" + gr.getGroupName());
        }
    }

    private void checkTableCell(net.sf.jasperreports.components.table.BaseCell cell,
                                Map<String, JRParameter> params, Map<String, JRField> fields,
                                Map<String, JRVariable> vars, String dsName, String location) {
        if (cell == null) return;
        JRElement[] elements = cell.getElements();
        if (elements != null) {
            for (JRElement element : elements) {
                validateElementExpressions(element, params, fields, vars, location);
            }
        }
    }

    /**
     * 诊断并修复所有表达式中的无效引用。
     */
    private void dumpCompilationContext(JasperDesign jasperDesign) {
        System.out.println("=== 编译上下文诊断 ===");

        Map<String, JRParameter> reportParams = jasperDesign.getParametersMap();
        System.out.println("[diagnostic] Report参数 (" + reportParams.size() + "): " + reportParams.keySet());

        // 主dataset
        JRDesignDataset mainDs = jasperDesign.getMainDesignDataset();
        if (mainDs != null) {
            Map<String, JRField> mainFields = new HashMap<>();
            for (JRField f : mainDs.getFieldsList()) {
                mainFields.put(f.getName(), f);
            }
            Map<String, JRVariable> mainVars = mainDs.getVariablesMap() != null ? mainDs.getVariablesMap() : new HashMap<>();

            Map<String, JRParameter> allParams = new HashMap<>(reportParams);
            for (JRParameter p : mainDs.getParametersList()) {
                allParams.put(p.getName(), p);
            }

            System.out.println("[diagnostic] 主Dataset参数合并后 (" + allParams.size() + "): " + allParams.keySet());
            System.out.println("[diagnostic] 主Dataset字段: " + mainFields.keySet());
            System.out.println("[diagnostic] 主Dataset变量: " + mainVars.keySet());

            // 遍历主dataset的group来检查表达式
            for (JRGroup group : mainDs.getGroups()) {
                checkSectionExpressions(group.getGroupHeaderSection(), allParams, mainFields, mainVars, "main/groupHeader/" + group.getName());
                checkSectionExpressions(group.getGroupFooterSection(), allParams, mainFields, mainVars, "main/groupFooter/" + group.getName());
            }
        }

        // 其他dataset
        for (JRDataset dataset : jasperDesign.getDatasets()) {
            if (!(dataset instanceof JRDesignDataset)) continue;
            JRDesignDataset ds = (JRDesignDataset) dataset;
            if (mainDs != null && ds.getName().equals(mainDs.getName())) continue;

            Map<String, JRField> dsFields = new HashMap<>();
            for (JRField f : ds.getFieldsList()) {
                dsFields.put(f.getName(), f);
            }
            Map<String, JRVariable> dsVars = ds.getVariablesMap() != null ? ds.getVariablesMap() : new HashMap<>();
            Map<String, JRParameter> allParams = new HashMap<>(reportParams);
            for (JRParameter p : ds.getParametersList()) {
                allParams.put(p.getName(), p);
            }

            System.out.println("[diagnostic] Dataset [" + ds.getName() + "]:");
            System.out.println("  参数: " + allParams.keySet());
            System.out.println("  字段: " + dsFields.keySet());
            System.out.println("  变量: " + dsVars.keySet());

            if (ds.getVariablesMap() != null) {
                for (JRVariable var : ds.getVariablesMap().values()) {
                    validateExpression(var.getExpression(), allParams, dsFields, dsVars, ds.getName(), "var:" + var.getName());
                    validateExpression(var.getInitialValueExpression(), allParams, dsFields, dsVars, ds.getName(), "varInit:" + var.getName());
                }
            }
            validateExpression(ds.getFilterExpression(), allParams, dsFields, dsVars, ds.getName(), "filter");

            for (JRGroup group : ds.getGroups()) {
                checkSectionExpressions(group.getGroupHeaderSection(), allParams, dsFields, dsVars, ds.getName() + "/groupHeader/" + group.getName());
                checkSectionExpressions(group.getGroupFooterSection(), allParams, dsFields, dsVars, ds.getName() + "/groupFooter/" + group.getName());
            }
        }

        System.out.println("=== 编译上下文诊断完成 ===");
    }

    /**
     * 检查JRSection中所有band的表达式。
     */
    private void checkSectionExpressions(JRSection section, Map<String, JRParameter> params,
                                         Map<String, JRField> fields, Map<String, JRVariable> vars,
                                         String location) {
        if (section == null) return;
        for (JRBand band : section.getBands()) {
            if (band == null) continue;
            for (JRElement element : band.getElements()) {
                validateElementExpressions(element, params, fields, vars, location);
            }
        }
    }

    /**
     * 检查单个元素中所有表达式的引用是否有效。
     */
    private void validateElementExpressions(JRElement element, Map<String, JRParameter> params,
                                            Map<String, JRField> fields, Map<String, JRVariable> vars,
                                            String dsName) {
        String prefix = "[validate:" + dsName + "] " + element.getKey();

        // 检查printWhenExpression
        validateExpression(element.getPrintWhenExpression(), params, fields, vars, dsName, prefix + " printWhen");

        if (element instanceof JRDesignTextField) {
            JRDesignTextField tf = (JRDesignTextField) element;
            validateExpression(tf.getExpression(), params, fields, vars, dsName, prefix + " textField");
        }
        if (element instanceof JRDesignImage) {
            JRDesignImage img = (JRDesignImage) element;
            validateExpression(img.getExpression(), params, fields, vars, dsName, prefix + " image");
        }
        if (element instanceof JRDesignChart) {
            JRDesignChart chart = (JRDesignChart) element;
            validateExpression(chart.getTitleExpression(), params, fields, vars, dsName, prefix + " chartTitle");
            validateExpression(chart.getSubtitleExpression(), params, fields, vars, dsName, prefix + " chartSubtitle");
            validateExpression(chart.getAnchorNameExpression(), params, fields, vars, dsName, prefix + " chartAnchor");
        }
        // TableComponent的datasetRun表达式
        if (element instanceof JRDesignComponentElement) {
            Component comp = ((JRDesignComponentElement) element).getComponent();
            if (comp instanceof TableComponent) {
                JRDatasetRun run = ((TableComponent) comp).getDatasetRun();
                if (run instanceof JRDesignDatasetRun) {
                    JRDesignDatasetRun dr = (JRDesignDatasetRun) run;
                    validateExpression(dr.getConnectionExpression(), params, fields, vars, dsName, prefix + " tableConnExpr");
                    validateExpression(dr.getDataSourceExpression(), params, fields, vars, dsName, prefix + " tableDSExpr");
                }
            }
        }
        // Subreport表达式
        if (element instanceof JRDesignSubreport) {
            JRDesignSubreport sub = (JRDesignSubreport) element;
            validateExpression(sub.getConnectionExpression(), params, fields, vars, dsName, prefix + " subConnExpr");
            validateExpression(sub.getDataSourceExpression(), params, fields, vars, dsName, prefix + " subDSExpr");
            validateExpression(sub.getExpression(), params, fields, vars, dsName, prefix + " subExpr");
        }
    }

    /**
     * 检查单个表达式中所有chunk的引用是否有效。
     * 如果发现无效引用，自动补声明参数。
     */
    private void validateExpression(JRExpression expression, Map<String, JRParameter> params,
                                    Map<String, JRField> fields, Map<String, JRVariable> vars,
                                    String dsName, String location) {
        if (expression == null) return;
        JRExpressionChunk[] chunks = expression.getChunks();
        if (chunks == null) return;

        for (JRExpressionChunk chunk : chunks) {
            String chunkText = chunk.getText();
            if (chunkText == null || chunkText.isEmpty()) continue;

            switch (chunk.getType()) {
                case JRExpressionChunk.TYPE_PARAMETER:
                    if (!params.containsKey(chunkText)) {
                        System.out.println("[validate] ❌ 缺失参数: " + chunkText
                            + " (在 " + location + " 中引用, dataset=" + dsName + ")");
                        System.out.println("[validate]    当前参数列表: " + params.keySet());
                    }
                    break;
                case JRExpressionChunk.TYPE_FIELD:
                    if (!fields.containsKey(chunkText)) {
                        System.out.println("[validate] ❌ 缺失字段: " + chunkText
                            + " (在 " + location + " 中引用, dataset=" + dsName + ")");
                        System.out.println("[validate]    当前字段列表: " + fields.keySet());
                    }
                    break;
                case JRExpressionChunk.TYPE_VARIABLE:
                    if (!vars.containsKey(chunkText)) {
                        System.out.println("[validate] ❌ 缺失变量: " + chunkText
                            + " (在 " + location + " 中引用, dataset=" + dsName + ")");
                        System.out.println("[validate]    当前变量列表: " + vars.keySet());
                    }
                    break;
            }
        }
    }

    /**
     * 模拟JasperReports编译器的过滤逻辑，检查每个dataset的表达式编译上下文。
     * 编译器只将实际被表达式引用的参数/字段/变量包含在编译单元中。
     */
    private void simulateCompilationFilter(JasperDesign jasperDesign) {
        System.out.println("=== 模拟编译过滤 ===");
        Map<String, JRParameter> reportParams = jasperDesign.getParametersMap();

        for (JRDataset dataset : jasperDesign.getDatasets()) {
            if (!(dataset instanceof JRDesignDataset)) continue;
            JRDesignDataset ds = (JRDesignDataset) dataset;

            // 1. 收集该dataset的所有表达式（模拟JRExpressionCollector）
            java.util.List<JRExpression> allExprs = new java.util.ArrayList<>();
            // dataset级别的表达式
            if (ds.getVariablesMap() != null) {
                for (JRVariable var : ds.getVariablesMap().values()) {
                    addExprToList(allExprs, var.getExpression());
                    addExprToList(allExprs, var.getInitialValueExpression());
                }
            }
            addExprToList(allExprs, ds.getFilterExpression());

            // band表达式（对主dataset）
            if (ds.isMainDataset()) {
                for (JRBand band : jasperDesign.getAllBands()) {
                    if (band == null) continue;
                    collectBandExprs(allExprs, band);
                }
            }

            // table cell表达式（对table dataset）
            if (!ds.isMainDataset()) {
                for (JRBand band : jasperDesign.getAllBands()) {
                    if (band == null) continue;
                    for (JRElement element : band.getElements()) {
                        if (element instanceof JRDesignComponentElement) {
                            Component comp = ((JRDesignComponentElement) element).getComponent();
                            if (comp instanceof TableComponent) {
                                TableComponent table = (TableComponent) comp;
                                JRDatasetRun run = table.getDatasetRun();
                                if (run != null && ds.getName().equals(run.getDatasetName())) {
                                    // 收集cell表达式
                                    for (BaseColumn column : table.getColumns()) {
                                        collectTableCellExprs(allExprs, column.getTableHeader());
                                        collectTableCellExprs(allExprs, column.getTableFooter());
                                        collectTableCellExprs(allExprs, column.getColumnHeader());
                                        collectTableCellExprs(allExprs, column.getColumnFooter());
                                        if (column instanceof net.sf.jasperreports.components.table.Column) {
                                            collectTableCellExprs(allExprs,
                                                ((net.sf.jasperreports.components.table.Column) column).getDetailCell());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. 收集被引用的参数名（模拟collectExpressionsIncluded）
            java.util.Set<String> usedParams = new java.util.HashSet<>();
            java.util.Set<String> usedFields = new java.util.HashSet<>();
            java.util.Set<String> usedVars = new java.util.HashSet<>();
            for (JRExpression expr : allExprs) {
                if (expr == null) continue;
                JRExpressionChunk[] chunks = expr.getChunks();
                if (chunks == null) continue;
                for (JRExpressionChunk chunk : chunks) {
                    String text = chunk.getText();
                    if (text == null || text.isEmpty()) continue;
                    switch (chunk.getType()) {
                        case JRExpressionChunk.TYPE_PARAMETER: usedParams.add(text); break;
                        case JRExpressionChunk.TYPE_FIELD: usedFields.add(text); break;
                        case JRExpressionChunk.TYPE_VARIABLE: usedVars.add(text); break;
                    }
                }
            }

            // 3. 构建dataset的完整参数map（包含built-in参数）
            java.util.Map<String, JRParameter> dsParams = new java.util.HashMap<>(reportParams);
            for (JRParameter p : ds.getParametersList()) {
                dsParams.put(p.getName(), p);
            }
            java.util.Map<String, JRField> dsFields = new java.util.HashMap<>();
            for (JRField f : ds.getFieldsList()) {
                dsFields.put(f.getName(), f);
            }
            java.util.Map<String, JRVariable> dsVars = ds.getVariablesMap() != null
                ? new java.util.HashMap<>(ds.getVariablesMap()) : new java.util.HashMap<>();

            // 4. 过滤后可用的参数（模拟ReportSourceCompilation过滤）
            java.util.Map<String, JRParameter> filteredParams = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, JRParameter> e : dsParams.entrySet()) {
                if (usedParams.contains(e.getKey())) {
                    filteredParams.put(e.getKey(), e.getValue());
                }
            }
            java.util.Map<String, JRField> filteredFields = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, JRField> e : dsFields.entrySet()) {
                if (usedFields.contains(e.getKey())) {
                    filteredFields.put(e.getKey(), e.getValue());
                }
            }
            java.util.Map<String, JRVariable> filteredVars = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, JRVariable> e : dsVars.entrySet()) {
                if (usedVars.contains(e.getKey())) {
                    filteredVars.put(e.getKey(), e.getValue());
                }
            }

            System.out.println("[simFilter] Dataset [" + ds.getName() + "] (isMain=" + ds.isMainDataset() + "):");
            System.out.println("  表达式数: " + allExprs.size());
            System.out.println("  使用的参数: " + usedParams);
            System.out.println("  使用的字段: " + usedFields);
            System.out.println("  使用的变量: " + usedVars);
            System.out.println("  过滤后参数: " + filteredParams.keySet());
            System.out.println("  过滤后字段: " + filteredFields.keySet());
            System.out.println("  过滤后变量: " + filteredVars.keySet());

            // 5. 检查每个表达式的引用是否在过滤后的map中
            for (JRExpression expr : allExprs) {
                if (expr == null) continue;
                JRExpressionChunk[] chunks = expr.getChunks();
                if (chunks == null) continue;
                String exprText = expr.getText();
                for (JRExpressionChunk chunk : chunks) {
                    String text = chunk.getText();
                    if (text == null || text.isEmpty()) continue;
                    switch (chunk.getType()) {
                        case JRExpressionChunk.TYPE_PARAMETER:
                            if (!filteredParams.containsKey(text)) {
                                System.out.println("[simFilter] ❌ 编译会失败! 参数 [" + text
                                    + "] 被引用但不在过滤后参数map中 (expr=" + exprText + ")");
                            }
                            break;
                        case JRExpressionChunk.TYPE_FIELD:
                            if (!filteredFields.containsKey(text)) {
                                System.out.println("[simFilter] ❌ 编译会失败! 字段 [" + text
                                    + "] 被引用但不在过滤后字段map中 (expr=" + exprText + ")");
                            }
                            break;
                        case JRExpressionChunk.TYPE_VARIABLE:
                            if (!filteredVars.containsKey(text)) {
                                System.out.println("[simFilter] ❌ 编译会失败! 变量 [" + text
                                    + "] 被引用但不在过滤后变量map中 (expr=" + exprText + ")");
                            }
                            break;
                    }
                }
            }
        }
        System.out.println("=== 模拟编译过滤完成 ===");
    }

    private void addExprToList(java.util.List<JRExpression> list, JRExpression expr) {
        if (expr != null) list.add(expr);
    }

    private void collectBandExprs(java.util.List<JRExpression> list, JRBand band) {
        if (band == null) return;
        for (JRElement element : band.getElements()) {
            addExprToList(list, element.getPrintWhenExpression());
            addExprToList(list, element.getStyleExpression());
            if (element instanceof JRDesignTextField) {
                addExprToList(list, ((JRDesignTextField) element).getExpression());
                addExprToList(list, ((JRDesignTextField) element).getPatternExpression());
            }
            if (element instanceof JRDesignImage) {
                addExprToList(list, ((JRDesignImage) element).getExpression());
            }
            if (element instanceof JRDesignSubreport) {
                addExprToList(list, ((JRDesignSubreport) element).getConnectionExpression());
                addExprToList(list, ((JRDesignSubreport) element).getDataSourceExpression());
                addExprToList(list, ((JRDesignSubreport) element).getExpression());
            }
            if (element instanceof JRDesignComponentElement) {
                Component comp = ((JRDesignComponentElement) element).getComponent();
                if (comp instanceof TableComponent) {
                    JRDatasetRun run = ((TableComponent) comp).getDatasetRun();
                    if (run instanceof JRDesignDatasetRun) {
                        JRDesignDatasetRun dr = (JRDesignDatasetRun) run;
                        addExprToList(list, dr.getConnectionExpression());
                        addExprToList(list, dr.getDataSourceExpression());
                    }
                }
            }
        }
    }

    private void collectTableCellExprs(java.util.List<JRExpression> list,
                                       net.sf.jasperreports.components.table.BaseCell cell) {
        if (cell == null) return;
        JRElement[] elements = cell.getElements();
        if (elements != null) {
            for (JRElement element : elements) {
                addExprToList(list, element.getPrintWhenExpression());
                addExprToList(list, element.getStyleExpression());
                if (element instanceof JRDesignTextField) {
                    addExprToList(list, ((JRDesignTextField) element).getExpression());
                    addExprToList(list, ((JRDesignTextField) element).getPatternExpression());
                }
                if (element instanceof JRDesignImage) {
                    addExprToList(list, ((JRDesignImage) element).getExpression());
                }
            }
        }
    }

    /**
     * 为表格元素设置子数据源。
     * 使用静态Map存储数据源，表达式直接引用静态字段，绕过JasperReports参数作用域限制。
     *
     * 为什么不能用参数表达式？
     * - 自定义参数($P{subDS_xxx})：TableCompiler将dataSourceExpression收集到主collector
     *   而非dataset collector，导致编译器参数过滤器不包含自定义参数 → "Parameter not found"
     * - REPORT_DATA_SOURCE参数：fillReport()会用主数据源覆盖该参数的值 → 子数据源丢失
     * - 静态字段引用：绕过所有参数解析问题，表达式编译为简单的静态方法调用
     */
    private void applySubDataSources(JasperDesign jasperDesign, Map<String, Object> parameters,
                                      Map<String, List<Map<String, Object>>> subDataSources) {
        System.out.println("=== 开始处理子数据源 ===");

        Map<String, JRDataset> datasetsByName = jasperDesign.getDatasetMap();
        System.out.println("JRXML中定义的数据集: " + datasetsByName.keySet());

        int tableCount = 0;
        for (JRBand band : jasperDesign.getAllBands()) {
            if (band == null) continue;
            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignComponentElement) {
                    JRDesignComponentElement compElement = (JRDesignComponentElement) element;
                    Component component = compElement.getComponent();
                    if (component instanceof TableComponent) {
                        tableCount++;
                        TableComponent table = (TableComponent) component;
                        JRDatasetRun datasetRun = table.getDatasetRun();
                        if (datasetRun == null) continue;

                        String datasetName = datasetRun.getDatasetName();
                        System.out.println("[策略] 找到TableComponent #" + tableCount
                            + ", datasetName=[" + datasetName + "]");

                        if (datasetName != null && subDataSources.containsKey(datasetName)) {
                            List<Map<String, Object>> rows = subDataSources.get(datasetName);

                            // 将数据源存入静态Map，生成唯一key
                            String staticKey = "ds_" + (dsCounter++);
                            JRDataSource ds = new JRBeanCollectionDataSource(rows);
                            SUB_DATASOURCES.put(staticKey, ds);
                            System.out.println("[策略] 已注册静态数据源 key=" + staticKey + ", 行数: " + rows.size());
                            if (!rows.isEmpty()) {
                                System.out.println("[策略] 第一行: " + rows.get(0));
                            }

                            if (datasetRun instanceof net.sf.jasperreports.engine.design.JRDesignDatasetRun) {
                                net.sf.jasperreports.engine.design.JRDesignDatasetRun designRun =
                                    (net.sf.jasperreports.engine.design.JRDesignDatasetRun) datasetRun;

                                // connectionExpression设为null（JasperReports编译器要求）
                                designRun.setConnectionExpression(null);

                                // dataSourceExpression引用静态Map，绕过参数解析
                                String className = PdfGenerationController.class.getName();
                                JRDesignExpression expr = new JRDesignExpression(
                                    "((net.sf.jasperreports.engine.JRDataSource)"
                                    + className + ".SUB_DATASOURCES.get(\"" + staticKey + "\"))");
                                expr.setValueClassName("net.sf.jasperreports.engine.JRDataSource");
                                designRun.setDataSourceExpression(expr);
                                System.out.println("[策略] 已设置dataSourceExpression为静态引用: " + className + ".SUB_DATASOURCES.get(\"" + staticKey + "\")");
                            }

                            // 清除query（使用数据源而非数据库查询）
                            if (datasetsByName.containsKey(datasetName)) {
                                JRDataset dataset = datasetsByName.get(datasetName);
                                if (dataset instanceof net.sf.jasperreports.engine.design.JRDesignDataset) {
                                    net.sf.jasperreports.engine.design.JRDesignDataset designDataset =
                                        (net.sf.jasperreports.engine.design.JRDesignDataset) dataset;
                                    if (designDataset.getQuery() != null) {
                                        designDataset.setQuery(null);
                                        System.out.println("[策略] 已清除query");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (tableCount == 0) {
            System.out.println("[策略] 未找到任何TableComponent");
        }
        System.out.println("=== 子数据源处理完成 ===");
    }
}
