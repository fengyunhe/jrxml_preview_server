package cn.firegod.pdfpreviewserver.service;

import cn.firegod.pdfpreviewserver.model.ExportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportService单元测试
 * 注意：此测试不依赖字体配置，可以独立运行
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @InjectMocks
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        // 初始化
    }

    @Test
    void testExportRequestCreation() {
        // 测试ExportRequest模型创建
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");

        assertEquals("<jasperReport/>", request.getJrxml());
        assertEquals("pdf", request.getFormat());
    }

    @Test
    void testExportRequestWithParameters() {
        // 测试参数传递
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");

        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_TITLE", "Test Report");
        params.put("REPORT_DATE", new Date());
        request.setParameters(params);

        assertNotNull(request.getParameters());
        assertEquals("Test Report", request.getParameters().get("REPORT_TITLE"));
    }

    @Test
    void testExportRequestWithDataSource() {
        // 测试数据源传递
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");

        List<Map<String, Object>> dataSource = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Product A");
        row1.put("amount", 100.00);
        dataSource.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("name", "Product B");
        row2.put("amount", 200.00);
        dataSource.add(row2);

        request.setDataSource(dataSource);

        assertNotNull(request.getDataSource());
        assertEquals(2, request.getDataSource().size());
        assertEquals("Product A", request.getDataSource().get(0).get("name"));
    }

    @Test
    void testExportRequestWithSubDataSources() {
        // 测试子数据源传递
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");

        Map<String, List<Map<String, Object>>> subDataSources = new HashMap<>();

        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> tableRow = new HashMap<>();
        tableRow.put("column1", "Value 1");
        tableRow.put("column2", "Value 2");
        tableData.add(tableRow);

        subDataSources.put("tableDataset", tableData);
        request.setSubDataSources(subDataSources);

        assertNotNull(request.getSubDataSources());
        assertTrue(request.getSubDataSources().containsKey("tableDataset"));
        assertEquals(1, request.getSubDataSources().get("tableDataset").size());
    }

    @Test
    void testExportOptions() {
        // 测试导出选项
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");

        ExportRequest.ExportOptions options = new ExportRequest.ExportOptions();
        options.setEmbedImages(true);
        options.setEncoding("UTF-8");
        options.setLocale("zh_CN");

        request.setOptions(options);

        assertNotNull(request.getOptions());
        assertTrue(request.getOptions().isEmbedImages());
        assertEquals("UTF-8", request.getOptions().getEncoding());
        assertEquals("zh_CN", request.getOptions().getLocale());
    }

    @Test
    void testMultipleExportFormats() {
        // 测试多种导出格式
        String[] formats = {"pdf", "html", "excel", "word", "csv", "rtf"};

        for (String format : formats) {
            ExportRequest request = new ExportRequest();
            request.setJrxml("<jasperReport/>");
            request.setFormat(format);

            assertEquals(format, request.getFormat());
        }
    }

    @Test
    void testNullParametersHandling() {
        // 测试空参数处理
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");
        request.setParameters(null);
        request.setDataSource(null);
        request.setSubDataSources(null);

        assertNull(request.getParameters());
        assertNull(request.getDataSource());
        assertNull(request.getSubDataSources());
    }

    @Test
    void testComplexNestedDataSource() {
        // 测试复杂嵌套数据源
        ExportRequest request = new ExportRequest();
        request.setJrxml("<jasperReport/>");
        request.setFormat("pdf");

        Map<String, List<Map<String, Object>>> subDataSources = new HashMap<>();

        // 多个子数据源
        for (int i = 0; i < 5; i++) {
            List<Map<String, Object>> tableData = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                Map<String, Object> row = new HashMap<>();
                row.put("table" + i + "_col" + j, "Value " + j);
                tableData.add(row);
            }
            subDataSources.put("dataset_" + i, tableData);
        }

        request.setSubDataSources(subDataSources);

        assertEquals(5, request.getSubDataSources().size());
        for (int i = 0; i < 5; i++) {
            assertEquals(10, request.getSubDataSources().get("dataset_" + i).size());
        }
    }

    @Test
    void testRequestBuilder() {
        // 测试构建器模式
        ExportRequest request = new ExportRequest(
            "<jasperReport><field name=\"id\" class=\"java.lang.Integer\"/></jasperReport>",
            "pdf"
        );

        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_TITLE", "Test");

        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        data.add(row);

        request.setParameters(params);
        request.setDataSource(data);

        assertNotNull(request.getJrxml());
        assertEquals("pdf", request.getFormat());
        assertNotNull(request.getParameters());
        assertNotNull(request.getDataSource());
    }
}
