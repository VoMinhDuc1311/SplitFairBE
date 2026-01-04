package com.anygroup.splitfair.dto;
import lombok.Data;

@Data
public class UserBankInfoRequest {
    private String bankCode;
    private String bankAccountNo;
    private String bankAccountName;
}
