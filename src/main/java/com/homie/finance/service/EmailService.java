package com.homie.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Lấy email mà homie đã cấu hình trong properties làm người gửi
            message.setFrom("homiefinance@no-reply.com");
            message.setTo(toEmail);
            message.setSubject("🎉 Chào mừng homie gia nhập Homie Finance!");
            message.setText("Chào " + username + ",\n\n" +
                    "Chúc mừng homie đã đăng ký thành công tài khoản trên hệ thống Homie Finance! 🚀\n" +
                    "Từ nay mọi chi tiêu của bạn đã có hệ thống lo. Hãy bắt đầu ghi chép ngay để quản lý tài chính thông minh hơn nhé.\n\n" +
                    "Thân mến,\n" +
                    "Đội ngũ Homie Dev.");

            // Bắt đầu gửi đi
            mailSender.send(message);
            System.out.println("Đã gửi email thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    public void sendSimpleEmail(String to, String content, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}