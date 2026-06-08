# JRXML Web Designer 后端服务架构设计

## 📋 文档概述

本文档定义 JRXML Web Designer 后端服务的完整架构，包括：
1. 多格式导出支持
2. 数据源连接管理
3. AI智能设计接口
4. 模板和版本管理

基于现有 `jrxml_preview_server` 扩展，保持向后兼容。

---

## 一、现有服务分析

### 1.1 当前功能

```
cn.firegod.pdfpreviewserver/
├── PdfPreviewServerApplication.java      # Spring Boot入口
├── controller/
│   ├── PdfGenerationController.java      # 核心PDF生成
│   └── PageController.java               # 页面控制
├── model/
│   └── PdfGenerationRequest.java         # 请求模型
└── config/
    └── WebConfig.java                    # CORS配置
```

**已有接口**：
- `POST /api/pdf/generateForm` - 表单方式生成PDF
- `POST /api/pdf/generate` - JSON方式生成PDF

**技术栈**：
- Spring Boot 2.7.15
- JasperReports 6.21.5
- Java 8

### 1.2 可复用的组件

1. **JRXML编译和验证** - `sanitizeNullExpressions()` 和 `validateElementExpressions()`
2. **数据源处理** - `applySubDataSources()` 用于Table组件
3. **表达式诊断** - `dumpCompilationContext()` 和 `simulateCompilationFilter()`

---

## 二、新增服务模块

### 2.1 模块划分

```
cn.firegod.pdfpreviewserver/
├── controller/
│   ├── PdfGenerationController.java      # 已有 - 保留
│   ├── ExportController.java             # 新增 - 多格式导出
│   ├── DataSourceController.java         # 新增 - 数据源管理
│   ├── AiDesignController.java           # 新增 - AI设计接口
│   ├── TemplateController.java           # 新增 - 模板管理
│   └── ReportController.java             # 新增 - 报表CRUD
│
├── service/
│   ├── JasperReportService.java          # 新增 - 核心编译服务
│   ├── ExportService.java                # 新增 - 多格式导出
│   ├── DataSourceService.java            # 新增 - 数据源连接
│   ├── AiDesignService.java              # 新增 - AI智能设计
│   ├── TemplateService.java              # 新增 - 模板管理
│   └── CacheService.java                 # 新增 - 编译缓存
│
├── model/
│   ├── PdfGenerationRequest.java         # 已有
│   ├── ExportRequest.java                # 新增
│   ├── DataSourceConfig.java             # 新增
│   ├── AiDesignRequest.java              # 新增
│   ├── ReportDesign.java                 # 新增
│   ├── FieldSchema.java                  # 新增
│   └── ReportTemplate.java               # 新增
│
├── dto/
│   ├── ExportResponse.java               # 新增
│   ├── DataSourceTestResponse.java       # 新增
│   ├── SchemaResponse.java               # 新增
│   └── AiDesignResponse.java             # 新增
│
└── config/
    ├── WebConfig.java                    # 已有 - CORS配置
    ├── AiConfig.java                     # 新增 - AI服务配置
    └── DataSourceConfig.java             # 新增 - 数据源池配置
```

---

## 三、接口详细设计

### 3.1 多格式导出接口

#### `POST /api/export`
**功能**：导出报表为多种格式

**请求体**：
```json
{
  "jrxml": "<jasperReport>...</jasperReport>",
  "format": "pdf|html|excel|word|csv",
  "parameters": {
    "REPORT_TITLE": "销售报表"
  },
  "dataSource": [
    {"id": 1, "name": "产品A", "amount": 100.00}
  ],
  "subDataSources": {
    "tableDataset": [...]
  }
}
```

**响应**：
- **PDF/Excel/Word**：二进制文件流（Content-Type: application/octet-stream）
- **HTML**：`text/html` 内容
- **CSV**：`text/csv` 内容

**响应头**：
```
Content-Type: application/pdf (或对应类型)
Content-Disposition: attachment; filename=report.pdf
X-Content-Type-Options: nosniff
```

