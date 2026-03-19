package com.homie.finance.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Mã định danh bảo mật UUID", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Tên danh mục không được để trống đâu homie!")
    @Schema(description = "Tên của danh mục", example = "Lương tháng 10")
    private String name;

    @NotBlank(message = "Loại danh mục (INCOME/EXPENSE) là bắt buộc!")
    @Schema(description = "Phân loại thu hoặc chi", example = "INCOME")
    private String type;
}