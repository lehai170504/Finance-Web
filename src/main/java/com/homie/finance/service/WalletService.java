package com.homie.finance.service;

import com.homie.finance.entity.User;
import com.homie.finance.entity.Wallet;
import com.homie.finance.repository.UserRepository;
import com.homie.finance.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletService {
    @Autowired
    private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;

    public Wallet createWallet(Wallet wallet, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        wallet.setUser(user);
        return walletRepository.save(wallet);
    }

    public List<Wallet> getMyWallets(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return walletRepository.findByUser(user);
    }

    // Logic Chuyển tiền giữa 2 ví (Transfer)
    @Transactional
    public void transferMoney(String fromId, String toId, Double amount) {
        Wallet fromWallet = walletRepository.findById(fromId).orElseThrow();
        Wallet toWallet = walletRepository.findById(toId).orElseThrow();

        if (fromWallet.getBalance() < amount) {
            throw new IllegalArgumentException("Số dư ví nguồn không đủ!");
        }

        fromWallet.setBalance(fromWallet.getBalance() - amount);
        toWallet.setBalance(toWallet.getBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }

    public Double getTotalBalance(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Lỗi xác thực"));
        Double total = walletRepository.sumBalanceByUser(user);
        return total != null ? total : 0.0;
    }
}