**实现**：
```java
@PostMapping
public ResponseEntity<byte[]> export(@RequestBody ExportRequest request) {
    // 1. 加载和编译JRXML
    JasperDesign design = loadDesign(request.getJrxml());
    JasperReport report = compileReport(design);
    
    // 2. 准备数据源和参数
    JRDataSource dataSource = prepareDataSource(request);
    Map<String, Object> params = prepareParameters(request);
    
    // 3. 填充报表
    JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
    
    // 4. 按格式导出
    return switch (request.getFormat()) {
        case "html" -> exportHtml(print);
        case "excel" -> exportExcel(print);
        case "word" -> exportWord(print);
        case "csv" -> exportCsv(print);
        default -> exportPdf(print);
    };
}
```

**依赖库**：
```xml
<!-- 已有 -->
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports</artifactId>
    <version>6.21.5</version>
</dependency>

<!-- 新增 -->
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-html</artifactId>
    <version>6.21.5</version>
</dependency>
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-xls</artifactId>
    <version>6.21.5</version>
</dependency>
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-docx</artifactId>
    <version>6.21.5</version>
</dependency>
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-csv</artifactId>
    <version>6.21.5</version>
</dependency>
```

---

### 3.2 数据源管理接口

#### `POST /api/datasource/test`
**功能**：测试数据库连接

**请求体**：
```json
{
  "type": "jdbc|xml|csv",
  "jdbcUrl": "jdbc:mysql://localhost:3306/mydb",
  "username": "root",
  "password": "****",
  "driverClass": "com.mysql.cj.jdbc.Driver",
  "validationQuery": "SELECT 1"
}
```

**响应**：
```json
{
  "success": true,
  "message": "Connection successful",
  "responseTime": 123,
  "serverVersion": "MySQL 8.0.32"
}
```

---

#### `POST /api/datasource/query`
**功能**：从数据库查询数据

**请求体**：
```json
{
  "config": {
    "type": "jdbc",
    "jdbcUrl": "jdbc:mysql://localhost:3306/mydb",
    "username": "root",
    "password": "****"
  },
  "query": "SELECT id, name, amount FROM sales WHERE date >= ?",
  "parameters": ["2024-01-01"],
  "limit": 100
}
```

**响应**：
```json
{
  "columns": ["id", "name", "amount"],
  "rows": [
    [1, "产品A", 100.00],
    [2, "产品B", 200.00]
  ],
  "metadata": {
    "columnTypes": ["INTEGER", "VARCHAR", "DECIMAL"],
    "totalRows": 1000,
    "executionTime": 50
  }
}
```

---

#### `POST /api/datasource/schema`
**功能**：获取数据源的表结构

**请求体**：
```json
{
  "config": {
    "type": "jdbc",
    "jdbcUrl": "jdbc:mysql://localhost:3306/mydb",
    "username": "root",
    "password": "****"
  },
  "table": "sales"
}
```

**响应**：
```json
{
  "tableName": "sales",
  "columns": [
    {
      "name": "id",
      "type": "INTEGER",
      "nullable": false,
      "primaryKey": true,
      "autoIncrement": true
    },
    {
      "name": "name",
      "type": "VARCHAR(255)",
      "nullable": false,
      "primaryKey": false
    },
    {
      "name": "amount",
      "type": "DECIMAL(10, 2)",
      "nullable": true,
      "primaryKey": false
    }
  ],
  "indexes": [...],
  "foreignKeys": [...]
}
```

---

### 3.3 AI设计接口

#### `POST /api/ai/design`
**功能**：从自然语言生成报表设计

**请求体**：
```json
{
  "description": "生成一个销售报表，显示每个产品的销售总额，按地区分组，包含图表",
  "dataSource": {
    "type": "jdbc",
    "config": {...}
  },
  "style": {
    "theme": "professional|minimal|modern",
    "colorScheme": "blue",
    "fontSize": 12
  },
  "constraints": {
    "pageWidth": 595,
    "pageHeight": 842,
    "margins": [20, 20, 20, 20]
  }
}
```

