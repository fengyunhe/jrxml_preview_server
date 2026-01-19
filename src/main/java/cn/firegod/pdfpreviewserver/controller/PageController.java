package cn.firegod.pdfpreviewserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class PageController {

    @GetMapping("/")
    public void index(HttpServletResponse response) throws IOException {
        // 添加支持iframe嵌入的响应头
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Content-Security-Policy", "frame-ancestors *");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        
        // 重定向到静态index.html
        response.sendRedirect("/index.html");
    }
}