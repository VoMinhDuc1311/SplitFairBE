package com.anygroup.splitfair.service;

import com.anygroup.splitfair.dto.BalanceDTO;
import com.anygroup.splitfair.dto.DebtDTO;
import com.anygroup.splitfair.dto.VietQrDTO;
import com.anygroup.splitfair.model.Expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DebtService {
    List<DebtDTO> getAllDebts();
    DebtDTO getDebtById(UUID id);
    DebtDTO createDebt(DebtDTO dto);
    DebtDTO updateDebt(DebtDTO dto);
    void deleteDebt(UUID id);

    void calculateDebtsForExpense(Expense expense);
    DebtDTO markAsSettled(UUID id);
    Map<UUID, BigDecimal> getNetBalances();
    List<String> getReadableBalances();
    List<DebtDTO> getDebtsByUser(UUID userId);

    List<BalanceDTO> getNetBalancesByGroup(UUID groupId);

    //thêm
    void markBatchAsSettled(List<UUID> debtIds);
    VietQrDTO requestPayment(UUID debtId, String email);

    DebtDTO confirmPayment(UUID debtId, String email);
    DebtDTO rejectPayment(UUID debtId, String email);

}