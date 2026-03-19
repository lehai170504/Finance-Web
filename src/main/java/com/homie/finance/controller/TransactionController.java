package com.homie.finance.controller;

import com.homie.finance.dto.*;
import com.homie.finance.entity.Transaction;
import com.homie.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "2. Transaction", description = "Quản lý luồng tiền ra vào (Thêm, Sửa, Xóa, Tìm kiếm)")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // --- NHÓM 1: LẤY DANH SÁCH & TÌM KIẾM ---

    @GetMapping
    @Operation(summary = "Lấy danh sách giao dịch (Có phân trang)", description = "Hiển thị lịch sử chi tiêu, tự động sắp xếp mới nhất lên đầu.")
    public ApiResponse<PageResponse<TransactionResponse>> getAllTransactions(
            @Parameter(description = "Số trang (Bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số item mỗi trang") @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionResponse> data = transactionService.getAllTransactions(page, size);
        return new ApiResponse<>(200, "Lấy danh sách trang " + page + " thành công!", data);
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm theo Ghi chú", description = "Tìm nhanh các khoản chi chứa từ khóa (VD: 'trà sữa'). Hỗ trợ phân trang.")
    public ApiResponse<PageResponse<TransactionResponse>> searchTransactions(
            @Parameter(description = "Từ khóa cần tìm") @RequestParam String keyword,
            @Parameter(description = "Số trang (Bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số item mỗi trang") @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionResponse> data = transactionService.searchTransactions(keyword, page, size);
        return new ApiResponse<>(200, "Kết quả tìm kiếm cho: " + keyword, data);
    }

    @GetMapping("/filter")
    @Operation(summary = "Lọc theo Thu / Chi", description = "Chỉ xem tiền vào (INCOME) hoặc tiền ra (EXPENSE).")
    public ApiResponse<List<TransactionResponse>> filterTransactions(
            @Parameter(description = "Loại (INCOME hoặc EXPENSE)", example = "EXPENSE") @RequestParam String type) {
        List<TransactionResponse> data = transactionService.getTransactionsByType(type);
        return new ApiResponse<>(200, "Đã lọc danh sách " + type, data);
    }

    // --- NHÓM 2: TÍNH TỔNG ---

    @GetMapping("/total-income")
    @Operation(summary = "Xem Tổng Thu Nhập", description = "Cộng dồn tất cả các giao dịch mang nhãn INCOME.")
    public ApiResponse<Double> getTotalIncome() {
        return new ApiResponse<>(200, "Tổng thu nhập", transactionService.getTotalByType("INCOME"));
    }

    @GetMapping("/total-expense")
    @Operation(summary = "Xem Tổng Chi Tiêu", description = "Cộng dồn tất cả các giao dịch mang nhãn EXPENSE.")
    public ApiResponse<Double> getTotalExpense() {
        return new ApiResponse<>(200, "Tổng chi tiêu", transactionService.getTotalByType("EXPENSE"));
    }

    // --- NHÓM 3: THÊM / SỬA / XÓA ---

    @PostMapping
    @Operation(summary = "Thêm giao dịch mới", description = "Ghi chép một khoản thu/chi. Hệ thống sẽ tự check ngân sách (Budget) nếu là khoản chi tiêu.")
    public ApiResponse<Transaction> createTransaction(
            @Parameter(description = "ID của Danh mục (Category)") @RequestParam String categoryId,
            @Valid @RequestBody TransactionRequest request) {
        Transaction newData = transactionService.createTransaction(categoryId, request);
        return new ApiResponse<>(201, "Đã ghi chép giao dịch mới!", newData);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa giao dịch", description = "Cập nhật lại thông tin (số tiền, ghi chú, ngày tháng) hoặc đổi sang danh mục khác.")
    public ApiResponse<Transaction> updateTransaction(
            @Parameter(description = "ID của giao dịch cần sửa") @PathVariable String id,
            @Parameter(description = "ID Danh mục (Category) mới") @RequestParam String categoryId,
            @Valid @RequestBody TransactionRequest request) {
        Transaction updatedData = transactionService.updateTransaction(id, categoryId, request);
        return new ApiResponse<>(200, "Cập nhật thành công!", updatedData);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa giao dịch", description = "Xóa vĩnh viễn giao dịch (và ảnh hóa đơn kèm theo nếu có).")
    public ApiResponse<String> deleteTransaction(
            @Parameter(description = "ID của giao dịch") @PathVariable String id) {
        transactionService.deleteTransaction(id);
        return new ApiResponse<>(200, "Đã xóa khỏi sổ!", "ID: " + id);
    }

    // --- NHÓM 4: MEDIA (UPLOAD) ---

    @PostMapping(value = "/{id}/upload-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Đính kèm ảnh hóa đơn", description = "Upload file ảnh (.jpg, .png) < 5MB lên Cloudinary và gắn link vào giao dịch này.")
    public ApiResponse<TransactionResponse> uploadReceipt(
            @Parameter(description = "ID của giao dịch") @PathVariable String id,
            @Parameter(description = "File ảnh hóa đơn") @RequestPart("file") MultipartFile file) {
        TransactionResponse updatedData = transactionService.uploadReceipt(id, file);
        return new ApiResponse<>(200, "Tải ảnh lên mây thành công!", updatedData);
    }
}