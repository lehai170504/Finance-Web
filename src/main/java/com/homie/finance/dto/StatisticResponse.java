package com.homie.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // Bắt buộc phải có cái này để Spring ngầm định gắp dữ liệu bỏ vào
@NoArgsConstructor
public class StatisticResponse {

    @Schema(description = "Tên danh mục", example = "Ăn uống")
    private String categoryName;

    @Schema(description = "Loại (INCOME/EXPENSE)", example = "EXPENSE")
    private String categoryType;

    @Schema(description = "Tổng tiền của danh mục này", example = "1500000")
    private Double totalAmount;
}