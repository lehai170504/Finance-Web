package com.homie.finance.controller;

import com.homie.finance.dto.ApiResponse;
import com.homie.finance.entity.Wallet;
import com.homie.finance.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "7. Wallet", description = "Quản lý nguồn tiền (Ví cá nhân, Thẻ ngân hàng...)")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping
    @Operation(summary = "Lấy danh sách Ví", description = "Load tất cả các ví của user đang đăng nhập.")
    public ApiResponse<List<Wallet>> getWallets() {
        return new ApiResponse<>(200, "Danh sách ví", walletService.getMyWallets());
    }

    @PostMapping
    @Operation(summary = "Tạo Ví mới", description = "Thêm một nguồn tiền mới (Ví dụ: Momo, Tiền mặt). FE chỉ cần gửi name, balance, color.")
    public ApiResponse<Wallet> create(@RequestBody Wallet wallet) {
        return new ApiResponse<>(201, "Đã tạo ví mới", walletService.createWallet(wallet));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Chuyển tiền giữa các Ví", description = "Dịch chuyển số dư từ ví A sang ví B.")
    public ApiResponse<String> transfer(@RequestParam String fromId, @RequestParam String toId, @RequestParam Double amount) {
        walletService.transferMoney(fromId, toId, amount);
        return new ApiResponse<>(200, "Chuyển tiền thành công!", null);
    }

    @GetMapping("/total-balance")
    @Operation(summary = "Lấy Tổng số dư hiện tại", description = "Cộng dồn tất cả tiền trong các ví của User.")
    public ApiResponse<Double> getTotalBalance() {
        Double total = walletService.getTotalBalance();
        return new ApiResponse<>(200, "Tổng số dư thực tế", total);
    }
}