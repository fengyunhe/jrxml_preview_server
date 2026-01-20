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

应用程序将在 `http://localhost:8084` 启动。

## API使用

### 控制器方法列表

#### 1. 主页重定向

**端点:** `GET /`
**控制器:** `PageController`
**功能:** 重定向到静态index.html页面，支持iframe嵌入

#### 2. 通过表单参数生成PDF

**端点:** `POST /api/pdf/generateForm`
**控制器:** `PdfGenerationController`
**功能:** 通过表单参数生成PDF
**参数:**
- `jrxml`: JRXML模板内容（必需）
- `parameters`: JSON格式的参数（可选）
- `dataSource`: JSON数组格式的数据源（可选）

#### 3. 通过纯文本JRXML生成PDF

**端点:** `POST /api/pdf/generate`
**控制器:** `PdfGenerationController`
**功能:** 通过纯文本JRXML内容生成PDF
**请求头:** `Content-Type: text/plain`
**请求体:** JRXML模板内容

#### 4. 通过JSON请求生成PDF

**端点:** `POST /api/pdf/generate`
**控制器:** `PdfGenerationController`
**功能:** 通过JSON请求生成PDF，支持参数和数据源
**请求头:** `Content-Type: application/json`
**请求体格式:**
```json
{
  "jrxmlContent": "JRXML模板内容",
  "parameters": { "key1": "value1", "key2": "value2" },
  "dataSource": [{ "field1": "value1" }, { "field2": "value2" }]
}
```

### 示例请求

#### 示例1: 通过curl使用纯文本JRXML生成PDF

```bash
curl -X POST "http://localhost:8084/api/pdf/generate" \
  -H "Content-Type: text/plain" \
  --data-binary @src/main/resources/sample.jrxml
```

#### 示例2: 通过curl使用JSON请求生成PDF

```bash
curl -X POST "http://localhost:8080/api/pdf/generate" \
  -H "Content-Type: application/json" \
  -d '{"jrxmlContent": "'$(cat src/main/resources/sample.jrxml | tr -d '\n' | tr -d '\r')'"}'
```

#### 示例3: 通过curl使用表单参数生成PDF

```bash
curl -X POST "http://localhost:8084/api/pdf/generateForm" \
  -F "jrxml=@src/main/resources/sample.jrxml" \
  -F "parameters={\"title\":\"测试报告\"}" \
  -F "dataSource=[{\"name\":\"测试数据\"}]"
```

#### 示例4: 使用Postman通过JSON请求生成PDF

1. 设置请求方法为POST
2. 输入URL: `http://localhost:8084/api/pdf/generate`
3. 在Headers选项卡中，添加 `Content-Type: application/json`
4. 在Body选项卡中，选择"raw"格式和"JSON"类型
5. 输入JSON请求体，包含jrxmlContent、parameters和dataSource
6. 发送请求，Postman会自动在响应查看器中显示PDF

### 响应格式

**成功响应:**
- **状态码:** 200 OK
- **响应头:** `Content-Type: application/pdf`
- **响应头:** `Content-Disposition: inline; filename=report.pdf`
- **响应体:** PDF文件内容

**错误响应:**
- **状态码:** 500 Internal Server Error
- **响应头:** `Content-Type: application/json`
- **响应体格式:**
  ```json
  {
    "error": "PDF生成失败",
    "message": "错误详细信息"
  }
  ```

## 自定义JRXML

您可以根据需要自定义JRXML模板。推荐使用[JRXML Web Designer](https://github.com/fengyunhe/jrxml_web_designer)，这是一个基于Vue 3的Web设计器，可以在浏览器中直接创建和编辑JRXML文件。

JasperReports支持各种元素，如：

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

---

[English README](README-en.md)