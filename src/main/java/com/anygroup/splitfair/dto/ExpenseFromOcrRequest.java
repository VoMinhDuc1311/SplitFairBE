package com.anygroup.splitfair.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ExpenseFromOcrRequest {

    private UUID billId;
    private UUID paidBy;
    private String description;
    private BigDecimal totalAmount;

    private List<ShareItem> shares;

    @Data
    public static class ShareItem {
        private UUID userId;
        private BigDecimal amount;
    }
}
