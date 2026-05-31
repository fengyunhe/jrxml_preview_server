package cn.firegod.pdfpreviewserver.model;

import java.util.Map;
import java.util.List;

public class PdfGenerationRequest {
    private String jrxmlContent;
    private Map<String, Object> parameters;
    private List<Map<String, Object>> dataSource;
    private Map<String, List<Map<String, Object>>> subDataSources;

    public String getJrxmlContent() {
        return jrxmlContent;
    }

    public void setJrxmlContent(String jrxmlContent) {
        this.jrxmlContent = jrxmlContent;
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
}