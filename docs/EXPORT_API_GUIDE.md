# 导出接口使用指南

## 概述

本文档介绍如何使用新增的多格式导出接口，支持将JRXML报表导出为PDF、HTML、Excel、Word、CSV等多种格式。

---

## 接口列表

| 接口 | 方法 | 格式 | 说明 |
|------|------|------|------|
| `/api/export` | POST | 通用 | 通用导出接口，支持所有格式 |
| `/api/export/pdf` | POST | PDF | 快速导出PDF |
| `/api/export/html` | POST | HTML | 快速导出HTML |
| `/api/export/excel` | POST | Excel | 快速导出Excel (xlsx) |
| `/api/export/word` | POST | Word | 快速导出Word (docx) |
| `/api/export/csv` | POST | CSV | 快速导出CSV |
| `/api/export/validate` | POST | - | 验证JRXML是否有效 |
| `/api/export/formats` | GET | - | 获取支持的格式列表 |

---

## 快速开始

### 1. 使用通用导出接口

```typescript
// src/composables/useExport.ts

import axios from 'axios';

interface ExportRequest {
  jrxml: string;
  format: 'pdf' | 'html' | 'excel' | 'word' | 'csv';
  parameters?: Record<string, any>;
  dataSource?: Record<string, any>[];
  subDataSources?: Record<string, Record<string, any>[]>;
  options?: {
    embedImages?: boolean;
    encoding?: string;
    locale?: string;
  };
}

/**
 * 导出报表
 */
export async function exportReport(request: ExportRequest): Promise<Blob> {
  const response = await axios.post('/api/export', request, {
    responseType: 'blob',
    headers: {
      'Content-Type': 'application/json',
    },
  });

  return response.data;
}

/**
 * 下载报表文件
 */
export async function downloadReport(
  request: ExportRequest,
  filename: string = 'report'
): Promise<void> {
  const blob = await exportReport(request);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${filename}.${request.format}`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
```

### 2. 在Vue组件中使用

```vue
<!-- src/components/ExportButton.vue -->

<template>
  <div class="export-buttons">
    <button @click="exportToPdf" :disabled="loading">
      📄 Export PDF
    </button>
    <button @click="exportToHtml" :disabled="loading">
      🌐 Export HTML
    </button>
    <button @click="exportToExcel" :disabled="loading">
      📊 Export Excel
    </button>
    <button @click="exportToWord" :disabled="loading">
      📝 Export Word
    </button>
    <button @click="exportToCsv" :disabled="loading">
      📋 Export CSV
    </button>

    <div v-if="loading" class="loading">
      Exporting...
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { exportReport, downloadReport } from '@/composables/useExport';
import { useDesignerStore } from '@/stores/designer';

const designerStore = useDesignerStore();
const loading = ref(false);

const getExportRequest = (format: string) => ({
  jrxml: designerStore.currentJrxml,
  format,
  parameters: designerStore.parameters,
  dataSource: designerStore.dataSource,
  subDataSources: designerStore.subDataSources,
});

const exportToPdf = async () => {
  loading.value = true;
  try {
    await downloadReport(getExportRequest('pdf'), 'report');
  } finally {
    loading.value = false;
  }
};

const exportToHtml = async () => {
  loading.value = true;
  try {
    await downloadReport(getExportRequest('html'), 'report');
  } finally {
    loading.value = false;
  }
};

const exportToExcel = async () => {
  loading.value = true;
  try {
    await downloadReport(getExportRequest('excel'), 'report');
  } finally {
    loading.value = false;
  }
};

const exportToWord = async () => {
  loading.value = true;
  try {
    await downloadReport(getExportRequest('word'), 'report');
  } finally {
    loading.value = false;
  }
};

