package com.homie.finance.controller;

import com.homie.finance.dto.ApiResponse;
import com.homie.finance.entity.Category;
import com.homie.finance.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "3. Category", description = "Quản lý các Nhóm / Phân loại tiền tệ (Ăn uống, Lương, Xăng xe...)")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách danh mục", description = "Load tất cả các danh mục có trong hệ thống để người dùng chọn khi tạo giao dịch.")
    public ApiResponse<List<Category>> getAllCategories() {
        List<Category> data = categoryService.getAllCategories();
        return new ApiResponse<>(200, "Lấy danh sách thành công!", data);
    }

    @PostMapping
    @Operation(summary = "Tạo danh mục mới", description = "Tạo một phân loại mới. Chú ý: Cần gán đúng Type là INCOME (Thu nhập) hoặc EXPENSE (Chi tiêu).")
    public ApiResponse<Category> createCategory(@Valid @RequestBody Category category) {
        Category newData = categoryService.createCategory(category);
        return new ApiResponse<>(201, "Tạo danh mục mới thành công!", newData);
    }
}