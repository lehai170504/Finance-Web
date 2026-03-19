package com.homie.finance.service;

import com.homie.finance.entity.RefreshToken;
import com.homie.finance.entity.User;
import com.homie.finance.repository.RefreshTokenRepository;
import com.homie.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;

    @Transactional // Quan trọng: Phải có để thực hiện xóa và thêm trong 1 phiên
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        //Xóa token cũ của User này trước khi tạo cái mới
        refreshTokenRepository.deleteByUser(user);
        // Nhớ flush để database thực thi lệnh xóa ngay lập tức
        refreshTokenRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(2592000)); // 30 ngày
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại!");
        }
        return token;
    }

    @Transactional // Phải có cái này để DB cho phép xóa
    public void deleteByUserId(String userId) {
        // Tìm user trước
        userRepository.findById(userId).ifPresent(user -> {
            // Xóa tất cả Refresh Token liên quan đến User này
            refreshTokenRepository.deleteByUser(user);
        });
    }
}