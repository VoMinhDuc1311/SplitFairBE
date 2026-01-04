package com.anygroup.splitfair.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VietQrDTO {
    private String bankCode;
    private String accountNo;
    private String accountName;
    private BigDecimal amount;
    private String content;
    private String qrContent;
    private String qrUrl;
}