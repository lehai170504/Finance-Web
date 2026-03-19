package com.homie.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TransactionResponse {
    private String id;
    private Double amount;
    private String note;
    private LocalDate date;
    private String categoryName;
    private String categoryType;
    private String receiptUrl;
}