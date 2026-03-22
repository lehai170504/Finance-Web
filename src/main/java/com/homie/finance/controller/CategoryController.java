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
@Tag(name = "3. Category", description = "Quản lý các Nhóm / Phân loại tiền tệ")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lấy danh sách danh mục")
    public ApiResponse<List<Category>> getAllCategories() {
        return new ApiResponse<>(200, "Lấy danh sách thành công!", categoryService.getAllCategories());
    }

    @PostMapping
    @Operation(summary = "Tạo danh mục mới")
    public ApiResponse<Category> createCategory(@Valid @RequestBody Category category) {
        return new ApiResponse<>(201, "Tạo danh mục mới thành công!", categoryService.createCategory(category));
    }

    // 💡 MỚI: Cập nhật danh mục (Đổi tên, loại hoặc icon)
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục")
    public ApiResponse<Category> updateCategory(@PathVariable String id, @Valid @RequestBody Category category) {
        return new ApiResponse<>(200, "Cập nhật danh mục thành công!", categoryService.updateCategory(id, category));
    }

    // 💡 MỚI: Xóa danh mục
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa danh mục")
    public ApiResponse<String> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return new ApiResponse<>(200, "Xóa danh mục thành công!", null);
    }
}