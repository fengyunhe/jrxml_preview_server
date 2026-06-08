package cn.firegod.pdfpreviewserver.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import cn.firegod.pdfpreviewserver.model.ExportRequest;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportService单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @InjectMocks
    private ExportService exportService;

    private String testJrxml;

    @BeforeEach
    void setUp() {
        // 创建一个简单的测试JRXML
        testJrxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\"\n" +
            "              xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "              xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports\n" +
            "                          http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\"\n" +
            "              name=\"TestReport\"\n" +
            "              pageWidth=\"595\" pageHeight=\"842\"\n" +
            "              columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\"\n" +
            "              topMargin=\"20\" bottomMargin=\"20\">\n" +
            "\n" +
            "    <field name=\"name\" class=\"java.lang.String\"/>\n" +
            "    <field name=\"amount\" class=\"java.lang.Double\"/>\n" +
            "\n" +
            "    <title>\n" +
            "        <band height=\"50\">\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"0\" width=\"200\" height=\"30\"/>\n" +
            "                <text><![CDATA[Test Report]]></text>\n" +
            "            </staticText>\n" +
            "        </band>\n" +
            "    </title>\n" +
            "\n" +
            "    <detail>\n" +
            "        <band height=\"30\">\n" +
            "            <textField>\n" +
            "                <reportElement x=\"0\" y=\"0\" width=\"100\" height=\"30\"/>\n" +
            "                <textFieldExpression><![CDATA[$F{name}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"100\" y=\"0\" width=\"100\" height=\"30\"/>\n" +
            "                <textFieldExpression><![CDATA[$F{amount}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "        </band>\n" +
            "    </detail>\n" +
            "\n" +
            "</jasperReport>";
    }

    @Test
    void testExportToPdf() throws JRException {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "pdf");
        List<Map<String, Object>> dataSource = Arrays.asList(
            createTestData("Product A", 100.0),
            createTestData("Product B", 200.0)
        );
        request.setDataSource(dataSource);

        // 执行导出
        byte[] pdfData = (byte[]) exportService.export(request);

        // 验证结果
        assertNotNull(pdfData, "PDF data should not be null");
        assertTrue(pdfData.length > 0, "PDF data should not be empty");

        // 验证PDF头
        String header = new String(pdfData, 0, Math.min(5, pdfData.length));
        assertTrue(header.startsWith("%PDF"), "Output should be a valid PDF");
    }

    @Test
    void testExportToHtml() throws JRException {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "html");
        List<Map<String, Object>> dataSource = Arrays.asList(
            createTestData("Product A", 100.0)
        );
        request.setDataSource(dataSource);

        // 执行导出
        String htmlContent = (String) exportService.export(request);

        // 验证结果
        assertNotNull(htmlContent, "HTML content should not be null");
        assertTrue(htmlContent.contains("<html"), "Output should contain HTML tag");
        assertTrue(htmlContent.contains("Test Report"), "Output should contain report title");
    }

    @Test
    void testExportToExcel() throws JRException {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "xlsx");
        List<Map<String, Object>> dataSource = Arrays.asList(
            createTestData("Product A", 100.0),
            createTestData("Product B", 200.0)
        );
        request.setDataSource(dataSource);

        // 执行导出
        byte[] excelData = (byte[]) exportService.export(request);

        // 验证结果
        assertNotNull(excelData, "Excel data should not be null");
        assertTrue(excelData.length > 0, "Excel data should not be empty");
    }

    @Test
    void testExportToWord() throws JRException {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "docx");
        List<Map<String, Object>> dataSource = Arrays.asList(
            createTestData("Product A", 100.0)
        );
        request.setDataSource(dataSource);

        // 执行导出
        byte[] wordData = (byte[]) exportService.export(request);

        // 验证结果
        assertNotNull(wordData, "Word data should not be null");
        assertTrue(wordData.length > 0, "Word data should not be empty");
    }

    @Test
    void testExportToCsv() throws JRException {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "csv");
        List<Map<String, Object>> dataSource = Arrays.asList(
            createTestData("Product A", 100.0),
            createTestData("Product B", 200.0)
        );
        request.setDataSource(dataSource);

        // 执行导出
        String csvContent = (String) exportService.export(request);

        // 验证结果
        assertNotNull(csvContent, "CSV content should not be null");
        assertTrue(csvContent.contains("name"), "CSV should contain header");
        assertTrue(csvContent.contains("amount"), "CSV should contain header");
        assertTrue(csvContent.contains("Product A"), "CSV should contain data");
    }

    @Test
    void testExportWithParameters() throws JRException {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "pdf");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Custom Title");
        request.setParameters(parameters);

        // 执行导出
        byte[] pdfData = (byte[]) exportService.export(request);

        // 验证结果
        assertNotNull(pdfData, "PDF data should not be null");
        assertTrue(pdfData.length > 0, "PDF data should not be empty");
    }

    @Test
    void testExportWithEmptyDataSource() throws JRException {
        // 准备测试数据（空数据源）
        ExportRequest request = new ExportRequest(testJrxml, "pdf");
        request.setDataSource(Collections.emptyList());

        // 执行导出
        byte[] pdfData = (byte[]) exportService.export(request);

        // 验证结果
        assertNotNull(pdfData, "PDF data should not be null even with empty data");
    }

    @Test
    void testExportInvalidJrxml() {
        // 准备无效的JRXML
        ExportRequest request = new ExportRequest("invalid jrxml", "pdf");

        // 执行导出并验证异常
        assertThrows(Exception.class, () -> {
            exportService.export(request);
        }, "Should throw exception for invalid JRXML");
    }

    @Test
    void testExportUnsupportedFormat() {
        // 准备测试数据
        ExportRequest request = new ExportRequest(testJrxml, "unsupported");

        // 执行导出并验证异常
        assertThrows(Exception.class, () -> {
            exportService.export(request);
        }, "Should throw exception for unsupported format");
    }

    /**
     * 创建测试数据
     */
    private Map<String, Object> createTestData(String name, double amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("amount", amount);
        return data;
    }
}
