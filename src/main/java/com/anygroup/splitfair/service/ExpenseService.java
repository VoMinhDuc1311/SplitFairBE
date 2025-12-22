package com.anygroup.splitfair.service;

import com.anygroup.splitfair.dto.ExpenseDTO;
import com.anygroup.splitfair.dto.PaymentStatDTO;
import com.anygroup.splitfair.dto.PersonalExpenseStatDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseService {
    ExpenseDTO createExpense(ExpenseDTO dto);

    List<ExpenseDTO> getAllExpenses();

    ExpenseDTO getExpenseById(UUID id);

    List<ExpenseDTO> getExpensesByBill(UUID billId);

    List<ExpenseDTO> getExpensesCreatedByUser(UUID userId);

    List<ExpenseDTO> getExpensesPaidByUser(UUID userId);

    ExpenseDTO updateExpense(UUID id, ExpenseDTO dto);

    void deleteExpense(UUID id);

    // Thống kê tổng số tiền mỗi user trong một group
    List<PaymentStatDTO> getPaymentStatsByGroup(UUID groupId);

    List<ExpenseDTO> getExpensesByGroup(UUID groupId);
    PersonalExpenseStatDTO personalStatisticByDay(UUID userId, LocalDate date);

    PersonalExpenseStatDTO personalStatisticByWeek(UUID userId, LocalDate date);

    PersonalExpenseStatDTO personalStatisticByMonth(UUID userId, LocalDate date);
}
