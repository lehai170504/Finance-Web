package com.homie.finance.service;

import com.homie.finance.dto.PageResponse;
import com.homie.finance.dto.StatisticResponse;
import com.homie.finance.dto.TransactionRequest;
import com.homie.finance.dto.TransactionResponse;
import com.homie.finance.entity.Category;
import com.homie.finance.entity.GroupSpace;
import com.homie.finance.entity.Transaction;
import com.homie.finance.entity.User;
import com.homie.finance.entity.Wallet;
import com.homie.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CloudinaryService cloudinaryService;
    @Autowired private UserRepository userRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private AlertService alertService;
    @Autowired private GroupSpaceRepository groupSpaceRepository;
    @Autowired private WalletRepository walletRepository;

    private User getCurrentLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));
    }

    // 1. Lưu giao dịch mới (CÓ XỬ LÝ VÍ & NHÓM)
    @Transactional
    @CacheEvict(value = "statistics", key = "#result.user.id")
    public Transaction createTransaction(String walletId, String categoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví!"));

        // Check ví có phải của người dùng này không
        if (!wallet.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Ví này không thuộc về bạn!");
        }

        // TÍNH TOÁN SỐ DƯ VÍ
        if ("EXPENSE".equals(category.getType())) {
            if (wallet.getBalance() < request.getAmount()) {
                throw new IllegalArgumentException("Số dư trong ví " + wallet.getName() + " không đủ để chi tiêu!");
            }
            wallet.setBalance(wallet.getBalance() - request.getAmount()); // Trừ tiền ví

            // Xử lý cảnh báo Budget
            checkBudgetAndAlert(currentUser, category, request);

        } else if ("INCOME".equals(category.getType())) {
            wallet.setBalance(wallet.getBalance() + request.getAmount()); // Cộng tiền ví
        }
        walletRepository.save(wallet); // Lưu lại số dư mới

        // Lưu Giao dịch
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setWallet(wallet);
        transaction.setUser(currentUser);

        // 💡 LOGIC MỚI: Nếu có truyền groupId xuống -> Gắn giao dịch này vào Nhóm
        if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
            GroupSpace group = groupSpaceRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Nhóm không tồn tại!"));

            // BẢO MẬT: Phải là thành viên mới được thêm giao dịch vào nhóm
            if (!group.getMembers().contains(currentUser)) {
                throw new IllegalArgumentException("Homie không phải thành viên của nhóm này!");
            }
            transaction.setGroupSpace(group);
        }

        return transactionRepository.save(transaction);
    }

    // 2. Sửa giao dịch (XỬ LÝ HOÀN TIỀN VÀ TRỪ LẠI TIỀN)
    @Transactional
    @CacheEvict(value = "statistics", key = "#result.user.id")
    public Transaction updateTransaction(String id, String newWalletId, String newCategoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();

        Transaction oldTx = transactionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy GD!"));
        if (!oldTx.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("Không có quyền!");

        Wallet oldWallet = oldTx.getWallet();
        Category oldCategory = oldTx.getCategory();

        // BƯỚC 1: Hoàn tác (Revert) số dư cũ
        if (oldWallet != null && oldCategory != null) {
            if ("EXPENSE".equals(oldCategory.getType())) {
                oldWallet.setBalance(oldWallet.getBalance() + oldTx.getAmount());
            } else if ("INCOME".equals(oldCategory.getType())) {
                oldWallet.setBalance(oldWallet.getBalance() - oldTx.getAmount());
            }
            walletRepository.save(oldWallet);
        }

        // BƯỚC 2: Áp dụng số dư mới
        Wallet newWallet = walletRepository.findById(newWalletId).orElseThrow(() -> new IllegalArgumentException("Ví mới không tồn tại"));
        Category newCategory = categoryRepository.findById(newCategoryId).orElseThrow();

        if ("EXPENSE".equals(newCategory.getType())) {
            if (newWallet.getBalance() < request.getAmount()) throw new IllegalArgumentException("Số dư ví mới không đủ!");
            newWallet.setBalance(newWallet.getBalance() - request.getAmount());
        } else if ("INCOME".equals(newCategory.getType())) {
            newWallet.setBalance(newWallet.getBalance() + request.getAmount());
        }
        walletRepository.save(newWallet);

        // Cập nhật thông tin giao dịch
        oldTx.setAmount(request.getAmount());
        oldTx.setNote(request.getNote());
        oldTx.setDate(request.getDate());
        oldTx.setCategory(newCategory);
        oldTx.setWallet(newWallet);

        // (Lưu ý: Nếu muốn cho phép đổi nhóm, homie có thể thêm logic update GroupId ở đây.
        // Hiện tại tạm giữ nguyên nhóm cũ nếu có).

        return transactionRepository.save(oldTx);
    }

    // 3. Xóa giao dịch (PHẢI HOÀN TIỀN LẠI CHO VÍ)
    @Transactional
    public void deleteTransaction(String id) {
        User currentUser = getCurrentLoggedInUser();
        Transaction transaction = transactionRepository.findById(id).orElseThrow();
        if (!transaction.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("Không có quyền!");

        Wallet wallet = transaction.getWallet();
        Category category = transaction.getCategory();

        if (wallet != null && category != null) {
            if ("EXPENSE".equals(category.getType())) {
                wallet.setBalance(wallet.getBalance() + transaction.getAmount());
            } else if ("INCOME".equals(category.getType())) {
                wallet.setBalance(wallet.getBalance() - transaction.getAmount());
            }
            walletRepository.save(wallet);
        }

        transactionRepository.delete(transaction);
        evictStatisticCache(currentUser.getId());
    }

    private void checkBudgetAndAlert(User currentUser, Category category, TransactionRequest request) {
        int month = request.getDate().getMonthValue();
        int year = request.getDate().getYear();

        budgetRepository.findByUserAndCategoryAndMonthAndYear(currentUser, category, month, year)
                .ifPresent(budget -> {
                    Double limit = budget.getLimitAmount();
                    LocalDate startDate = YearMonth.of(year, month).atDay(1);
                    LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
                    Double alreadySpent = transactionRepository.sumAmountByUserAndCategoryAndDateBetween(currentUser, category, startDate, endDate);
                    if (alreadySpent == null) alreadySpent = 0.0;

                    if (alreadySpent + request.getAmount() > limit) {
                        alertService.sendBudgetAlertEmail(currentUser.getEmail(), currentUser.getUsername(), category.getName(), limit);
                    }
                });
    }

    @CacheEvict(value = "statistics", key = "#userId")
    public void evictStatisticCache(String userId) { }

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

    public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        return mapToPageResponse(transactionRepository.findByUser(currentUser, pageable));
    }

    public PageResponse<TransactionResponse> searchTransactions(String keyword, int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        return mapToPageResponse(transactionRepository.findByUserAndNoteContainingIgnoreCase(currentUser, keyword, pageable));
    }

    // Lấy danh sách giao dịch của Không gian nhóm
    public PageResponse<TransactionResponse> getGroupTransactions(String groupId, int page, int size) {
        User currentUser = getCurrentLoggedInUser();

        // 💡 Dùng hàm này sẽ KHÔNG bao giờ bị lỗi Lazy Load
        if (!groupSpaceRepository.existsByIdAndMembersContaining(groupId, currentUser)) {
            throw new IllegalArgumentException("Homie không có quyền xem giao dịch của nhóm này!");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Transaction> transactionPage = transactionRepository.findByGroupSpaceId(groupId, pageable);

        return mapToPageResponse(transactionPage);
    }

    public Double getTotalByType(String type) {
        Double total = transactionRepository.sumAmountByUserAndType(getCurrentLoggedInUser(), type);
        return total != null ? total : 0.0;
    }

    public List<TransactionResponse> getTransactionsByType(String type){
        List<Transaction> transactions = transactionRepository.findByUserAndCategoryType(getCurrentLoggedInUser(), type);
        return transactions.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public TransactionResponse uploadReceipt(String transactionId, MultipartFile file) {
        User currentUser = getCurrentLoggedInUser();
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        if (!transaction.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("Không có quyền!");

        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("File quá nặng!");
        if (transaction.getReceiptUrl() != null) cloudinaryService.deleteImage(transaction.getReceiptUrl());

        transaction.setReceiptUrl(cloudinaryService.uploadImage(file));
        transactionRepository.save(transaction);
        return mapToDto(transaction);
    }

    public List<TransactionResponse> getAllTransactionsForExport() {
        return mapToPageResponse(transactionRepository.findByUser(getCurrentLoggedInUser(), Pageable.unpaged())).getContent();
    }

    private TransactionResponse mapToDto(Transaction t) {
        TransactionResponse res = new TransactionResponse();
        res.setId(t.getId());
        res.setAmount(t.getAmount());
        res.setNote(t.getNote());
        res.setDate(t.getDate());
        res.setReceiptUrl(t.getReceiptUrl());
        if (t.getCategory() != null) {
            res.setCategoryName(t.getCategory().getName());
            res.setCategoryType(t.getCategory().getType());
        }
        if (t.getWallet() != null) {
            res.setWalletName(t.getWallet().getName());
        }
        return res;
    }

    private PageResponse<TransactionResponse> mapToPageResponse(Page<Transaction> page) {
        List<TransactionResponse> content = page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());
        return new PageResponse<>(content, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }
}