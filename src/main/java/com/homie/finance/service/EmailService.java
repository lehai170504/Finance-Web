package com.homie.finance.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // --- CÁC HÀM CŨ CỦA HOMIE (GIỮ NGUYÊN) ---
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("hoanghaile175@gmail.com");
            message.setTo(toEmail);
            message.setSubject("🎉 Chào mừng homie gia nhập Homie Finance!");
            message.setText("Chào " + username + ",\n\n" +
                    "Chúc mừng homie đã đăng ký thành công tài khoản trên hệ thống Homie Finance! 🚀\n" +
                    "Thân mến,\n" + "Đội ngũ Homie Dev.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail chào mừng: " + e.getMessage());
        }
    }

    public void sendSimpleEmail(String to, String content, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    // --- HÀM NÂNG CẤP (Dùng cho HTML và Async) ---

    /**
     * Gửi Email định dạng HTML (Dùng cho Báo cáo và Cảnh báo đẹp)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi HTML email: " + e.getMessage());
        }
    }
}