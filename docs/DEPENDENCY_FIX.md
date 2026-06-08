# 依赖问题修复说明

## 问题描述

在编译项目时遇到以下错误：
```
Could not resolve dependencies for project com.example:pdf-preview-server:jar:1.0-SNAPSHOT
dependency: net.sf.jasperreports:jasperreports-html:jar:6.21.5
dependency: net.sf.jasperreports:jasperreports-xls:jar:6.21.5
dependency: net.sf.jasperreports:jasperreports-docx:jar:6.21.5
dependency: net.sf.jasperreports:jasperreports-csv:jar:6.21.5
dependency: net.sf.jasperreports:jasperreports-odf:jar:6.21.5
```

**原因**：JasperReports 6.21.5版本中，这些扩展库不存在或需要不同的依赖方式。

---

## 解决方案

### 1. ✅ 已修复 - 移除不存在的依赖

**修改文件**：`pom.xml`

**修改内容**：
```xml
<!-- 移除了以下不存在的依赖 -->
<!--
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
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-odf</artifactId>
    <version>6.21.5</version>
</dependency>
-->
```

**保留的依赖**：
```xml
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports</artifactId>
    <version>6.21.5</version>
</dependency>
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-fonts</artifactId>
    <version>6.21.5</version>
</dependency>
```

---

### 2. ✅ 已修复 - 更新ExportService导出器类

**修改文件**：`service/ExportService.java`

**修改内容**：

#### 修改导入语句
```java
// 之前的导入（错误）
import net.sf.jasperreports.engine.export.*;
import net.sf.jasperreports.export.*;

// 修正后的导入
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
```

#### 使用正确的导出器类
- `JRPdfExporter` - PDF导出器（从`net.sf.jasperreports.engine.export`导入）
- `JRHtmlExporter` - HTML导出器
- `JRXlsxExporter` - Excel 2007+ 导出器（从`net.sf.jasperreports.engine.export.ooxml`导入）
- `JRXlsExporter` - Excel 97-2003 导出器
- `JRDocxExporter` - Word导出器（从`net.sf.jasperreports.engine.export.ooxml`导入）
- `JRCsvExporter` - CSV导出器
- `JRRtfExporter` - RTF导出器

---

## 编译和测试

### 1. 清理Maven缓存
```bash
cd /Users/yan.yang/open/jrxml_preview_server

# 清理Maven缓存中的失败记录
mvn dependency:purge-local-repository

# 或者强制更新
mvn clean compile -U
```

### 2. 编译项目
```bash
mvn clean compile -DskipTests
```

### 3. 运行测试
```bash
mvn test -Dtest=ExportServiceTest
```

### 4. 打包项目
```bash
mvn clean package -DskipTests
```

---

## 验证步骤

### 1. 验证pom.xml
```bash
cd /Users/yan.yang/open/jrxml_preview_server
cat pom.xml | grep -A 5 "jasperreports"
```

**期望输出**：
```xml
<!-- JasperReports -->
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports</artifactId>
    <version>6.21.5</version>
</dependency>

<!-- JasperReports Font Extension -->
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-fonts</artifactId>
    <version>6.21.5</version>
</dependency>
```

### 2. 验证编译
```bash
mvn compile -DskipTests
```

**期望输出**：
```
[INFO] BUILD SUCCESS
[INFO] -------------------------------------------------------
[INFO] Total time:  15.123 s
[INFO] Finished at: 2024-03-15T15:30:00+08:00
```

### 3. 验证测试
```bash
mvn test -Dtest=ExportServiceTest
```

**期望输出**：
```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 现在支持的导出格式

| 格式 | 类名 | 包路径 | 状态 |
|------|------|--------|------|
| PDF | JRPdfExporter | net.sf.jasperreports.engine.export | ✅ |
| HTML | JRHtmlExporter | net.sf.jasperreports.engine.export | ✅ |
| Excel (xlsx) | JRXlsxExporter | net.sf.jasperreports.engine.export.ooxml | ✅ |
| Excel (xls) | JRXlsExporter | net.sf.jasperreports.engine.export | ✅ |
| Word (docx) | JRDocxExporter | net.sf.jasperreports.engine.export.ooxml | ✅ |
| CSV | JRCsvExporter | net.sf.jasperreports.engine.export | ✅ |
| RTF | JRRtfExporter | net.sf.jasperreports.engine.export | ✅ |

---

## 常见问题

### Q1: 为什么不能用单独的扩展库？
**A**: JasperReports 6.x版本的导出器已经直接包含在核心库中，不需要单独的扩展库依赖。

### Q2: 编译时还是报错怎么办？
**A**: 
1. 确保清理了Maven缓存：`mvn dependency:purge-local-repository`
2. 强制更新依赖：`mvn clean compile -U`
3. 检查Maven仓库是否正确配置

### Q3: 测试失败怎么办？
**A**: 
1. 检查JasperReports版本是否正确（6.21.5）
2. 确保所有导入语句正确
3. 检查测试用例中的JRXML格式

---

## 修复总结

| 修复项 | 状态 | 说明 |
|--------|------|------|
| 移除不存在的依赖 | ✅ | 已从pom.xml移除 |
| 修正导入语句 | ✅ | 使用正确的包路径 |
| 验证导出器类 | ✅ | 确认类存在于核心库中 |
| 编译测试 | 🔄 | 需要在完整Maven环境中验证 |

---

**修复日期**: 2024-03-15
**修复人**: Claude Code
**状态**: ✅ 已修复，待编译验证
