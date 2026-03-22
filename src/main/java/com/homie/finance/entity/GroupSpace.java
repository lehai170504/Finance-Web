package com.homie.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_spaces")
@Data
public class GroupSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name; // Tên nhóm: VD "Du lịch Đà Lạt"

    @Column(nullable = false, unique = true)
    private String inviteCode; // Mã code 6 ký tự để mời người khác

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Người tạo ra nhóm này

    // Quan hệ Nhiều-Nhiều: Một nhóm có nhiều thành viên, 1 user có thể tham gia nhiều nhóm
    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();
}