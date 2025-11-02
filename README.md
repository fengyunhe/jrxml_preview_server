# PDF预览服务器

这是一个基于Spring Boot和JasperReports的PDF预览服务器。该服务器提供REST API，允许客户端提交JRXML内容并在浏览器中直接查看生成的PDF。

## 功能特点

- 接收JRXML内容并将其编译为JasperReport
- 生成PDF并在浏览器中直接显示（而不是下载）
- 使用Spring Boot框架，易于部署和扩展
- 提供示例JRXML模板用于测试

## 技术栈

- Java 11
- Spring Boot 2.7.x
- JasperReports 6.20.0
- Maven

## 快速开始

### 构建项目

```bash
mvn clean package
```

### 运行应用程序

```bash
java -jar target/pdf-preview-server-1.0-SNAPSHOT.jar
```

应用程序将在 `http://localhost:8080` 启动。

## API使用

### 生成PDF

**端点:** `POST /api/pdf/generate`

**请求体:** JRXML内容（XML字符串）

**响应:** PDF内容，设置为在浏览器中内联显示

### 示例请求（使用curl）

```bash
curl -X POST "http://localhost:8080/api/pdf/generate" \
  -H "Content-Type: text/plain" \
  --data-binary @src/main/resources/sample.jrxml
```

### 使用Postman

1. 设置请求方法为POST
2. 输入URL: `http://localhost:8080/api/pdf/generate`
3. 在Body选项卡中，选择"raw"格式
4. 复制并粘贴JRXML内容
5. 发送请求，Postman会自动在响应查看器中显示PDF

## 自定义JRXML

您可以根据需要自定义JRXML模板。JasperReports支持各种元素，如：

- 文本字段
- 图像
- 表格
- 图表
- 条件格式

有关JRXML语法的更多信息，请参阅[JasperReports文档](https://community.jaspersoft.com/documentation).

## 故障排除

### 常见问题

1. **PDF不显示**：确保响应头设置正确，特别是`Content-Type`和`Content-Disposition`
2. **JRXML编译错误**：检查JRXML语法是否正确
3. **字体问题**：如果PDF中的文本显示不正确，可能需要添加额外的字体依赖

## 许可证

本项目采用MIT许可证。