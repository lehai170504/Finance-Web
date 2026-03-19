package com.homie.finance.repository;

import com.homie.finance.dto.StatisticResponse;
import com.homie.finance.entity.Category;
import com.homie.finance.entity.Transaction;
import com.homie.finance.entity.User; // Bắt buộc phải import User
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // 1. Lấy tất cả giao dịch của RIÊNG USER ĐÓ
    Page<Transaction> findByUser(User user, Pageable pageable);

    // 2. Tính tổng tiền theo loại (INCOME hoặc EXPENSE) của RIÊNG USER ĐÓ
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category.type = :type")
    Double sumAmountByUserAndType(@Param("user") User user, @Param("type") String type);

    // 3. Lọc danh sách giao dịch theo loại (INCOME/EXPENSE) của RIÊNG USER ĐÓ
    List<Transaction> findByUserAndCategoryType(User user, String type);

    // 4. Tìm kiếm theo từ khóa trong Ghi chú của RIÊNG USER ĐÓ
    Page<Transaction> findByUserAndNoteContainingIgnoreCase(User user, String keyword, Pageable pageable);

    // 5. Gom nhóm thống kê theo ngày tháng CỦA RIÊNG USER ĐÓ
    @Query("SELECT new com.homie.finance.dto.StatisticResponse(c.name, c.type, SUM(t.amount)) " +
            "FROM Transaction t JOIN t.category c " +
            "WHERE t.user = :user AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY c.name, c.type")
    List<StatisticResponse> getCategoryStatistics(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.date BETWEEN :startDate AND :endDate")
    Double sumAmountByUserAndCategoryAndDateBetween(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}