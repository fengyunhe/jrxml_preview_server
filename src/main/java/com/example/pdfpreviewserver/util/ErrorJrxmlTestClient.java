package com.example.pdfpreviewserver.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ErrorJrxmlTestClient {

    public static void main(String[] args) {
        try {
            // 读取有错误的JRXML文件
            File file = new File("src/main/resources/error_sample.jrxml");
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            System.out.println("发送有错误的JRXML内容进行测试...");
            System.out.println("错误描述: 元素 'title' 中不允许出现属性 'height'");
            
            // 发送HTTP请求
            URL url = new URL("http://localhost:8084/api/pdf/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(content.toString().getBytes("UTF-8"));
            }
            
            int responseCode = conn.getResponseCode();
            System.out.println("响应码: " + responseCode);
            
            // 读取响应内容
            String contentType = conn.getContentType();
            System.out.println("Content-Type: " + contentType);
            
            if (contentType != null && contentType.contains("application/json")) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                System.out.println("错误响应JSON: " + response.toString());
            } else {
                // 如果不是JSON，可能是其他类型的响应
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                System.out.println("响应内容: " + response.toString());
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
