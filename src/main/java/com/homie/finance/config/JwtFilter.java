package com.homie.finance.config;

import com.homie.finance.repository.BlacklistedTokenRepository;
import com.homie.finance.service.CustomUserDetailsService;
import com.homie.finance.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private BlacklistedTokenRepository blacklistRepository;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);

            // 1. Kiểm tra Blacklist trước khi làm bất cứ việc gì tốn tài nguyên
            if (blacklistRepository.existsByToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been blacklisted. Please login again.");
                return; // Chặn đứng tại đây
            }

            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                // Token lỗi định dạng hoặc hết hạn
                logger.error("Could not extract username from token", e);
            }
        }

        // 2. Nếu có username và chưa được xác thực trong phiên này (Context null)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Chỉ Load DB nếu Token còn hạn
            if (jwtUtil.validateToken(token)) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Quan trọng: Gắn thêm chi tiết Request vào Token
                authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 3. LUÔN LUÔN phải gọi dòng này để request đi tiếp (Trừ khi đã sendError ở trên)
        filterChain.doFilter(request, response);
    }
}