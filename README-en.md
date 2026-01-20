# PDF Preview Server

This is a PDF preview server based on Spring Boot and JasperReports. The server provides a REST API that allows clients to submit JRXML content and directly view the generated PDF in the browser.

## Features

- Receive JRXML content and compile it to JasperReport
- Generate PDF and display it directly in the browser (instead of downloading)
- Built with Spring Boot framework, easy to deploy and extend
- Provides sample JRXML template for testing

## Tech Stack

- Java 11
- Spring Boot 2.7.x
- JasperReports 6.20.0
- Maven

## Quick Start

### Build the Project

```bash
mvn clean package
```

### Run the Application

```bash
java -jar target/pdf-preview-server-1.0-SNAPSHOT.jar
```

The application will start at `http://localhost:8084`.

## API Usage

### Controller Methods

#### 1. Home Page Redirect

**Endpoint:** `GET /`
**Controller:** `PageController`
**Function:** Redirect to static index.html page, supports iframe embedding

#### 2. Generate PDF via Form Parameters

**Endpoint:** `POST /api/pdf/generateForm`
**Controller:** `PdfGenerationController`
**Function:** Generate PDF via form parameters
**Parameters:**
- `jrxml`: JRXML template content (required)
- `parameters`: JSON formatted parameters (optional)
- `dataSource`: JSON array formatted data source (optional)

#### 3. Generate PDF via Plain Text JRXML

**Endpoint:** `POST /api/pdf/generate`
**Controller:** `PdfGenerationController`
**Function:** Generate PDF via plain text JRXML content
**Request Header:** `Content-Type: text/plain`
**Request Body:** JRXML template content

#### 4. Generate PDF via JSON Request

**Endpoint:** `POST /api/pdf/generate`
**Controller:** `PdfGenerationController`
**Function:** Generate PDF via JSON request, supports parameters and data source
**Request Header:** `Content-Type: application/json`
**Request Body Format:**
```json
{
  "jrxmlContent": "JRXML template content",
  "parameters": { "key1": "value1", "key2": "value2" },
  "dataSource": [{ "field1": "value1" }, { "field2": "value2" }]
}
```

### Sample Requests

#### Sample 1: Generate PDF via curl with Plain Text JRXML

```bash
curl -X POST "http://localhost:8084/api/pdf/generate" \
  -H "Content-Type: text/plain" \
  --data-binary @src/main/resources/sample.jrxml
```

#### Sample 2: Generate PDF via curl with JSON Request

```bash
curl -X POST "http://localhost:8080/api/pdf/generate" \
  -H "Content-Type: application/json" \
  -d '{"jrxmlContent": "'$(cat src/main/resources/sample.jrxml | tr -d '\n' | tr -d '\r')'"}'
```

#### Sample 3: Generate PDF via curl with Form Parameters

```bash
curl -X POST "http://localhost:8084/api/pdf/generateForm" \
  -F "jrxml=@src/main/resources/sample.jrxml" \
  -F "parameters={\"title\":\"Test Report\"}" \
  -F "dataSource=[{\"name\":\"Test Data\"}]"
```

#### Sample 4: Generate PDF via Postman with JSON Request

1. Set request method to POST
2. Enter URL: `http://localhost:8084/api/pdf/generate`
3. In Headers tab, add `Content-Type: application/json`
4. In Body tab, select "raw" format and "JSON" type
5. Enter JSON request body containing jrxmlContent, parameters, and dataSource
6. Send request, Postman will automatically display the PDF in the response viewer

### Response Formats

**Success Response:**
- **Status Code:** 200 OK
- **Response Header:** `Content-Type: application/pdf`
- **Response Header:** `Content-Disposition: inline; filename=report.pdf`
- **Response Body:** PDF file content

**Error Response:**
- **Status Code:** 500 Internal Server Error
- **Response Header:** `Content-Type: application/json`
- **Response Body Format:**
  ```json
  {
    "error": "PDF generation failed",
    "message": "Error details"
  }
  ```

## Customizing JRXML

You can customize JRXML templates according to your needs. It is recommended to use [JRXML Web Designer](https://github.com/fengyunhe/jrxml_web_designer), a Vue 3 based web designer that allows you to create and edit JRXML files directly in the browser.

JasperReports supports various elements such as:

- Text fields
- Images
- Tables
- Charts
- Conditional formatting

For more information about JRXML syntax, please refer to [JasperReports Documentation](https://community.jaspersoft.com/documentation).

## Troubleshooting

### Common Issues

1. **PDF not displaying**: Ensure response headers are set correctly, especially `Content-Type` and `Content-Disposition`
2. **JRXML compilation errors**: Check if JRXML syntax is correct
3. **Font issues**: If text in PDF is not displaying correctly, you may need to add additional font dependencies

## License

This project is licensed under the MIT License.

---

[中文版本 README](README.md)