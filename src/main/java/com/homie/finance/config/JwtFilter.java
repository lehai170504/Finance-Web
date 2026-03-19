package com.homie.finance.config;

import com.homie.finance.repository.BlacklistedTokenRepository;
import com.homie.finance.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired private BlacklistedTokenRepository blacklistRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 2. THÊM ĐIỀU KIỆN: Thẻ phải chuẩn VÀ KHÔNG nằm trong danh sách đen (Logout)
            if (jwtUtil.validateToken(token)) {

                if (blacklistRepository.existsByToken(token)) {
                    // Nếu token nằm trong blacklist, coi như thẻ giả, không cho qua!
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Thẻ này đã đăng xuất, vui lòng login lại!");
                    return;
                }

                String username = jwtUtil.extractUsername(token);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}