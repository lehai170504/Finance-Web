package com.homie.finance.repository;

import com.homie.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    // 🎯 Đổi trọng tâm sang Email
    Optional<User> findByEmail(String email);

    // Giữ cái này để check trùng lúc đăng ký
    Optional<User> findByUsername(String username);
}