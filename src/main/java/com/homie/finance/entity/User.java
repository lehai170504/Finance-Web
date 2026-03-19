package com.homie.finance.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Column(unique = true) // Đảm bảo mỗi người 1 username, không ai trùng ai
    @Schema(description = "Tên tài khoản", example = "homiedev")
    private String username;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Schema(description = "Mật khẩu (Sẽ được mã hóa)", example = "123456")
    private String password;

    @Email(message = "Email không hợp lệ")
    @Schema(description = "Email liên hệ", example = "homie@gmail.com")
    private String email;
    private String otp;
    private java.time.Instant otpExpiry;
}