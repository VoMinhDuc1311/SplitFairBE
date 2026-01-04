package com.anygroup.splitfair.enums;

public enum NotificationType {
    GROUP_INVITE,
    EXPENSE_ADDED,
    DEBT_PAYMENT_REQUEST,   // Con nợ nhấn "Trả"
    DEBT_PAYMENT_REJECTED,  // Chủ nợ từ chối
    DEBT_SETTLED,           // Chủ nợ xác nhận đã nhận
    DEBT_REMINDER,
    SYSTEM
}