**响应**：
```json
{
  "design": {
    "reportProperties": {...},
    "bands": {...},
    "fields": [...],
    "parameters": [...],
    "variables": [...]
  },
  "jrxml": "<jasperReport>...</jasperReport>",
  "preview": {
    "imageUrl": "/api/preview/image/{designId}",
    "pdfUrl": "/api/preview/pdf/{designId}"
  },
  "confidence": 0.95,
  "suggestions": [
    "考虑添加分组汇总",
    "可使用条件样式突出高销售额"
  ]
}
```

**实现流程**：
```java
@Service
public class AiDesignService {
    
    @Autowired
    private SchemaInferenceService schemaService;
    
    @Autowired
    private LlmClient llmClient;  // LLM API客户端
    
    public ReportDesign generateDesign(AiDesignRequest request) {
        // 1. 获取数据源Schema
        FieldSchema schema = schemaService.inferSchema(
            request.getDataSource()
        );
        
        // 2. 构建提示词
        String prompt = buildDesignPrompt(
            request.getDescription(),
            schema,
            request.getStyle()
        );
        
        // 3. 调用LLM生成设计
        String designJson = llmClient.generate(prompt);
        
        // 4. 验证设计
        ReportDesign design = parseDesign(designJson);
        ValidationResult validation = validateDesign(design);
        
        // 5. 自动修复错误
        if (!validation.isValid()) {
            design = autoFix(design, validation.getErrors());
        }
        
        // 6. 生成JRXML
        String jrxml = generateJrxml(design);
        
        return ReportDesign.builder()
            .design(design)
            .jrxml(jrxml)
            .confidence(calculateConfidence(design))
            .suggestions(generateSuggestions(design))
            .build();
    }
}
```

---

#### `POST /api/ai/suggest-layout`
**功能**：推荐报表布局方案

**请求体**：
```json
{
  "fields": [
    {"name": "product_name", "type": "VARCHAR"},
    {"name": "amount", "type": "DECIMAL"},
    {"name": "region", "type": "VARCHAR"}
  ],
  "scene": "sales_report",
  "pageConstraints": {
    "width": 595,
    "height": 842
  }
}
```

**响应**：
```json
{
  "recommendations": [
    {
      "id": "layout_1",
      "name": "表格+图表",
      "confidence": 0.9,
      "description": "使用表格显示详细数据，配合图表展示趋势",
      "preview": "..."
    },
    {
      "id": "layout_2",
      "name": "分组表格",
      "confidence": 0.85,
      "description": "按地区分组显示销售数据",
      "preview": "..."
    }
  ]
}
```

---

#### `POST /api/ai/validate`
**功能**：验证报表设计

**请求体**：
```json
{
  "design": {...},
  "rules": [
    "no-null-expressions",
    "valid-field-references",
    "layout-constraints"
  ]
}
```

**响应**：
```json
{
  "valid": false,
  "errors": [
    {
      "type": "error",
      "field": "bands.detail.elements[0].expression",
      "message": "Expression references undefined field: $F{nonexistent}",
      "line": 15,
      "suggestion": "Ensure field is declared in the fields array"
    }
  ],
  "warnings": [
    {
      "type": "warning",
      "message": "Element exceeds band height",
      "suggestion": "Consider using stretchable text field"
    }
  ]
}
```

---

#### `POST /api/ai/optimize`
**功能**：优化报表设计

**请求体**：
```json
{
  "design": {...},
  "optimizations": [
    "performance",
    "layout",
    "usability"
  ]
}
```

**响应**：
```json
{
  "optimizedDesign": {...},
  "changes": [
    {
      "type": "performance",
      "description": "合并多个相似的表达式为变量",
      "impact": "减少15%的编译时间"
    }
  ],
  "improvementScore": 0.23
}
```

---

### 3.4 报表管理接口

