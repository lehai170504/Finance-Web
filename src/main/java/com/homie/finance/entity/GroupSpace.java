package com.homie.finance.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_spaces")
@Data
// 💡 THÊM: Tránh lỗi vòng lặp vô hạn khi dùng Lombok với quan hệ ManyToMany
@EqualsAndHashCode(exclude = {"members", "owner"})
@ToString(exclude = {"members", "owner"})
public class GroupSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"groups", "password", "otp", "otpExpiry", "role"})
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"groups", "password", "otp", "otpExpiry", "role"})
    private Set<User> members = new HashSet<>();
}