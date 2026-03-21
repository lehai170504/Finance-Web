package com.homie.finance.service;

import com.homie.finance.dto.PageResponse;
import com.homie.finance.dto.StatisticResponse;
import com.homie.finance.dto.TransactionRequest;
import com.homie.finance.dto.TransactionResponse;
import com.homie.finance.entity.Category;
import com.homie.finance.entity.Transaction;
import com.homie.finance.entity.User;
import com.homie.finance.repository.BudgetRepository;
import com.homie.finance.repository.CategoryRepository;
import com.homie.finance.repository.TransactionRepository;
import com.homie.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private AlertService alertService;

    // 🛠 BẢO BỐI: Hàm lấy thông tin User đang đăng nhập từ Token
    private User getCurrentLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));
    }

    // 1. Lưu giao dịch mới
    @CacheEvict(value = "statistics", key = "#result.user.id")
    public Transaction createTransaction(String categoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));

        if ("EXPENSE".equals(category.getType())) {
            int month = request.getDate().getMonthValue();
            int year = request.getDate().getYear();

            budgetRepository.findByUserAndCategoryAndMonthAndYear(currentUser, category, month, year)
                    .ifPresent(budget -> {
                        Double limit = budget.getLimitAmount();
                        LocalDate startDate = YearMonth.of(year, month).atDay(1);
                        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

                        Double alreadySpent = transactionRepository.sumAmountByUserAndCategoryAndDateBetween(currentUser, category, startDate, endDate);
                        if (alreadySpent == null) alreadySpent = 0.0;

                        // KIỂM TRA: Nếu tiêu phát này nữa là lố?
                        if (alreadySpent + request.getAmount() > limit) {
                            // 🚀 GỌI ALERT SERVICE (Khớp tên hàm 100%)
                            alertService.sendBudgetAlertEmail(
                                    currentUser.getEmail(),
                                    currentUser.getUsername(),
                                    category.getName(),
                                    limit
                            );
                            System.out.println("⚠️ Cảnh báo lố ngân sách đã được gửi ngầm!");
                        }
                    });
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setUser(currentUser);

        return transactionRepository.save(transaction);
    }

    // 5. Sửa giao dịch (Xóa Cache cũ đi vì tiền đã thay đổi)
    @CacheEvict(value = "statistics", key = "#result.user.id")
    public Transaction updateTransaction(String id, String categoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();

        return transactionRepository.findById(id).map(transaction -> {
            if (!transaction.getUser().getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("Homie không có quyền sửa giao dịch của người khác đâu nhé!");
            }

            transaction.setAmount(request.getAmount());
            transaction.setNote(request.getNote());
            transaction.setDate(request.getDate());

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục: " + categoryId));
            transaction.setCategory(category);

            return transactionRepository.save(transaction);
        }).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch id: " + id));
    }

    // 6. Xóa giao dịch
    public void deleteTransaction(String id) {
        User currentUser = getCurrentLoggedInUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không thể xóa! Giao dịch không tồn tại với ID: " + id));

        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Homie không có quyền xóa giao dịch của người khác đâu nhé!");
        }

        transactionRepository.delete(transaction);

        // Gọi hàm giả để ép Spring kích hoạt xóa Cache
        evictStatisticCache(currentUser.getId());
    }

    // Hàm phụ trợ xóa Cache
    @CacheEvict(value = "statistics", key = "#userId")
    public void evictStatisticCache(String userId) {
        System.out.println("Đã xóa Cache Thống kê do Giao dịch bị xóa. UserId: " + userId);
    }

    // 8. Thống kê (Sử dụng Cache)
    @Cacheable(value = "statistics", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #startDate + '-' + #endDate")
    public List<StatisticResponse> getCategoryStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentLoggedInUser();

        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }
        return transactionRepository.getCategoryStatistics(currentUser, startDate, endDate);
    }

    // --- CÁC HÀM CÒN LẠI GIỮ NGUYÊN ---

    public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Transaction> transactionPage = transactionRepository.findByUser(currentUser, pageable);
        return mapToPageResponse(transactionPage);
    }

    public PageResponse<TransactionResponse> searchTransactions(String keyword, int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Transaction> transactionPage = transactionRepository.findByUserAndNoteContainingIgnoreCase(currentUser, keyword, pageable);
        return mapToPageResponse(transactionPage);
    }

    public Double getTotalByType(String type) {
        User currentUser = getCurrentLoggedInUser();
        Double total = transactionRepository.sumAmountByUserAndType(currentUser, type);
        return total != null ? total : 0.0;
    }

    public List<TransactionResponse> getTransactionsByType(String type){
        User currentUser = getCurrentLoggedInUser();
        List<Transaction> transactions = transactionRepository.findByUserAndCategoryType(currentUser, type);
        List<TransactionResponse> responseList = new ArrayList<>();
        for(Transaction t: transactions){
            TransactionResponse res = new TransactionResponse();
            res.setId(t.getId());
            res.setNote(t.getNote());
            res.setAmount(t.getAmount());
            res.setDate(t.getDate());
            if(t.getCategory() != null){
                res.setCategoryName(t.getCategory().getName());
                res.setCategoryType(t.getCategory().getType());
            }
            responseList.add(res);
        }
        return responseList;
    }

    public TransactionResponse uploadReceipt(String transactionId, MultipartFile file) {
        long MAX_FILE_SIZE = 5 * 1024 * 1024;
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File quá nặng!");
        User currentUser = getCurrentLoggedInUser();
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new IllegalArgumentException("Không thấy GD!"));
        if (!transaction.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("Không có quyền!");
        if (transaction.getReceiptUrl() != null) cloudinaryService.deleteImage(transaction.getReceiptUrl());
        String imageUrl = cloudinaryService.uploadImage(file);
        transaction.setReceiptUrl(imageUrl);
        transactionRepository.save(transaction);
        TransactionResponse res = new TransactionResponse();
        res.setId(transaction.getId());
        res.setReceiptUrl(transaction.getReceiptUrl());
        return res;
    }

    public List<TransactionResponse> getAllTransactionsForExport() {
        User currentUser = getCurrentLoggedInUser();
        Page<Transaction> page = transactionRepository.findByUser(currentUser, Pageable.unpaged());
        return mapToPageResponse(page).getContent();
    }

    private PageResponse<TransactionResponse> mapToPageResponse(Page<Transaction> transactionPage) {
        List<TransactionResponse> content = transactionPage.getContent().stream().map(t -> {
            TransactionResponse res = new TransactionResponse();
            res.setId(t.getId());
            res.setAmount(t.getAmount());
            res.setNote(t.getNote());
            res.setDate(t.getDate());
            if (t.getCategory() != null) {
                res.setCategoryName(t.getCategory().getName());
                res.setCategoryType(t.getCategory().getType());
            }
            return res;
        }).collect(Collectors.toList());
        return new PageResponse<>(content, transactionPage.getNumber(), transactionPage.getTotalPages(), transactionPage.getTotalElements());
    }
}