#### `POST /api/reports`
**功能**：创建/保存报表

**请求体**：
```json
{
  "name": "销售报表",
  "description": "月度销售统计报表",
  "design": {...},
  "jrxml": "<jasperReport>...</jasperReport>",
  "templateId": "tmpl_sales_001",
  "tags": ["sales", "monthly", "chart"]
}
```

**响应**：
```json
{
  "reportId": "rpt_20240315_001",
  "createdAt": "2024-03-15T10:30:00Z",
  "version": 1
}
```

---

#### `GET /api/reports/{reportId}`
**功能**：获取报表详情

**响应**：
```json
{
  "reportId": "rpt_20240315_001",
  "name": "销售报表",
  "design": {...},
  "jrxml": "<jasperReport>...</jasperReport>",
  "versions": [
    {"version": 1, "createdAt": "..."},
    {"version": 2, "createdAt": "..."}
  ],
  "metadata": {
    "createdBy": "user_123",
    "lastModified": "..."
  }
}
```

---

#### `GET /api/reports`
**功能**：列出所有报表

**查询参数**：
- `page` - 页码（默认1）
- `pageSize` - 每页条数（默认20）
- `search` - 搜索关键词
- `tags` - 标签过滤
- `sortBy` - 排序字段（createdAt, name）
- `sortOrder` - asc/desc

**响应**：
```json
{
  "reports": [...],
  "pagination": {
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

---

### 3.5 模板管理接口

#### `GET /api/templates`
**功能**：列出报表模板

**响应**：
```json
{
  "templates": [
    {
      "templateId": "tmpl_sales_001",
      "name": "销售报表模板",
      "category": "sales",
      "preview": "https://...",
      "usageCount": 150
    }
  ]
}
```

---

#### `POST /api/templates`
**功能**：创建模板

**请求体**：
```json
{
  "name": "销售报表模板",
  "description": "标准销售报表布局",
  "category": "sales",
  "design": {...},
  "jrxml": "<jasperReport>...</jasperReport>",
  "preview": "base64_encoded_image"
}
```

---

## 四、数据模型设计

### 4.1 ExportRequest.java

```java
public class ExportRequest {
    private String jrxml;
    private String format;  // pdf, html, excel, word, csv
    private Map<String, Object> parameters;
    private List<Map<String, Object>> dataSource;
    private Map<String, List<Map<String, Object>>> subDataSources;
    private ExportOptions options;
    
    // getters/setters
}

public class ExportOptions {
    private boolean embedImages;
    private String encoding;  // UTF-8, GBK, etc.
    private String locale;
    private Map<String, String> customProperties;
}
```

### 4.2 DataSourceConfig.java

```java
public class DataSourceConfig {
    private String type;  // jdbc, xml, csv, bean
    
    // JDBC specific
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClass;
    private int connectionTimeout;
    private int maxPoolSize;
    
    // CSV specific
    private String csvFilePath;
    private String delimiter;
    private boolean hasHeader;
    
    // XML specific
    private String xmlFilePath;
    private String xmlQuery;
    
    // Validation
    private String validationQuery;
}
```

### 4.3 AiDesignRequest.java

```java
public class AiDesignRequest {
    private String description;
    private DataSourceConfig dataSource;
    private StylePreferences style;
    private PageConstraints constraints;
    private List<String> requirements;
}

public class StylePreferences {
    private String theme;  // professional, minimal, modern
    private String colorScheme;
    private int fontSize;
    private String fontFamily;
    private boolean showGridLines;
}

public class PageConstraints {
    private int pageWidth;
    private int pageHeight;
    private int[] margins;
    private String orientation;  // portrait, landscape
}
```

### 4.4 ReportDesign.java

```java
public class ReportDesign {
    private ReportProperties reportProperties;
    private Map<String, Band> bands;
    private List<Field> fields;
    private List<Parameter> parameters;
    private List<Variable> variables;
    private List<Style> styles;
    
