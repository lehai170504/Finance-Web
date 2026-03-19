package com.homie.finance.config;

import com.homie.finance.entity.Category;
import com.homie.finance.entity.Transaction;
import com.homie.finance.entity.User;
import com.homie.finance.repository.CategoryRepository;
import com.homie.finance.repository.TransactionRepository;
import com.homie.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Hàm này sẽ TỰ ĐỘNG CHẠY 1 LẦN khi Spring Boot khởi động xong
    @Override
    public void run(String... args) throws Exception {

        // Kiểm tra: Nếu bảng User chưa có ai thì mới chạy Seeding (để tránh tạo trùng lặp)
        if (userRepository.count() == 0) {
            System.out.println("🌱 Database đang trống! Bắt đầu gieo hạt (Data Seeding)...");

            // 1. Tạo tài khoản Admin mặc định
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456")); // Nhớ mã hóa mật khẩu nhé
            admin.setEmail("admin@homie.com");
            userRepository.save(admin);
            System.out.println("✅ Đã tạo User: admin / 123456");

            // 2. Tạo Danh mục (Categories)
            Category food = new Category();
            food.setName("Ăn uống");
            food.setType("EXPENSE");

            Category salary = new Category();
            salary.setName("Tiền lương");
            salary.setType("INCOME");

            categoryRepository.saveAll(List.of(food, salary));
            System.out.println("✅ Đã tạo 2 danh mục mẫu (Ăn uống, Tiền lương).");

            // 3. Tạo Giao dịch (Transactions)
            Transaction t1 = new Transaction();
            t1.setAmount(55000.0);
            t1.setNote("Ăn phở bò full topping");
            t1.setDate(LocalDate.now()); // Ngày hôm nay
            t1.setCategory(food); // Link với danh mục Ăn uống
            t1.setUser(admin); // NÂNG CẤP: Gắn admin làm chủ sở hữu

            Transaction t2 = new Transaction();
            t2.setAmount(15000000.0);
            t2.setNote("Lương tháng này");
            t2.setDate(LocalDate.now());
            t2.setCategory(salary); // Link với danh mục Tiền lương
            t2.setUser(admin); // NÂNG CẤP: Gắn admin làm chủ sở hữu

            transactionRepository.saveAll(List.of(t1, t2));
            System.out.println("✅ Đã tạo 2 giao dịch mẫu cho Admin.");
            System.out.println("🚀 Hệ thống sẵn sàng chiến đấu! Lên Swagger test thôi homie!");
        }
    }
}