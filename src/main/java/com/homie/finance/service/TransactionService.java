package com.homie.finance.service;

import com.homie.finance.dto.*;
import com.homie.finance.entity.*;
import com.homie.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Autowired private DebtRepository debtRepository;
    @Autowired private NotificationService notificationService;

    private User getCurrentLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));
    }

    // --- 1. TẠO GIAO DỊCH ---
    @Transactional
    public Transaction createTransaction(String walletId, String categoryId, String groupId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví!"));

        if (!wallet.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Ví này không thuộc về bạn!");
        }

        if ("EXPENSE".equals(category.getType())) {
            if (wallet.getBalance() < request.getAmount()) {
                throw new IllegalArgumentException("Số dư trong ví " + wallet.getName() + " không đủ!");
            }
            wallet.setBalance(wallet.getBalance() - request.getAmount());
            checkBudgetAndAlert(currentUser, category, request);
        } else if ("INCOME".equals(category.getType())) {
            wallet.setBalance(wallet.getBalance() + request.getAmount());
        }
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setWallet(wallet);
        transaction.setUser(currentUser);

        if (groupId != null && !groupId.isEmpty()) {
            GroupSpace group = groupSpaceRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Nhóm không tồn tại!"));

            boolean isMember = group.getMembers().stream()
                    .anyMatch(m -> m.getId().equals(currentUser.getId()));

            if (!isMember) {
                throw new IllegalArgumentException("Homie không phải thành viên của nhóm này!");
            }
            transaction.setGroupSpace(group);
            Transaction savedTx = transactionRepository.save(transaction);

            if ("EXPENSE".equals(category.getType())) {
                processSplit(savedTx, group);
            }

            notificationService.sendToGroup(group,
                    currentUser.getUsername() + " vừa chi " + savedTx.getAmount() + " cho " + category.getName(),
                    currentUser);

            return savedTx;
        }

        return transactionRepository.save(transaction);
    }

    private void processSplit(Transaction t, GroupSpace group) {
        Set<User> members = group.getMembers();
        if (members.size() <= 1) return;

        double shareAmount = t.getAmount() / members.size();

        for (User member : members) {
            if (!member.getId().equals(t.getUser().getId())) {
                Debt debt = new Debt();
                debt.setCreditor(t.getUser());
                debt.setDebtor(member);
                debt.setAmount(shareAmount);
                debt.setGroup(group);
                debt.setSettled(false);
                debtRepository.save(debt);
            }
        }
    }

    // --- 2. CẬP NHẬT GIAO DỊCH ---
    @Transactional
    public Transaction updateTransaction(String id, String newWalletId, String newCategoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();
        Transaction oldTx = transactionRepository.findById(id).orElseThrow();
        if (!oldTx.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("Không có quyền!");

        Wallet oldWallet = oldTx.getWallet();
        Category oldCategory = oldTx.getCategory();
        if (oldWallet != null && oldCategory != null) {
            if ("EXPENSE".equals(oldCategory.getType())) {
                oldWallet.setBalance(oldWallet.getBalance() + oldTx.getAmount());
            } else if ("INCOME".equals(oldCategory.getType())) {
                oldWallet.setBalance(oldWallet.getBalance() - oldTx.getAmount());
            }
            walletRepository.save(oldWallet);
        }

        Wallet newWallet = walletRepository.findById(newWalletId).orElseThrow();
        Category newCategory = categoryRepository.findById(newCategoryId).orElseThrow();

        if ("EXPENSE".equals(newCategory.getType())) {
            if (newWallet.getBalance() < request.getAmount()) throw new IllegalArgumentException("Số dư không đủ!");
            newWallet.setBalance(newWallet.getBalance() - request.getAmount());
        } else if ("INCOME".equals(newCategory.getType())) {
            newWallet.setBalance(newWallet.getBalance() + request.getAmount());
        }
        walletRepository.save(newWallet);

        oldTx.setAmount(request.getAmount());
        oldTx.setNote(request.getNote());
        oldTx.setDate(request.getDate());
        oldTx.setCategory(newCategory);
        oldTx.setWallet(newWallet);

        return transactionRepository.save(oldTx);
    }

    // --- 3. XÓA GIAO DỊCH ---
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
    }

    // --- 4. THỐNG KÊ NHÓM ---
    public GroupStatsResponse getGroupStats(String groupId, int month, int year) {
        List<Transaction> transactions = transactionRepository.findAllByGroupSpaceId(groupId).stream()
                .filter(t -> t.getDate() != null && t.getDate().getMonthValue() == month && t.getDate().getYear() == year)
                .collect(Collectors.toList());

        Double totalExpense = transactions.stream()
                .filter(t -> t.getCategory() != null && "EXPENSE".equals(t.getCategory().getType()))
                .mapToDouble(Transaction::getAmount).sum();

        Map<String, Double> byCategory = transactions.stream()
                .filter(t -> t.getCategory() != null && "EXPENSE".equals(t.getCategory().getType()))
                .collect(Collectors.groupingBy(t -> t.getCategory().getName(), Collectors.summingDouble(Transaction::getAmount)));

        Map<String, Double> byUser = transactions.stream()
                .filter(t -> t.getUser() != null)
                .collect(Collectors.groupingBy(t -> t.getUser().getUsername(), Collectors.summingDouble(Transaction::getAmount)));

        return new GroupStatsResponse(totalExpense, byCategory, byUser);
    }

    // --- 5. TÌM KIẾM & PHÂN TRANG ---

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
        return transactionRepository.findByUser(getCurrentLoggedInUser(), Pageable.unpaged())
                .getContent().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public Double getTotalByType(String type) {
        Double total = transactionRepository.sumAmountByUserAndType(getCurrentLoggedInUser(), type);
        return total != null ? total : 0.0;
    }

    public List<TransactionResponse> getTransactionsByType(String type) {
        return transactionRepository.findByUserAndCategoryType(getCurrentLoggedInUser(), type)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // --- 6. THỐNG KÊ CHI TIÊU & BUDGET ---

    public List<StatisticResponse> getCategoryStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentLoggedInUser();
        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }
        return transactionRepository.getCategoryStatistics(currentUser, startDate, endDate);
    }

    private void checkBudgetAndAlert(User currentUser, Category category, TransactionRequest request) {
        int month = request.getDate().getMonthValue();
        int year = request.getDate().getYear();

        budgetRepository.findByUserAndCategoryAndMonthAndYear(currentUser, category, month, year)
                .ifPresent(budget -> {
                    Double limit = budget.getLimitAmount();
                    LocalDate startDate = YearMonth.of(year, month).atDay(1);
                    LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
                    Double spent = transactionRepository.sumAmountByUserAndCategoryAndDateBetween(currentUser, category, startDate, endDate);
                    if (spent == null) spent = 0.0;
                    if (spent + request.getAmount() > limit) {
                        alertService.sendBudgetAlertEmail(currentUser.getEmail(), currentUser.getUsername(), category.getName(), limit);
                    }
                });
    }

    public PageResponse<TransactionResponse> getGroupTransactions(String groupId, int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        if (!groupSpaceRepository.existsByIdAndMembersContaining(groupId, currentUser)) throw new IllegalArgumentException("Không có quyền!");
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        return mapToPageResponse(transactionRepository.findByGroupSpaceId(groupId, pageable));
    }

    // --- MAPPING ---

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