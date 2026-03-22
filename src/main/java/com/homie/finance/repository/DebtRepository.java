package com.homie.finance.repository;

import com.homie.finance.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, String> {
    List<Debt> findByGroupIdAndIsSettledFalse(String groupId);
}