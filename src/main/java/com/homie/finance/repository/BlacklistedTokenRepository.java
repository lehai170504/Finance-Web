package com.homie.finance.repository;

import com.homie.finance.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    // Kiểm tra xem Token này có nằm trong danh sách đen không
    boolean existsByToken(String token);

    // (Tùy chọn) Hàm này để sau này bạn viết một cái Cron Job
    // tự động xóa các token đã hết hạn cho nhẹ Database
    void deleteByExpiryDateBefore(Instant now);
}