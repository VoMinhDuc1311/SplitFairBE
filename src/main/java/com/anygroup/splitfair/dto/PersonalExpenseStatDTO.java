package com.anygroup.splitfair.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class PersonalExpenseStatDTO {
    private UUID userId;
    private BigDecimal totalAmount;
    private Instant from;
    private Instant to;
}
