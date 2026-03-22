package com.homie.finance.dto;

import lombok.Data;

@Data
public class WalletRequest {
    private String name;
    private Double balance;
    private String color;
}