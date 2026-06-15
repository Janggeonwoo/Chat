package com.example.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 혹시나 yml 경로 끝에 슬래시가 누락되었을 경우를 대비해 자바 코드로 안전장치를 칩니다.
        String location = uploadDir;
        if (!location.endsWith("/")) {
            location += "/";
        }

        // 윈도우 물리 경로를 스프링 리소스 규격(file:///)으로 완벽하게 강제 치환합니다.
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }

        System.out.println("🌐 [정적 리소스 매핑 활성화]");
        System.out.println("👉 웹 요청 주소: http://localhost:8080/uploads/**");
        System.out.println("👉 매핑될 실제 폴더: " + location);

        // http://localhost:8080/uploads/파일명.png 로 요청이 오면
        // 본체 하드디스크의 C:/workspace/Chat/IMAGE/파일명.png 를 꺼내주도록 연결!
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}