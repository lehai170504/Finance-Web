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

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (categoryRepository.count() == 0) {
            System.out.println("🌱 Database đang trống! Bắt đầu gieo hạt (Data Seeding)...");

            // 1. Tạo Admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setEmail("admin@homie.com");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            System.out.println("✅ Đã tạo User: admin / 123456");

            // 2. Tạo Danh mục (CẬP NHẬT THÊM ICON CHO ĐẸP)
            Category food = new Category();
            food.setName("Ăn uống"); food.setType("EXPENSE"); food.setIcon("🍔");

            Category transport = new Category();
            transport.setName("Di chuyển"); transport.setType("EXPENSE"); transport.setIcon("🚗");

            Category shopping = new Category();
            shopping.setName("Mua sắm"); shopping.setType("EXPENSE"); shopping.setIcon("🛍️");

            Category salary = new Category();
            salary.setName("Tiền lương"); salary.setType("INCOME"); salary.setIcon("💵");

            categoryRepository.saveAll(List.of(food, transport, shopping, salary));
            System.out.println("✅ Đã tạo 4 danh mục mẫu có Icon rực rỡ.");

            // 3. Tạo Giao dịch
            Transaction t1 = new Transaction();
            t1.setAmount(55000.0);
            t1.setNote("Ăn phở bò full topping");
            t1.setDate(LocalDate.now());
            t1.setCategory(food);
            t1.setUser(admin);

            Transaction t2 = new Transaction();
            t2.setAmount(15000000.0);
            t2.setNote("Lương tháng này");
            t2.setDate(LocalDate.now());
            t2.setCategory(salary);
            t2.setUser(admin);

            transactionRepository.saveAll(List.of(t1, t2));
            System.out.println("✅ Đã tạo 2 giao dịch mẫu cho Admin.");
            System.out.println("🚀 Hệ thống sẵn sàng chiến đấu! Lên Swagger test thôi homie!");
        }
    }
}