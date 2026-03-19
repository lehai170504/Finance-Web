package com.homie.finance.repository;

import com.homie.finance.entity.RefreshToken;
import com.homie.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}