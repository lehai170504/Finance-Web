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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // Phải gọi thêm ông Thủ kho User
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    // 🛠 BẢO BỐI: Hàm lấy thông tin User đang đăng nhập từ Token
    private User getCurrentLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));
    }

    // 1. Lưu giao dịch mới (Gắn thẻ tên User)
    public Transaction createTransaction(String categoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser(); // Lấy user hiện tại

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục nào với ID này: " + categoryId));

        // Chỉ kiểm tra bẫy nếu đây là danh mục CHI TIÊU (EXPENSE)
        if ("EXPENSE".equals(category.getType())) {
            int month = request.getDate().getMonthValue();
            int year = request.getDate().getYear();

            // Xem tháng này có bị "vợ" set ngân sách cho khoản này không?
            budgetRepository.findByUserAndCategoryAndMonthAndYear(currentUser, category, month, year)
                    .ifPresent(budget -> {
                        Double limit = budget.getLimitAmount();

                        // Lấy ngày mùng 1 và ngày cuối của tháng đó
                        LocalDate startDate = YearMonth.of(year, month).atDay(1);
                        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

                        // Cộng dồn tiền đã tiêu từ đầu tháng tới giờ
                        Double alreadySpent = transactionRepository.sumAmountByUserAndCategoryAndDateBetween(currentUser, category, startDate, endDate);
                        if (alreadySpent == null) alreadySpent = 0.0;

                        // KIỂM TRA: Tiền đã tiêu + Tiền chuẩn bị tiêu > Hạn mức ???
                        if (alreadySpent + request.getAmount() > limit) {
                            // KÍCH HOẠT CHẾ ĐỘ "CHỬI" 🤬
                            throw new IllegalArgumentException(
                                    "Ê homie! Ngân sách '" + category.getName() + "' tháng này chỉ có " + limit +
                                            "đ thôi. Tiêu thêm quả này là lố mọe ngân sách rồi, tém tém lại đi!!!"
                            );
                        }
                    });
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setUser(currentUser); // Gắn user vào giao dịch

        return transactionRepository.save(transaction);
    }

    // 2. Lấy danh sách phân trang (Chỉ của User này)
    public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Cập nhật hàm gọi repository
        Page<Transaction> transactionPage = transactionRepository.findByUser(currentUser, pageable);

        return mapToPageResponse(transactionPage);
    }

    // 3. Tìm kiếm theo từ khóa (Chỉ của User này)
    public PageResponse<TransactionResponse> searchTransactions(String keyword, int page, int size) {
        User currentUser = getCurrentLoggedInUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Cập nhật hàm gọi repository
        Page<Transaction> transactionPage = transactionRepository.findByUserAndNoteContainingIgnoreCase(currentUser, keyword, pageable);

        return mapToPageResponse(transactionPage);
    }

    // 4. Lấy tổng tiền (Chỉ của User này)
    public Double getTotalByType(String type) {
        User currentUser = getCurrentLoggedInUser();
        Double total = transactionRepository.sumAmountByUserAndType(currentUser, type);
        return total != null ? total : 0.0;
    }

    // 5. Sửa giao dịch (Kiểm tra quyền sở hữu)
    public Transaction updateTransaction(String id, String categoryId, TransactionRequest request) {
        User currentUser = getCurrentLoggedInUser();

        return transactionRepository.findById(id).map(transaction -> {
            // Chặn ngay nếu không phải chủ nhân
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

    // 6. Xóa giao dịch (Kiểm tra quyền sở hữu)
    public void deleteTransaction(String id) {
        User currentUser = getCurrentLoggedInUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không thể xóa! Giao dịch không tồn tại với ID: " + id));

        // Chặn ngay nếu không phải chủ nhân
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Homie không có quyền xóa giao dịch của người khác đâu nhé!");
        }

        transactionRepository.delete(transaction);
    }

    // 7. Lọc Transaction theo Category (Chỉ của User này)
    public List<TransactionResponse> getTransactionsByType(String type){
        User currentUser = getCurrentLoggedInUser();

        // Cập nhật hàm gọi repository
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

    // 8. Thống kê tổng tiền (Chỉ của User này)
    public List<StatisticResponse> getCategoryStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentLoggedInUser();

        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }
        return transactionRepository.getCategoryStatistics(currentUser, startDate, endDate);
    }

    // 🛠 Hàm phụ trợ: Xào nấu từ Page của Spring sang PageResponse của mình
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

        return new PageResponse<>(
                content,
                transactionPage.getNumber(),
                transactionPage.getTotalPages(),
                transactionPage.getTotalElements()
        );
    }

    // Upload hình ảnh
    public TransactionResponse uploadReceipt(String transactionId, MultipartFile file) {
        // 🛡️ BƯỚC TỐI ƯU 1: Kiểm tra dung lượng (Tối đa 5MB)
        long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB tính bằng bytes
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Homie ơi file nặng quá! Vui lòng chọn ảnh dưới 5MB nhé.");
        }

        // 🛡️ BƯỚC TỐI ƯU 2: Kiểm tra định dạng (Chỉ nhận Ảnh)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Sai định dạng rồi! Hệ thống chỉ nhận file hình ảnh (JPG, PNG...) thôi nhé.");
        }

        User currentUser = getCurrentLoggedInUser();

        // 1. Tìm giao dịch xem có không
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch!"));

        // 2. Chặn nếu không phải chủ nhân
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Homie không được úp ảnh vào hóa đơn của người khác!");
        }

        // Nếu giao dịch này ĐÃ CÓ ảnh từ trước -> Kêu Cloudinary xóa cái ảnh đó đi cho đỡ tốn dung lượng
        if (transaction.getReceiptUrl() != null && !transaction.getReceiptUrl().isEmpty()) {
            cloudinaryService.deleteImage(transaction.getReceiptUrl());
        }

        // 3. Đưa ảnh lên mây và lấy link về
        String imageUrl = cloudinaryService.uploadImage(file);

        // 4. Lưu link vào Database
        transaction.setReceiptUrl(imageUrl);
        transactionRepository.save(transaction);

        // 5. Trả về Response (Chuyển tay sang DTO)
        TransactionResponse res = new TransactionResponse();
        res.setId(transaction.getId());
        res.setAmount(transaction.getAmount());
        res.setNote(transaction.getNote());
        res.setDate(transaction.getDate());
        res.setReceiptUrl(transaction.getReceiptUrl());
        if (transaction.getCategory() != null) {
            res.setCategoryName(transaction.getCategory().getName());
            res.setCategoryType(transaction.getCategory().getType());
        }
        return res;
    }

    //Lấy tất cả giao dịch (Không phân trang) để xuất Excel
    public List<TransactionResponse> getAllTransactionsForExport() {
        User currentUser = getCurrentLoggedInUser();

        // Dùng unpaged() để "hack" cái hàm phân trang, bắt nó lấy hết ra!
        Page<Transaction> page = transactionRepository.findByUser(currentUser, Pageable.unpaged());

        // Tái sử dụng luôn cái hàm nhào nặn DTO lúc trước, quá tiện!
        return mapToPageResponse(page).getContent();
    }
}