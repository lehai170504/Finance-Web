package com.homie.finance.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "wallets")
@Data
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name; // Ví dụ: Tiền mặt, Techcombank, MoMo

    private Double balance = 0.0; // Số dư hiện tại trong ví

    private String color; // Mã màu để hiển thị trên App Android cho đẹp (VD: #FF0000)

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Ví này thuộc về ai
}