package com.example.pdfpreviewserver.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PdfTestClient {

    public static void main(String[] args) {
        try {
            // 读取JRXML文件
            String jrxmlPath = "src/main/resources/sample.jrxml";
            String jrxmlContent = new String(Files.readAllBytes(Paths.get(jrxmlPath)));
            
            // 创建URL连接
            URL url = new URL("http://localhost:8084/api/pdf/generate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setDoOutput(true);
            
            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jrxmlContent.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }
            
            // 获取响应状态
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            if (responseCode == 200) {
                // 保存PDF文件
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream("generated.pdf")) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    System.out.println("PDF generated successfully!");
                }
            } else {
                // 读取错误信息
                if (connection.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.out.println("Error Response: " + errorResponse.toString());
                }
            } else {
                System.out.println("No error stream available");
            }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}