    // Nested classes
    public static class ReportProperties {
        private String name;
        private String title;
        private int columnWidth;
        private int pageWidth;
        private int pageHeight;
        private int[] margins;
        private String whenNoDataType;
        // ...
    }
    
    public static class Band {
        private List<DesignElement> elements;
        private int height;
        // ...
    }
    
    public static class DesignElement {
        private String type;
        private String uuid;
        private int x, y, width, height;
        private Map<String, Object> properties;
        // ...
    }
}
```

### 4.5 FieldSchema.java

```java
public class FieldSchema {
    private String name;
    private String type;
    private boolean nullable;
    private boolean primaryKey;
    private boolean autoIncrement;
    private String description;
    private Long sampleCount;
    private List<Object> sampleValues;
}
```

---

## 五、实现细节

### 5.1 ExportService.java

```java
@Service
public class ExportService {
    
    public byte[] exportToPdf(JasperPrint print) throws JRException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JasperExportManager.exportReportToPdfStream(print, output);
        return output.toByteArray();
    }
    
    public String exportToHtml(JasperPrint print) throws JRException {
        StringWriter writer = new StringWriter();
        JRHtmlExporter exporter = new JRHtmlExporter();
        exporter.setExporterOutput(new SimpleHtmlWriterOutput(writer));
        exporter.exportReport();
        return writer.toString();
    }
    
    public byte[] exportToExcel(JasperPrint print) throws JRException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setExporterParameter(JRExporterParameter.OUTPUT_STREAM, output);
        exporter.exportReport();
        return output.toByteArray();
    }
    
    public byte[] exportToWord(JasperPrint print) throws JRException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JRDocxExporter exporter = new JRDocxExporter();
        exporter.setExporterParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setExporterParameter(JRExporterParameter.OUTPUT_STREAM, output);
        exporter.exportReport();
        return output.toByteArray();
    }
    
    public String exportToCsv(JasperPrint print) throws JRException {
        StringWriter writer = new StringWriter();
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setExporterOutput(new SimpleWriterExporterOutput(writer));
        exporter.exportReport();
        return writer.toString();
    }
}
```

### 5.2 DataSourceService.java

```java
@Service
public class DataSourceService {
    
    private final Map<String, DataSource> dataSourcePool = new ConcurrentHashMap<>();
    
    public ConnectionTestResult testConnection(DataSourceConfig config) {
        long start = System.currentTimeMillis();
        
        try (Connection conn = createConnection(config)) {
            long responseTime = System.currentTimeMillis() - start;
            
            DatabaseMetaData meta = conn.getMetaData();
            return ConnectionTestResult.builder()
                .success(true)
                .message("Connection successful")
                .responseTime(responseTime)
                .serverVersion(meta.getDatabaseProductVersion())
                .build();
        } catch (SQLException e) {
            return ConnectionTestResult.builder()
                .success(false)
                .message(e.getMessage())
                .responseTime(System.currentTimeMillis() - start)
                .build();
        }
    }
    
    public QueryResult executeQuery(DataSourceConfig config, String query, List<Object> params) {
        try (Connection conn = createConnection(config);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            // 设置参数
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
            }
            
            ResultSet rs = stmt.executeQuery();
            return convertToQueryResult(rs);
            
        } catch (SQLException e) {
            throw new DataSourceException("Query execution failed", e);
        }
    }
    
    public List<FieldSchema> inferSchema(DataSourceConfig config, String table) {
        try (Connection conn = createConnection(config)) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, table, "%");
            
            List<FieldSchema> schema = new ArrayList<>();
            while (columns.next()) {
                FieldSchema field = new FieldSchema();
                field.setName(columns.getString("COLUMN_NAME"));
                field.setType(columns.getString("TYPE_NAME"));
                field.setNullable("YES".equals(columns.getString("IS_NULLABLE")));
                field.setPrimaryKey(isPrimaryKey(meta, table, field.getName()));
                schema.add(field);
            }
            
            return schema;
            
        } catch (SQLException e) {
            throw new DataSourceException("Schema inference failed", e);
        }
    }
    
    private Connection createConnection(DataSourceConfig config) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        
        Class.forName(config.getDriverClass());
        return DriverManager.getConnection(config.getJdbcUrl(), props);
    }
}
```

### 5.3 AiDesignService.java

```java
@Service
public class AiDesignService {
    
