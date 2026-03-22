package com.homie.finance.repository;

import com.homie.finance.entity.Wallet;
import com.homie.finance.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUser(User user);

    @Query("SELECT SUM(w.balance) FROM Wallet w WHERE w.user = :user")
    Double sumBalanceByUser(@Param("user") User user);
}