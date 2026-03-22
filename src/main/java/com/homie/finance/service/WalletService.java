package com.homie.finance.service;

import com.homie.finance.entity.User;
import com.homie.finance.entity.Wallet;
import com.homie.finance.repository.UserRepository;
import com.homie.finance.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletService {
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private UserRepository userRepository;

    // 💡 BẢO BỐI: Tự động lấy User đang đăng nhập từ Token
    private User getCurrentLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));
    }

    // 1. TẠO VÍ (Chỉ nhận Tên, Số dư, Màu)
    public Wallet createWallet(Wallet requestWallet) {
        User currentUser = getCurrentLoggedInUser();

        Wallet newWallet = new Wallet();
        // ID sẽ do Spring Boot và Database tự động sinh ra (UUID)
        newWallet.setName(requestWallet.getName());
        newWallet.setBalance(requestWallet.getBalance() != null ? requestWallet.getBalance() : 0.0);
        newWallet.setColor(requestWallet.getColor());

        newWallet.setUser(currentUser);

        return walletRepository.save(newWallet);
    }

    // 2. LẤY DANH SÁCH VÍ CỦA TÔI
    public List<Wallet> getMyWallets() {
        return walletRepository.findByUser(getCurrentLoggedInUser());
    }

    // 3. CHUYỂN TIỀN
    @Transactional
    public void transferMoney(String fromId, String toId, Double amount) {
        User currentUser = getCurrentLoggedInUser();

        Wallet fromWallet = walletRepository.findById(fromId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví nguồn"));
        Wallet toWallet = walletRepository.findById(toId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví đích"));

        // Bảo mật: Ví rút tiền ra BẮT BUỘC phải là ví của người đang đăng nhập
        if (!fromWallet.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Homie không thể rút tiền từ ví của người khác!");
        }

        if (fromWallet.getBalance() < amount) {
            throw new IllegalArgumentException("Số dư ví nguồn không đủ!");
        }

        fromWallet.setBalance(fromWallet.getBalance() - amount);
        toWallet.setBalance(toWallet.getBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }

    // 4. TÍNH TỔNG SỐ DƯ
    public Double getTotalBalance() {
        Double total = walletRepository.sumBalanceByUser(getCurrentLoggedInUser());
        return total != null ? total : 0.0;
    }
}