    @Autowired
    private LlmClient llmClient;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private JasperReportService jasperService;
    
    public ReportDesign generateDesign(AiDesignRequest request) {
        // 1. 推断数据源Schema
        List<FieldSchema> schema = null;
        if (request.getDataSource() != null) {
            schema = dataSourceService.inferSchema(
                request.getDataSource(),
                "table_name"
            );
        }
        
        // 2. 构建提示词
        String prompt = buildDesignPrompt(request.getDescription(), schema, request.getStyle());
        
        // 3. 调用LLM
        String response = llmClient.generate(prompt);
        ReportDesign design = parseDesignJson(response);
        
        // 4. 验证和修复
        ValidationResult validation = jasperService.validateDesign(design);
        if (!validation.isValid()) {
            design = autoFixDesign(design, validation.getErrors());
        }
        
        // 5. 生成JRXML
        String jrxml = jasperService.generateJrxml(design);
        
        return ReportDesign.builder()
            .design(design)
            .jrxml(jrxml)
            .confidence(calculateConfidence(design, validation))
            .build();
    }
    
    private String buildDesignPrompt(String description, List<FieldSchema> schema, StylePreferences style) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a JasperReports JRXML design based on this description:\n\n");
        sb.append("Description: ").append(description).append("\n\n");
        
        if (schema != null && !schema.isEmpty()) {
            sb.append("Available fields:\n");
            for (FieldSchema field : schema) {
                sb.append("- ").append(field.getName())
                  .append(" (").append(field.getType()).append(")\n");
            }
            sb.append("\n");
        }
        
        if (style != null) {
            sb.append("Style preferences:\n");
            sb.append("- Theme: ").append(style.getTheme()).append("\n");
            sb.append("- Color scheme: ").append(style.getColorScheme()).append("\n");
            sb.append("- Font size: ").append(style.getFontSize()).append("\n");
        }
        
        sb.append("\nGenerate JSON design that includes:\n");
        sb.append("1. Report properties (page size, margins)\n");
        sb.append("2. Bands (title, pageHeader, columnHeader, detail, columnFooter, pageFooter)\n");
        sb.append("3. Fields from the schema\n");
        sb.append("4. Elements (staticText, textField, image, chart)\n");
        sb.append("5. Styles for visual presentation\n");
        
        return sb.toString();
    }
}
```

---

## 六、配置文件

### 6.1 application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

# AI Configuration
ai:
  provider: openai  # or anthropic, azure-openai
  api-key: ${AI_API_KEY}
  model: gpt-4
  max-tokens: 4000
  temperature: 0.7

# Cache Configuration
cache:
  type: redis
  ttl: 3600

# Export Configuration
export:
  max-file-size: 10MB
  allowed-formats:
    - pdf
    - html
    - excel
    - word
    - csv
  temp-directory: /tmp/jasper-exports

# DataSource Pool
datasource:
  pool:
    max-connections: 50
    idle-timeout: 300000
    validation-interval: 30000
```

### 6.2 AiConfig.java

```java
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {
    private String provider;
    private String apiKey;
    private String model;
    private int maxTokens;
    private double temperature;
    
    @Bean
    public LlmClient llmClient() {
        return switch (provider) {
            case "openai" -> new OpenAiClient(apiKey, model);
            case "anthropic" -> new AnthropicClient(apiKey, model);
            case "azure-openai" -> new AzureOpenAiClient(apiKey, model);
            default -> throw new IllegalArgumentException("Unknown AI provider: " + provider);
        };
    }
}
```

---

## 七、错误处理

### 7.1 异常类型

