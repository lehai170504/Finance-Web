package com.homie.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Mở cửa cho TẤT CẢ các đường dẫn API của mình
                .allowedOriginPatterns("*") // Cho phép TẤT CẢ các Frontend (React, Vue, Mobile) gọi vào
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Cho phép các hành động này
                .allowedHeaders("*") // Cho phép gửi mọi loại Header (đặc biệt là Authorization chứa Token JWT)
                .allowCredentials(true); // Cho phép gửi Cookie/Token
    }
}