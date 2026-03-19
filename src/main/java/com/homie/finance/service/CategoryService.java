package com.homie.finance.service;

import com.homie.finance.entity.Category;
import com.homie.finance.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    // Gọi ông Thủ kho lên để sai vặt
    @Autowired
    private CategoryRepository categoryRepository;

    // Món 1: Thêm một danh mục mới vào kho
    public Category createCategory(Category category) {
        return categoryRepository.save(category); // Lệnh save() có sẵn nhờ JpaRepository
    }

    // Món 2: Lấy toàn bộ danh mục đang có trong kho
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(); // Lệnh findAll() cũng có sẵn luôn
    }
}