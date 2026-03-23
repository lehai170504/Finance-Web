package com.homie.finance.controller;

import com.homie.finance.dto.ApiResponse;
import com.homie.finance.entity.Category;
import com.homie.finance.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // 💡 Nhớ import cái này
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "3. Category", description = "Quản lý các Nhóm / Phân loại tiền tệ")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // Mở cho mọi User (Ai cũng xem được để chọn lúc thêm giao dịch)
    @GetMapping
    @Operation(summary = "Lấy danh sách danh mục")
    public ApiResponse<List<Category>> getAllCategories() {
        return new ApiResponse<>(200, "Lấy danh sách thành công!", categoryService.getAllCategories());
    }

    //CHỈ ADMIN (Thêm mới)
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Tạo danh mục mới (Chỉ Admin)")
    public ApiResponse<Category> createCategory(@Valid @RequestBody Category category) {
        return new ApiResponse<>(201, "Tạo danh mục mới thành công!", categoryService.createCategory(category));
    }

    //CHỈ ADMIN (Sửa)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Cập nhật danh mục (Chỉ Admin)")
    public ApiResponse<Category> updateCategory(@PathVariable String id, @Valid @RequestBody Category category) {
        return new ApiResponse<>(200, "Cập nhật danh mục thành công!", categoryService.updateCategory(id, category));
    }

    //CHỈ ADMIN (Xóa)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Xóa danh mục (Chỉ Admin)")
    public ApiResponse<String> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return new ApiResponse<>(200, "Xóa danh mục thành công!", null);
    }
}