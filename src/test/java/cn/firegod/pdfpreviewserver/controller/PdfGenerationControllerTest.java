package cn.firegod.pdfpreviewserver.controller;

import cn.firegod.pdfpreviewserver.model.PdfGenerationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
public class PdfGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 简单的有效的JRXML内容
    private static final String VALID_JRXML = "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" name=\"sample\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\" isIgnorePagination=\"true\"><title><band height=\"50\" splitType=\"Stretch\"><staticText><reportElement x=\"0\" y=\"0\" width=\"555\" height=\"50\"/><textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"><font size=\"24\" isBold=\"true\"/></textElement><text><![CDATA[测试报表]]></text></staticText></band></title><summary><band height=\"100\" splitType=\"Stretch\"><staticText><reportElement x=\"0\" y=\"0\" width=\"555\" height=\"100\"/><textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"><font size=\"14\"/></textElement><text><![CDATA[这是一个简单的测试报表]]></text></staticText></band></summary></jasperReport>";

    // 带参数的有效的JRXML内容
    private static final String VALID_JRXML_WITH_PARAM = "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" name=\"sample\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\" isIgnorePagination=\"true\"><parameter name=\"reportTitle\" class=\"java.lang.String\"/><title><band height=\"50\" splitType=\"Stretch\"><textField><reportElement x=\"0\" y=\"0\" width=\"555\" height=\"50\"/><textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"><font size=\"24\" isBold=\"true\"/></textElement><textFieldExpression class=\"java.lang.String\"><![CDATA[$P{reportTitle}]]></textFieldExpression></textField></band></title><summary><band height=\"100\" splitType=\"Stretch\"><staticText><reportElement x=\"0\" y=\"0\" width=\"555\" height=\"100\"/><textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"><font size=\"14\"/></textElement><text><![CDATA[这是一个简单的测试报表]]></text></staticText></band></summary></jasperReport>";
    
    // 带动态字段的有效的JRXML内容
    private static final String VALID_JRXML_WITH_FIELDS = "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" name=\"sample\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\" isIgnorePagination=\"true\"><field name=\"id\" class=\"java.lang.Integer\"/><field name=\"name\" class=\"java.lang.String\"/><field name=\"value\" class=\"java.lang.Double\"/><title><band height=\"50\" splitType=\"Stretch\"><staticText><reportElement x=\"0\" y=\"0\" width=\"555\" height=\"50\"/><textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"><font size=\"24\" isBold=\"true\"/></textElement><text><![CDATA[动态字段测试报表]]></text></staticText></band></title><detail><band height=\"30\" splitType=\"Stretch\"><textField><reportElement x=\"0\" y=\"0\" width=\"100\" height=\"30\"/><textElement textAlignment=\"Center\" verticalAlignment=\"Middle\"/><textFieldExpression class=\"java.lang.Integer\"><![CDATA[$F{id}]]></textFieldExpression></textField><textField><reportElement x=\"100\" y=\"0\" width=\"200\" height=\"30\"/><textElement textAlignment=\"Left\" verticalAlignment=\"Middle\"/><textFieldExpression class=\"java.lang.String\"><![CDATA[$F{name}]]></textFieldExpression></textField><textField><reportElement x=\"300\" y=\"0\" width=\"100\" height=\"30\"/><textElement textAlignment=\"Right\" verticalAlignment=\"Middle\"/><textFieldExpression class=\"java.lang.Double\"><![CDATA[$F{value}]]></textFieldExpression></textField></band></detail></jasperReport>";

    // 无效的JRXML内容（缺少必要的根元素）
    private static final String INVALID_JRXML = "<invalidReport>这是一个无效的JRXML内容</invalidReport>";

    @Test
    public void testGeneratePdfFromJrxmlTextPlain() throws Exception {
        // 测试使用text/plain格式发送有效的JRXML内容
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generate")
                .contentType(MediaType.TEXT_PLAIN)
                .content(VALID_JRXML))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "inline; filename=report.pdf"));
    }

    @Test
    public void testGeneratePdfFromJrxmlJson() throws Exception {
        // 测试使用application/json格式发送有效的JRXML内容
        PdfGenerationRequest request = new PdfGenerationRequest();
        request.setJrxmlContent(VALID_JRXML);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jrxmlContent\": \"" + VALID_JRXML.replace("\"", "\\\"").replace("\n", "\\n") + "\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "inline; filename=report.pdf"));
    }

    @Test
    public void testGeneratePdfFromJrxmlWithParams() throws Exception {
        // 测试使用application/json格式发送带参数的JRXML内容
        Map<String, Object> params = new HashMap<>();
        params.put("reportTitle", "测试标题");

        PdfGenerationRequest request = new PdfGenerationRequest();
        request.setJrxmlContent(VALID_JRXML_WITH_PARAM);
        request.setParameters(params);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jrxmlContent\": \"" + VALID_JRXML_WITH_PARAM.replace("\"", "\\\"").replace("\n", "\\n") + "\", \"parameters\": {\"reportTitle\": \"测试标题\"}}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    public void testGeneratePdfFromJrxmlWithDataSource() throws Exception {
        // 测试使用application/json格式发送带数据源的JRXML内容
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jrxmlContent\": \"" + VALID_JRXML.replace("\"", "\\\"").replace("\n", "\\n") + "\", \"dataSource\": [{\"id\": 1, \"name\": \"测试项1\"}, {\"id\": 2, \"name\": \"测试项2\"}]}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    public void testGeneratePdfFromJrxmlInvalidContent() throws Exception {
        // 测试使用text/plain格式发送无效的JRXML内容
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generate")
                .contentType(MediaType.TEXT_PLAIN)
                .content(INVALID_JRXML))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("PDF生成失败"));
    }

    @Test
    public void testGeneratePdfFromJrxmlForm() throws Exception {
        // 测试generatePdfFromJrxmlForm端点
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generateForm")
                .param("jrxml", VALID_JRXML))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "inline; filename=report.pdf"));
    }
    
    @Test
    public void testGeneratePdfFromJrxmlWithFields() throws Exception {
        // 测试使用application/json格式发送带动态字段的JRXML内容和相应的数据源
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jrxmlContent\": \"" + VALID_JRXML_WITH_FIELDS.replace("\"", "\\\"").replace("\n", "\\n") + "\", \"dataSource\": [{\"id\": 1, \"name\": \"测试项1\", \"value\": 123.45}, {\"id\": 2, \"name\": \"测试项2\", \"value\": 678.90}, {\"id\": 3, \"name\": \"测试项3\", \"value\": 543.21}]}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF));
        // 把输出的pdf放到当前目录下

    }
    
    @Test
    public void testGeneratePdfFromJrxmlFormWithParamsAndDataSource() throws Exception {
        // 测试generateForm端点传递报表参数和数据字段
        String parametersJson = "{\"reportTitle\": \"表单测试标题\"}";
        String dataSourceJson = "[{\"id\": 1, \"name\": \"表单测试项1\", \"value\": 123.45}, {\"id\": 2, \"name\": \"表单测试项2\", \"value\": 678.90}]";
        
        // 使用带reportTitle参数的JRXML模板
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pdf/generateForm")
                .param("jrxml", VALID_JRXML_WITH_PARAM)
                .param("parameters", parametersJson)
                .param("dataSource", dataSourceJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "inline; filename=report.pdf"));
    }
}