package com.homie.finance.controller;

import com.homie.finance.dto.ApiResponse;
import com.homie.finance.entity.Budget;
import com.homie.finance.entity.Category;
import com.homie.finance.entity.User;
import com.homie.finance.repository.BudgetRepository;
import com.homie.finance.repository.CategoryRepository;
import com.homie.finance.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "4. Budget", description = "Cài đặt hạn mức chi tiêu (Cảnh báo vượt ngân sách)")
public class BudgetController {

    @Autowired private BudgetRepository budgetRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/set")
    @Operation(summary = "Đặt Hạn mức (Ngân sách)", description = "Khóa van chi tiêu cho một danh mục cụ thể trong tháng. Nếu tiêu lố số tiền này, hệ thống sẽ báo lỗi.")
    public ApiResponse<Object> setBudget(
            @Parameter(description = "ID của Danh mục cần kiểm soát (Chỉ áp dụng cho EXPENSE)") @RequestParam String categoryId,
            @Parameter(description = "Tháng áp dụng (1-12)", example = "3") @RequestParam int month,
            @Parameter(description = "Năm áp dụng", example = "2026") @RequestParam int year,
            @Parameter(description = "Số tiền tối đa cho phép", example = "3000000") @RequestParam Double limitAmount) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));

        Budget budget = budgetRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year)
                .orElse(new Budget());

        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month);
        budget.setYear(year);
        budget.setLimitAmount(limitAmount);

        budgetRepository.save(budget);

        return new ApiResponse<>(200, "Đã set ngân sách thành công mức " + limitAmount + "đ cho danh mục " + category.getName() + "!", null);
    }
}