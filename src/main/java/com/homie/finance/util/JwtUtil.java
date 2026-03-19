package com.homie.finance.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Khóa bí mật (Trong thực tế nên để ở application.properties)
    private final String SECRET_KEY = "DayLaMotCaiKhoaBiMatCucKyDaiVaKhoDoanDeBaoMatToiThieu256Bits";

    // Access Token sống 1 ngày (24h)
    private final long EXPIRATION_TIME = 86400000;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // 1. Tạo Token (In thẻ)
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Trích xuất Username từ Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. 🎯 HÀM MỚI: Trích xuất ngày hết hạn (Dùng cho logic Logout/Blacklist)
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 4. Hàm bổ trợ để trích xuất thông tin bất kỳ (Generic Claim Extractor)
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 5. Kiểm tra Token còn hạn không
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 6. Validate Token (Kiểm tra thẻ thật/giả và còn hạn không)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return !isTokenExpired(token); // Trả về true nếu đúng chữ ký VÀ còn hạn
        } catch (Exception e) {
            return false;
        }
    }
}