package com.homie.finance.service;

import com.homie.finance.dto.WalletRequest;
import com.homie.finance.entity.User;
import com.homie.finance.entity.Wallet;
import com.homie.finance.repository.UserRepository;
import com.homie.finance.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
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

    // Lấy User từ Token
    private User getCurrentLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));
    }

    // 1. TẠO VÍ
    @Transactional
    public Wallet createWallet(WalletRequest request) {
        User currentUser = getCurrentLoggedInUser();
        Wallet newWallet = new Wallet();
        newWallet.setName(request.getName());
        newWallet.setBalance(request.getBalance() != null ? request.getBalance() : 0.0);
        newWallet.setColor(request.getColor());
        newWallet.setUser(currentUser);
        return walletRepository.save(newWallet);
    }

    // 2. LẤY DANH SÁCH VÍ
    @Transactional(readOnly = true)
    public List<Wallet> getMyWallets() {
        return walletRepository.findByUser(getCurrentLoggedInUser());
    }

    // 3. CHUYỂN TIỀN
    @Transactional
    public void transferMoney(String fromId, String toId, Double amount) {
        User currentUser = getCurrentLoggedInUser();
        if (!walletRepository.existsByIdAndUser(fromId, currentUser)) {
            throw new IllegalArgumentException("Ví nguồn không tồn tại hoặc không thuộc về homie!");
        }

        if (!walletRepository.existsByIdAndUser(toId, currentUser)) {
            throw new IllegalArgumentException("Ví đích không thuộc về homie!");
        }

        Wallet fromWallet = walletRepository.findById(fromId).orElseThrow();
        Wallet toWallet = walletRepository.findById(toId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví đích"));

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

    // 5. CẬP NHẬT VÍ
    @Transactional
    public Wallet updateWallet(String id, WalletRequest request) {
        User currentUser = getCurrentLoggedInUser();

        // 💡 CÁCH 2: Check quyền trước khi load nặng
        if (!walletRepository.existsByIdAndUser(id, currentUser)) {
            throw new RuntimeException("Ví không tồn tại hoặc homie không có quyền sửa!");
        }

        Wallet wallet = walletRepository.findById(id).orElseThrow();
        wallet.setName(request.getName());
        wallet.setColor(request.getColor());
        if (request.getBalance() != null) wallet.setBalance(request.getBalance());

        return walletRepository.save(wallet);
    }

    // 6. XÓA VÍ
    @Transactional
    public void deleteWallet(String id) {
        User currentUser = getCurrentLoggedInUser();

        // 💡 CÁCH 2: Check quyền xóa
        if (!walletRepository.existsByIdAndUser(id, currentUser)) {
            throw new RuntimeException("Ví không tồn tại hoặc homie không có quyền xóa!");
        }

        Wallet wallet = walletRepository.findById(id).orElseThrow();

        // Kiểm tra số dư phải bằng 0 mới được xóa
        if (wallet.getBalance() != null && wallet.getBalance() > 0) {
            throw new IllegalArgumentException("Ví vẫn còn tiền (" + wallet.getBalance() + "). Phải tẩu tán hết tiền mới được xóa ví nha!");
        }

        walletRepository.delete(wallet);
    }
}