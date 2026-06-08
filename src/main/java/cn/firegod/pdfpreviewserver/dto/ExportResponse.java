package cn.firegod.pdfpreviewserver.dto;

import java.util.Map;

/**
 * 导出响应模型
 */
public class ExportResponse {
    private boolean success;
    private String format;
    private byte[] data;
    private String htmlContent;  // 用于HTML格式
    private String csvContent;   // 用于CSV格式
    private long fileSize;
    private String contentType;
    private String filename;
    private Map<String, String> metadata;

    public ExportResponse() {
    }

    public static ExportResponse success(String format, byte[] data, String filename) {
        ExportResponse response = new ExportResponse();
        response.setSuccess(true);
        response.setFormat(format);
        response.setData(data);
        response.setFilename(filename);
        response.setFileSize(data != null ? data.length : 0);
        response.setContentType(getContentTypeForFormat(format));
        return response;
    }

    public static ExportResponse successHtml(String htmlContent, String filename) {
        ExportResponse response = new ExportResponse();
        response.setSuccess(true);
        response.setFormat("html");
        response.setHtmlContent(htmlContent);
        response.setFilename(filename);
        response.setContentType("text/html; charset=UTF-8");
        return response;
    }

    public static ExportResponse successCsv(String csvContent, String filename) {
        ExportResponse response = new ExportResponse();
        response.setSuccess(true);
        response.setFormat("csv");
        response.setCsvContent(csvContent);
        response.setFilename(filename);
        response.setContentType("text/csv; charset=UTF-8");
        return response;
    }

    public static ExportResponse error(String message) {
        ExportResponse response = new ExportResponse();
        response.setSuccess(false);
        response.setFilename(message);
        return response;
    }

    private static String getContentTypeForFormat(String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "html":
                return "text/html; charset=UTF-8";
            case "excel":
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls":
                return "application/vnd.ms-excel";
            case "word":
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc":
                return "application/msword";
            case "csv":
                return "text/csv; charset=UTF-8";
            case "rtf":
                return "application/rtf";
            default:
                return "application/octet-stream";
        }
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getCsvContent() {
        return csvContent;
    }

    public void setCsvContent(String csvContent) {
        this.csvContent = csvContent;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
