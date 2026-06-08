package cn.firegod.pdfpreviewserver.controller;

import cn.firegod.pdfpreviewserver.model.ExportRequest;
import cn.firegod.pdfpreviewserver.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 多格式导出控制器
 */
@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 导出报表为指定格式
     */
    @PostMapping
    public ResponseEntity<?> exportReport(@RequestBody ExportRequest request) {
        try {
            // 验证请求
            if (request.getJrxml() == null || request.getJrxml().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("JRXML content is required"));
            }

            if (request.getFormat() == null || request.getFormat().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Export format is required"));
            }

            logger.info("Exporting report to format: {}", request.getFormat());

            // 调用导出服务
            Object result = exportService.export(request);

            // 根据格式返回响应
            return buildResponse(result, request.getFormat());

        } catch (Exception e) {
            logger.error("Export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Export failed: " + e.getMessage()));
        }
    }

    /**
     * 快速导出为PDF
     */
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody ExportRequest request) {
        try {
            request.setFormat("pdf");
            byte[] pdfData = (byte[]) exportService.export(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "report.pdf");
            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("PDF export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 快速导出为HTML
     */
    @PostMapping("/html")
    public ResponseEntity<byte[]> exportHtml(@RequestBody ExportRequest request) {
        try {
            request.setFormat("html");
            byte[] htmlData = (byte[]) exportService.export(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setContentDispositionFormData("attachment", "report.html");
            return new ResponseEntity<>(htmlData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("HTML export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 快速导出为Excel
     */
    @PostMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody ExportRequest request) {
        try {
            request.setFormat("xlsx");
            byte[] excelData = (byte[]) exportService.export(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "report.xlsx");
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Excel export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 快速导出为Word
     */
    @PostMapping("/word")
    public ResponseEntity<byte[]> exportWord(@RequestBody ExportRequest request) {
        try {
            request.setFormat("docx");
            byte[] wordData = (byte[]) exportService.export(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "report.docx");
            return new ResponseEntity<>(wordData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Word export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 快速导出为CSV
     */
    @PostMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestBody ExportRequest request) {
        try {
            request.setFormat("csv");
            byte[] csvData = (byte[]) exportService.export(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "report.csv");
            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("CSV export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 验证JRXML是否可以导出
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateJrxml(@RequestBody Map<String, String> request) {
        try {
            String jrxml = request.get("jrxml");
            if (jrxml == null || jrxml.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("valid", false);
                error.put("message", "JRXML content is required");
                return ResponseEntity.badRequest().body(error);
            }

            ExportRequest exportRequest = new ExportRequest(jrxml, "pdf");
            exportService.export(exportRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "JRXML is valid and can be exported");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取支持的导出格式
     */
    @GetMapping("/formats")
    public ResponseEntity<Map<String, String>> getSupportedFormats() {
        Map<String, String> formats = new HashMap<>();
        formats.put("pdf", "PDF Document");
        formats.put("html", "HTML Web Page");
        formats.put("xlsx", "Excel 2007+ Workbook");
        formats.put("xls", "Excel 97-2003 Workbook");
        formats.put("docx", "Word Document");
        formats.put("csv", "CSV Spreadsheet");
        formats.put("rtf", "RTF Document");
        return ResponseEntity.ok(formats);
    }

    /**
     * 构建响应
     */
    private ResponseEntity<?> buildResponse(Object result, String format) {
        if (result instanceof byte[]) {
            byte[] data = (byte[]) result;
            String filename = "report." + getExtension(format);
            String contentType = getContentType(format);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);

        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Unexpected export result type"));
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return "pdf";
            case "html": return "html";
            case "excel":
            case "xlsx": return "xlsx";
            case "xls": return "xls";
            case "word":
            case "docx": return "docx";
            case "csv": return "csv";
            case "rtf": return "rtf";
            default: return "bin";
        }
    }

    /**
     * 获取内容类型
     */
    private String getContentType(String format) {
        switch (format.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "html": return "text/html";
            case "excel":
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls": return "application/vnd.ms-excel";
            case "word":
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "csv": return "text/csv";
            case "rtf": return "application/rtf";
            default: return "application/octet-stream";
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
