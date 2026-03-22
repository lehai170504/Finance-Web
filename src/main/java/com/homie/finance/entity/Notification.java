package com.homie.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String message;
    private LocalDateTime createdAt;
    private boolean isRead = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Người nhận thông báo

    public Notification(User user, String message) {
        this.user = user;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
