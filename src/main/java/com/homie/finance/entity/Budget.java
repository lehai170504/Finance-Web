package com.homie.finance.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "budgets")
@Data
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Double limitAmount; // Số tiền tối đa được phép tiêu
    private int month;          // Tháng áp dụng
    private int year;           // Năm áp dụng

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;  // Ngân sách này dành cho danh mục nào?

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;          // Của user nào?
}