package com.homie.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Z. Server Ping", description = "Kiểm tra tình trạng sống/chết của máy chủ")
public class HelloController {

    @GetMapping("/hello")
    @Operation(summary = "Ping Server", description = "Đảm bảo API đang hoạt động trước khi kết nối Frontend.")
    public String sayHello() {
        return "Chào homie! Máy chủ Homie Finance (V1.0) đang chạy rất mượt!";
    }
}