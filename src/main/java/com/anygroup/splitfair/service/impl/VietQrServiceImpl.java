package com.anygroup.splitfair.service.impl;

import com.anygroup.splitfair.dto.VietQrDTO;
import com.anygroup.splitfair.model.Debt;
import com.anygroup.splitfair.model.User;
import com.anygroup.splitfair.service.VietQrService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class VietQrServiceImpl implements VietQrService {

    @Override
    public VietQrDTO generateDebtVietQr(Debt debt) {

        User creditor = debt.getAmountTo();

        String bankCode = creditor.getBankCode();
        String accountNo = creditor.getBankAccountNo();
        String accountName = creditor.getBankAccountName();

        String content = "SplitFair debt " + debt.getId();

        // nội dung QR (backend dùng, FE có thể không cần)
        String qrContent = String.format(
                "%s|%s|%s|%s",
                bankCode,
                accountNo,
                debt.getAmount().toPlainString(),
                content
        );

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact.jpg?amount=%s&addInfo=%s",
                bankCode,
                accountNo,
                debt.getAmount().toPlainString(),
                content
        );

        return VietQrDTO.builder()
                .bankCode(bankCode)
                .accountNo(accountNo)
                .accountName(accountName)
                .amount(debt.getAmount())
                .content(content)
                .qrContent(qrContent)
                .qrUrl(qrUrl)
                .build();
    }

}

