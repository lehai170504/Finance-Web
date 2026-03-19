package com.homie.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Chưa nhập username")
    @Schema(description = "Tên đăng nhập", example = "homiedev")
    private String username;

    @NotBlank(message = "Chưa nhập password")
    @Schema(description = "Mật khẩu", example = "123456")
    private String password;
}