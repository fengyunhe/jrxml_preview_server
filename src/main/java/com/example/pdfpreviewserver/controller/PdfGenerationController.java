package com.example.pdfpreviewserver.controller;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfGenerationController {

    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> generatePdfFromJrxml(@RequestBody String jrxmlContent) {
        try {
            // 1. 加载JRXML内容，确保使用UTF-8编码
            JasperDesign jasperDesign = JRXmlLoader.load(new ByteArrayInputStream(jrxmlContent.getBytes("UTF-8")));
            
            // 2. 编译JRXML为JasperReport
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            
            // 3. 准备参数和数据源
            Map<String, Object> parameters = new HashMap<>();
            
            // 设置字体映射，支持中文显示
            // parameters.put(JRParameter.REPORT_LOCALE, Locale.CHINA);
            
            // // 使用resources/fonts目录中的Noto Serif SC字体
            // parameters.put("net.sf.jasperreports.default.font.name", "Noto Serif SC");
            // // 设置字体扩展搜索路径，指向resources/fonts目录
            // parameters.put("net.sf.jasperreports.extension.search.path", "./fonts");
            // // 设置字体回退，确保兼容性
            // parameters.put("net.sf.jasperreports.default.font.fallback", "Noto Sans SC,SimSun,SimHei,宋体,黑体");
            // // 忽略缺失字体警告
            // parameters.put("net.sf.jasperreports.awt.ignore.missing.font", true);
            
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            
            // 4. 生成PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
            
            // 5. 设置响应头，确保PDF在浏览器中显示而不是下载
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "report.pdf");
            headers.setContentLength(outputStream.size());
            
            // 添加支持iframe嵌入的响应头
            headers.add("X-Frame-Options", "SAMEORIGIN");
            headers.add("Content-Security-Policy", "frame-ancestors *");
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "*");
            
            // 6. 返回PDF内容
            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            // 创建错误响应JSON
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "PDF生成失败");
            errorResponse.put("message", e.getMessage());
            
            // 设置JSON响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}