```java
public class ExportException extends RuntimeException {
    private String format;
    private String details;
}

public class DataSourceException extends RuntimeException {
    private String operation;
    private String details;
}

public class AiDesignException extends RuntimeException {
    private String phase;  // schema, generate, validate
    private String details;
}
```

### 7.2 全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ErrorResponse> handleExportException(ExportException e) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .code("EXPORT_FAILED")
                .message(e.getMessage())
                .details(e.getDetails())
                .build()
        );
    }
    
    @ExceptionHandler(DataSourceException.class)
    public ResponseEntity<ErrorResponse> handleDataSourceException(DataSourceException e) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .code("DATASOURCE_ERROR")
                .message(e.getMessage())
                .details(e.getDetails())
                .build()
        );
    }
}
```

---

## 八、性能优化

### 8.1 编译缓存

```java
@Service
public class CacheService {
    
    private final Cache<String, JasperReport> compilationCache;
    
    public JasperReport getOrCompile(JasperDesign design) {
        String key = calculateDesignHash(design);
        
        return compilationCache.get(key, k -> {
            try {
                return JasperCompileManager.compileReport(design);
            } catch (JRException e) {
                throw new CompilationException("Compilation failed", e);
            }
        });
    }
}
```

### 8.2 异步导出

```java
@Service
public class AsyncExportService {
    
    @Async
    public CompletableFuture<byte[]> exportAsync(ExportRequest request) {
        // 导出逻辑
        return CompletableFuture.completedFuture(result);
    }
}
```

---

## 九、测试策略

### 9.1 单元测试

```java
@SpringBootTest
class ExportServiceTest {
    
    @Autowired
    private ExportService exportService;
    
    @Test
    void testExportToHtml() throws JRException {
        JasperPrint print = createTestPrint();
        String html = exportService.exportToHtml(print);
        assertTrue(html.contains("<html>"));
    }
}
```

### 9.2 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
    
    @Test
    void testExportEndpoint() {
        ExportRequest request = createTestRequest();
        
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/export")
        .then()
            .statusCode(200)
            .header("Content-Type", "application/pdf");
    }
}
```

---

## 十、部署架构

```
┌─────────────────────────────────────┐
│      Load Balancer (nginx)          │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │  Application Server │
    │   (Spring Boot)     │
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   Service Layer     │
    ├─────────────────────┤
    │ Export Service      │
    │ DataSource Service  │
    │ AI Design Service   │
    │ Cache Service       │
    └──────────┬──────────┘
               │
    ┌──────────┴──────────┐
    │   External Systems  │
    ├─────────────────────┤
    │ Databases (JDBC)    │
    │ LLM API (OpenAI)    │
    │ Redis (Cache)       │
    └─────────────────────┘
```

---

## 十一、实施路线图

### Phase 1（1-2周）：多格式导出 ✅
- [ ] ExportController 实现
- [ ] ExportService 实现
- [ ] 单元测试
- [ ] 集成测试

### Phase 2（2-3周）：数据源管理 ✅
- [ ] DataSourceController 实现
- [ ] DataSourceService 实现
- [ ] 连接池配置
- [ ] Schema推断

### Phase 3（3-4周）：AI设计接口 ✅
- [ ] AiDesignController 实现
- [ ] AiDesignService 实现
- [ ] LLM集成
- [ ] 设计验证

### Phase 4（4-6周）：高级功能 ✅
- [ ] 报表管理
- [ ] 模板管理
- [ ] 版本控制
- [ ] 性能优化

---

## 十二、总结

本架构设计提供了完整的后端服务解决方案，支持：

1. **多格式导出** - PDF/HTML/Excel/Word/CSV
2. **数据源管理** - 连接测试、查询、Schema推断
3. **AI智能设计** - 自然语言→报表设计、布局推荐、自动优化
4. **报表管理** - CRUD、模板、版本控制

所有接口都遵循RESTful设计原则，提供清晰的请求/响应格式，并包含完善的错误处理机制。通过模块化设计，可以灵活扩展新功能。
