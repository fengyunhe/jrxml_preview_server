package cn.firegod.pdfpreviewserver.model;

import java.util.Map;
import java.util.List;

/**
 * 导出请求模型
 */
public class ExportRequest {
    private String jrxml;
    private String format;  // pdf, html, excel, word, csv
    private Map<String, Object> parameters;
    private List<Map<String, Object>> dataSource;
    private Map<String, List<Map<String, Object>>> subDataSources;
    private ExportOptions options;

    public ExportRequest() {
    }

    public ExportRequest(String jrxml, String format) {
        this.jrxml = jrxml;
        this.format = format;
    }

    public String getJrxml() {
        return jrxml;
    }

    public void setJrxml(String jrxml) {
        this.jrxml = jrxml;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<Map<String, Object>> getDataSource() {
        return dataSource;
    }

    public void setDataSource(List<Map<String, Object>> dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, List<Map<String, Object>>> getSubDataSources() {
        return subDataSources;
    }

    public void setSubDataSources(Map<String, List<Map<String, Object>>> subDataSources) {
        this.subDataSources = subDataSources;
    }

    public ExportOptions getOptions() {
        return options;
    }

    public void setOptions(ExportOptions options) {
        this.options = options;
    }

    /**
     * 导出选项
     */
    public static class ExportOptions {
        private boolean embedImages = true;
        private String encoding = "UTF-8";
        private String locale = "zh_CN";
        private Map<String, String> customProperties;

        public boolean isEmbedImages() {
            return embedImages;
        }

        public void setEmbedImages(boolean embedImages) {
            this.embedImages = embedImages;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public Map<String, String> getCustomProperties() {
            return customProperties;
        }

        public void setCustomProperties(Map<String, String> customProperties) {
            this.customProperties = customProperties;
        }
    }
}