const exportToCsv = async () => {
  loading.value = true;
  try {
    await downloadReport(getExportRequest('csv'), 'report');
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## API详细说明

### POST /api/export

通用导出接口，支持所有格式。

**请求体**：

```json
{
  "jrxml": "<jasperReport>...</jasperReport>",
  "format": "pdf",
  "parameters": {
    "REPORT_TITLE": "销售报表"
  },
  "dataSource": [
    {
      "id": 1,
      "name": "产品A",
      "amount": 100.00
    }
  ],
  "subDataSources": {
    "tableDataset": [
      {
        "col1": "值1",
        "col2": "值2"
      }
    ]
  },
  "options": {
    "embedImages": true,
    "encoding": "UTF-8",
    "locale": "zh_CN"
  }
}
```

**响应**：

- **PDF/Excel/Word**：二进制文件流
  - Content-Type: application/pdf (或对应类型)
  - Content-Disposition: attachment; filename=report.pdf
  
- **HTML**：HTML内容
  - Content-Type: text/html; charset=UTF-8
  
- **CSV**：CSV内容
  - Content-Type: text/csv; charset=UTF-8

**示例**：

```typescript
// 使用axios
import axios from 'axios';

const response = await axios.post('/api/export', {
  jrxml: jrxmlContent,
  format: 'pdf',
  parameters: { REPORT_TITLE: 'My Report' },
  dataSource: [{ id: 1, name: 'Test' }],
}, {
  responseType: 'blob',
});

// 下载文件
const url = window.URL.createObjectURL(new Blob([response.data]));
const link = document.createElement('a');
link.href = url;
link.download = 'report.pdf';
link.click();
```

---

### POST /api/export/pdf

快速导出PDF。

**请求体**：同通用接口，但`format`字段会自动设为`pdf`

**响应**：PDF二进制文件流

**示例**：

```typescript
const response = await axios.post('/api/export/pdf', {
  jrxml: jrxmlContent,
  parameters: { REPORT_TITLE: 'My Report' },
  dataSource: [{ id: 1, name: 'Test' }],
}, {
  responseType: 'blob',
});
```

---

### POST /api/export/html

快速导出HTML。

**请求体**：同通用接口，但`format`字段会自动设为`html`

**响应**：HTML字符串

**示例**：

```typescript
const response = await axios.post('/api/export/html', {
  jrxml: jrxmlContent,
  parameters: { REPORT_TITLE: 'My Report' },
});

// 在新窗口中打开HTML
const htmlContent = response.data;
const newWindow = window.open('', '_blank');
newWindow.document.write(htmlContent);
```

---

### POST /api/export/excel

快速导出Excel (xlsx)。

**请求体**：同通用接口，但`format`字段会自动设为`xlsx`

**响应**：Excel二进制文件流

**示例**：

```typescript
const response = await axios.post('/api/export/excel', {
  jrxml: jrxmlContent,
  parameters: { REPORT_TITLE: 'My Report' },
}, {
  responseType: 'blob',
});
```

---

### POST /api/export/word

快速导出Word (docx)。

**请求体**：同通用接口，但`format`字段会自动设为`docx`

**响应**：Word二进制文件流

**示例**：

```typescript
const response = await axios.post('/api/export/word', {
  jrxml: jrxmlContent,
  parameters: { REPORT_TITLE: 'My Report' },
}, {
  responseType: 'blob',
});
```

---

### POST /api/export/csv

快速导出CSV。

**请求体**：同通用接口，但`format`字段会自动设为`csv`

**响应**：CSV字符串

**示例**：

```typescript
const response = await axios.post('/api/export/csv', {
  jrxml: jrxmlContent,
  parameters: { REPORT_TITLE: 'My Report' },
});

// 下载CSV文件
const csvContent = response.data;
const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
const url = window.URL.createObjectURL(blob);
const link = document.createElement('a');
link.href = url;
link.download = 'report.csv';
link.click();
```

---

### POST /api/export/validate

验证JRXML是否有效。

**请求体**：

```json
{
  "jrxml": "<jasperReport>...</jasperReport>"
}
```

**响应**：

```json
{
  "valid": true,
  "message": "JRXML is valid and can be exported"
}
```

或

```json
{
  "valid": false,
  "message": "Validation failed: Invalid XML format"
}
```

**示例**：

```typescript
const response = await axios.post('/api/export/validate', {
  jrxml: jrxmlContent,
});

if (response.data.valid) {
  console.log('JRXML is valid');
} else {
  console.error('JRXML is invalid:', response.data.message);
}
```

---

### GET /api/export/formats

获取支持的格式列表。

**响应**：

```json
{
  "pdf": "PDF Document",
  "html": "HTML Web Page",
  "xlsx": "Excel 2007+ Workbook",
  "xls": "Excel 97-2003 Workbook",
  "docx": "Word Document",
  "csv": "CSV Spreadsheet",
  "rtf": "RTF Document"
}
```

**示例**：

```typescript
const response = await axios.get('/api/export/formats');
console.log('Supported formats:', response.data);
```

---

## 高级用法

### 1. 批量导出

```typescript
async function batchExport(jrxml: string, formats: string[]) {
  const results = await Promise.all(
    formats.map(async (format) => {
      const response = await axios.post('/api/export', {
        jrxml,
        format,
      }, {
        responseType: 'blob',
      });

      return {
        format,
        blob: response.data,
      };
    })
  );

  // 下载所有文件
  results.forEach(({ format, blob }) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `report.${format}`;
    link.click();
  });
}

// 使用
await batchExport(jrxmlContent, ['pdf', 'html', 'excel']);
```

### 2. 导出预览

```typescript
async function exportPreview(jrxml: string) {
  // 先验证JRXML
  const validation = await axios.post('/api/export/validate', { jrxml });
  if (!validation.data.valid) {
    throw new Error('Invalid JRXML: ' + validation.data.message);
  }

  // 导出HTML用于预览
  const response = await axios.post('/api/export/html', {
    jrxml,
  }, {
    responseType: 'text',
  });

  return response.data;
}

// 在iframe中显示预览
const html = await exportPreview(jrxmlContent);
const iframe = document.getElementById('preview-frame') as HTMLIFrameElement;
iframe.srcdoc = html;
```

### 3. 自定义导出选项

```typescript
async function exportWithOptions(jrxml: string) {
  const response = await axios.post('/api/export', {
    jrxml,
    format: 'excel',
    options: {
      embedImages: false,  // 不嵌入图片
      encoding: 'GBK',     // 使用GBK编码
      locale: 'zh_CN',     // 中文本地化
    },
  });

  return response.data;
}
```

---

## 错误处理

### 常见错误

1. **JRXML格式错误**
   ```json
   {
     "success": false,
     "error": "Export failed: Invalid XML format"
   }
   ```

2. **不支持的格式**
   ```json
   {
     "success": false,
     "error": "Export failed: Unsupported export format: xxx"
   }
   ```

3. **服务器错误**
   ```json
   {
     "success": false,
     "error": "Export failed: Internal server error"
   }
   ```

### 错误处理示例

```typescript
async function safeExport(jrxml: string, format: string) {
  try {
    const response = await axios.post('/api/export', {
      jrxml,
      format,
    }, {
      responseType: 'blob',
    });

    return response.data;

  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 400) {
        // 客户端错误
        const errorData = error.response.data;
        console.error('Export validation error:', errorData.error);
        throw new Error(errorData.error);
      } else if (error.response?.status === 500) {
        // 服务器错误
        console.error('Server error during export');
        throw new Error('Server error occurred during export');
      }
    }
    throw error;
  }
}
```

---

## 性能优化

### 1. 异步导出

对于大文件，可以使用异步导出：

```typescript
async function asyncExport(jrxml: string, format: string) {
  // 发起导出请求
  const response = await axios.post('/api/export', {
    jrxml,
    format,
  });

  // 返回下载链接
  return {
    downloadUrl: response.data.downloadUrl,
    expiresAt: response.data.expiresAt,
  };
}
```

### 2. 缓存

对于相同的JRXML，可以缓存导出结果：

```typescript
const exportCache = new Map<string, Blob>();

async function cachedExport(jrxml: string, format: string) {
  const cacheKey = `${format}_${hashCode(jrxml)}`;

  if (exportCache.has(cacheKey)) {
    return exportCache.get(cacheKey);
  }

  const blob = await exportReport({ jrxml, format });
  exportCache.set(cacheKey, blob);

  return blob;
}
```

---

## 测试

### 单元测试

```typescript
// src/__tests__/export.test.ts

import { exportReport, downloadReport } from '@/composables/useExport';
import axios from 'axios';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('Export Service', () => {
  it('should export to PDF', async () => {
    mockedAxios.post.mockResolvedValue({
      data: new Blob(['test'], { type: 'application/pdf' }),
    });

    const blob = await exportReport({
      jrxml: '<jasperReport/>',
      format: 'pdf',
    });

    expect(blob).toBeInstanceOf(Blob);
    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/api/export',
      expect.objectContaining({ format: 'pdf' }),
      expect.any(Object)
    );
  });

  it('should throw error for invalid JRXML', async () => {
    mockedAxios.post.mockRejectedValue({
      response: {
        status: 400,
        data: { error: 'Invalid XML format' },
      },
    });

    await expect(
      exportReport({
        jrxml: 'invalid',
        format: 'pdf',
      })
    ).rejects.toThrow();
  });
});
```

---

## 注意事项

1. **文件大小限制**：默认最大10MB，可在配置文件中调整
2. **支持的JRXML版本**：JasperReports 6.x
3. **中文支持**：UTF-8编码，需要配置中文字体
4. **图片嵌入**：默认嵌入图片到文档中，可在options中禁用
5. **浏览器兼容性**：Chrome 60+, Firefox 55+, Safari 11+, Edge 79+

---

## 相关文档

- [JasperReports导出文档](https://jasperreports.sourceforge.net/sample.reference/)
- [Vue.js组合式API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Axios文档](https://axios-http.com/docs/intro)
