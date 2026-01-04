package com.anygroup.splitfair.service.impl;

import com.anygroup.splitfair.dto.BalanceDTO;
import com.anygroup.splitfair.dto.DebtDTO;
import com.anygroup.splitfair.dto.VietQrDTO;
import com.anygroup.splitfair.enums.DebtStatus;
import com.anygroup.splitfair.enums.NotificationType;
import com.anygroup.splitfair.mapper.DebtMapper;
import com.anygroup.splitfair.model.*;
import com.anygroup.splitfair.repository.*;
import com.anygroup.splitfair.service.DebtService;
import com.anygroup.splitfair.service.NotificationService;
import com.anygroup.splitfair.service.VietQrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DebtServiceImpl implements DebtService {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;
    private final ExpenseShareRepository expenseShareRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BillRepository billRepository;
    private final VietQrService vietQrService;
// Inject BillRepository


    @Override
    public List<DebtDTO> getAllDebts() {
        return debtRepository.findAll()
                .stream()
                .map(debtMapper::toDTO)
                .collect(Collectors.toList());
    }

    //  nợ theo ID
    @Override
    public DebtDTO getDebtById(UUID id) {
        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Debt not found with id: " + id));
        return debtMapper.toDTO(debt);
    }

    // Tạo nợ thủ công
    @Override
    public DebtDTO createDebt(DebtDTO dto) {
        Debt debt = debtMapper.toEntity(dto);
        debt.setStatus(DebtStatus.UNSETTLED);
        debt = debtRepository.save(debt);
        return debtMapper.toDTO(debt);
    }


    @Override
    public DebtDTO updateDebt(DebtDTO dto) {
        if (dto.getId() == null) throw new RuntimeException("Debt ID is required for update");

        Debt existing = debtRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Debt not found with id: " + dto.getId()));

        Debt updated = debtMapper.toEntity(dto);
        updated.setId(existing.getId());

        updated = debtRepository.save(updated);
        return debtMapper.toDTO(updated);
    }


    @Override
    public void deleteDebt(UUID id) {
        debtRepository.deleteById(id);
    }


    private Bill getOrCreateSettlementBill(Group group, User payer, User payee, BigDecimal amountToAdd) {
        // 1. Tìm các Bill thanh toán trong ngày của Payer trong Group này
        java.time.Instant startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        
        List<Bill> recentBills = billRepository.findByGroup(group).stream()
                .filter(b -> b.getCreatedBy().getId().equals(payer.getId()))
                .filter(b -> Boolean.TRUE.equals(b.getIsPayment()))
                .filter(b -> b.getCreatedTime().isAfter(startOfDay))
                .collect(Collectors.toList());

        for (Bill bill : recentBills) {
            // Kiểm tra xem Bill này có phải trả cho Payee không
            List<Expense> expenses = expenseRepository.findByBill(bill);
            if (!expenses.isEmpty()) {
                Expense firstEx = expenses.get(0);
                List<ExpenseShare> shares = expenseShareRepository.findByExpense(firstEx);
                if (!shares.isEmpty()) {
                    User receiver = shares.get(0).getUser();
                    if (receiver.getId().equals(payee.getId())) {
                        // Found matching bill!
                        bill.setTotalAmount(bill.getTotalAmount().add(amountToAdd));
                        return billRepository.save(bill);
                    }
                }
            }
        }

        // Không tìm thấy -> Tạo mới
        Bill newBill = new Bill();
        newBill.setGroup(group);
        newBill.setDescription(payer.getUserName() + " thanh toán nợ cho " + payee.getUserName());
        newBill.setTotalAmount(amountToAdd);
        newBill.setCreatedBy(payer);
        newBill.setIsPayment(true);
        newBill.setStatus(com.anygroup.splitfair.enums.BillStatus.COMPLETED);
        return billRepository.save(newBill);
    }

    //Đánh dấu nợ đã được thanh toán
    @Override
    public DebtDTO markAsSettled(UUID id) {
        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Debt not found with id: " + id));
        
        if (debt.getStatus() == DebtStatus.SETTLED) {
             return debtMapper.toDTO(debt);
        }

        debt.setStatus(DebtStatus.SETTLED);
        
        if (debt.getExpense() != null && debt.getExpense().getBill() != null && debt.getExpense().getBill().getGroup() != null) {
             Group group = debt.getExpense().getBill().getGroup();
             User payer = debt.getAmountFrom();
             User payee = debt.getAmountTo();
             BigDecimal amount = debt.getAmount();

             // a. Get or Create Bill (Merged)
             Bill bill = getOrCreateSettlementBill(group, payer, payee, amount);

             // b. Create Expense
             Expense expense = new Expense();
             expense.setBill(bill);
             expense.setPaidBy(payer);
             expense.setCreatedBy(payer);
             expense.setAmount(amount);
             
             String originalDesc = (debt.getExpense().getDescription() != null) 
                 ? debt.getExpense().getDescription() 
                 : "Khoản nợ cũ";
             expense.setDescription("Trả nợ: " + originalDesc);
             
             expense.setStatus(com.anygroup.splitfair.enums.ExpenseStatus.COMPLETED);
             expense = expenseRepository.save(expense);

             // c. Create ExpenseShare
             ExpenseShare share = new ExpenseShare();
             share.setExpense(expense);
             share.setUser(payee);
             share.setShareAmount(amount);
             share.setPercentage(BigDecimal.valueOf(100));
             share.setStatus(com.anygroup.splitfair.enums.ShareStatus.PAID);
             expenseShareRepository.save(share);
        }

        debtRepository.save(debt);

        // Gửi thông báo
        User payer = debt.getAmountFrom();
        User creditor = debt.getAmountTo();
        String groupName = "";
        if (debt.getExpense() != null && debt.getExpense().getBill() != null && debt.getExpense().getBill().getGroup() != null) {
             groupName = " trong " + debt.getExpense().getBill().getGroup().getGroupName();
        }

        notificationService.createNotification(
                creditor.getId(),
                "Thanh toán nợ",
                payer.getUserName() + " đã thanh toán " + debt.getAmount() + "đ" + groupName,
                NotificationType.DEBT_SETTLED,
                debt.getExpense().getId().toString()
        );

        return debtMapper.toDTO(debt);
    }
    

    @Override
    @Transactional
    public void markBatchAsSettled(List<UUID> debtIds) {
        List<Debt> debts = debtRepository.findAllById(debtIds);
        
        Map<String, List<Debt>> groupedDebts = new HashMap<>();
        
        for (Debt debt : debts) {
            if (debt.getStatus() == DebtStatus.UNSETTLED) {
                String key = debt.getAmountFrom().getId() + "_" + debt.getAmountTo().getId();
                groupedDebts.computeIfAbsent(key, k -> new ArrayList<>()).add(debt);
            }
        }

        for (List<Debt> groupDebts : groupedDebts.values()) {
            if (groupDebts.isEmpty()) continue;

            Debt firstDebt = groupDebts.get(0);
            User payer = firstDebt.getAmountFrom();
            User payee = firstDebt.getAmountTo();
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Debt debt : groupDebts) {
                totalAmount = totalAmount.add(debt.getAmount());
                debt.setStatus(DebtStatus.SETTLED);
            }
            
            if (firstDebt.getExpense() != null && firstDebt.getExpense().getBill() != null && firstDebt.getExpense().getBill().getGroup() != null) {
                 Group group = firstDebt.getExpense().getBill().getGroup();
                 
                 // a. Get or Create Bill (Merged)
                 Bill bill = getOrCreateSettlementBill(group, payer, payee, totalAmount);

                 // b. Create MULTIPLE Expenses
                 for (Debt debt : groupDebts) {
                     Expense expense = new Expense();
                     expense.setBill(bill);
                     expense.setPaidBy(payer);
                     expense.setCreatedBy(payer);
                     expense.setAmount(debt.getAmount());
                     
                     String originalDesc = (debt.getExpense() != null && debt.getExpense().getDescription() != null) 
                         ? debt.getExpense().getDescription() 
                         : "Khoản nợ cũ";
                     expense.setDescription("Trả nợ: " + originalDesc);
                     
                     expense.setStatus(com.anygroup.splitfair.enums.ExpenseStatus.COMPLETED);
                     expense = expenseRepository.save(expense);

                     ExpenseShare share = new ExpenseShare();
                     share.setExpense(expense);
                     share.setUser(payee);
                     share.setShareAmount(debt.getAmount());
                     share.setPercentage(BigDecimal.valueOf(100));
                     share.setStatus(com.anygroup.splitfair.enums.ShareStatus.PAID);
                     expenseShareRepository.save(share);
                 }
            }
        }
        
        debtRepository.saveAll(debts);
    }

    // Tính toán nợ khi có Expense mới
    @Override
    public void calculateDebtsForExpense(Expense expense) {
        List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);
        User payer = expense.getPaidBy();

        for (ExpenseShare share : shares) {
            User debtor = share.getUser();
            if (debtor.getId().equals(payer.getId())) continue; // Người trả không nợ chính mình

            // Tính số tiền nợ theo phần trăm chia
            BigDecimal amount = expense.getAmount()
                    .multiply(share.getPercentage().divide(BigDecimal.valueOf(100)));

            // Kiểm tra nếu đã có nợ giữa hai người
            Optional<Debt> existing = debtRepository.findByAmountFromAndAmountTo(debtor, payer);
            if (existing.isPresent()) {
                Debt debt = existing.get();
                debt.setAmount(debt.getAmount().add(amount)); // cộng dồn
                debtRepository.save(debt);
            } else {
                Debt newDebt = Debt.builder()
                        .expense(expense)
                        .amountFrom(debtor)
                        .amountTo(payer)
                        .amount(amount)
                        .status(DebtStatus.UNSETTLED)
                        .build();
                debtRepository.save(newDebt);
            }
        }
    }

    // Lấy tổng kết số dư nợ của tất cả người dùng
    @Override
    public Map<UUID, BigDecimal> getNetBalances() {
        List<Debt> debts = debtRepository.findAll();
        Map<UUID, BigDecimal> balance = new HashMap<>();

        for (Debt d : debts) {
            UUID from = d.getAmountFrom().getId();
            UUID to = d.getAmountTo().getId();
            BigDecimal amount = d.getAmount();

            balance.put(from, balance.getOrDefault(from, BigDecimal.ZERO).subtract(amount));
            balance.put(to, balance.getOrDefault(to, BigDecimal.ZERO).add(amount));
        }

        return balance;
    }

    //Trả về dạng danh sách dễ đọc
    @Override
    public List<String> getReadableBalances() {
        Map<UUID, BigDecimal> balances = getNetBalances();

        List<String> readable = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            UUID userId = entry.getKey();
            BigDecimal amount = entry.getValue();

            //  Lấy tên người dùng đúng cách
            String userName = userRepository.findById(userId)
                    .map(User::getUserName)
                    .orElse("Unknown User");

            // Định dạng kết quả hiển thị
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                readable.add(userName + " owes " + amount.abs() + " VND");
            } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
                readable.add(userName + " should receive " + amount + " VND");
            } else {
                readable.add(userName + " is settled up");
            }
        }

        return readable;
    }

    // Lấy danh sách nợ theo người dùng
    @Override
    public List<DebtDTO> getDebtsByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<Debt> debts = debtRepository.findByAmountFrom(user);
        debts.addAll(debtRepository.findByAmountTo(user));

        return debts.stream().map(debtMapper::toDTO).collect(Collectors.toList());
    }


    @Override
    public List<BalanceDTO> getNetBalancesByGroup(UUID groupId) {
        
        // Lấy TẤT CẢ các bản ghi chia tiền (shares) trong nhóm
        List<ExpenseShare> allSharesInGroup = expenseShareRepository.findByExpense_Bill_Group_Id(groupId);
        
        // Lấy TẤT CẢ các thành viên trong nhóm (từ các bản ghi shares)
        Set<User> membersInGroup = allSharesInGroup.stream()
                                    .map(ExpenseShare::getUser)
                                    .collect(Collectors.toSet());
        
        if (membersInGroup.isEmpty()) {
            return new ArrayList<>(); // Không có ai trong nhóm
        }
        
        // Map để lưu tổng số tiền mỗi người "lẽ ra phải trả"
        Map<UUID, BigDecimal> totalOwedMap = new HashMap<>();
        
        // Map để lưu tổng số tiền mỗi người "đã thực sự trả"
        Map<UUID, BigDecimal> totalPaidMap = new HashMap<>();

        // Khởi tạo map
        for (User user : membersInGroup) {
            totalOwedMap.put(user.getId(), BigDecimal.ZERO);
            totalPaidMap.put(user.getId(), BigDecimal.ZERO);
        }

        // --- TÍNH TOÁN LẠI TỪ ĐẦU ---

        // 1. Tính tổng số tiền MỖI NGƯỜI ĐÃ TRẢ (giống logic biểu đồ tròn)
        List<Expense> allExpensesInGroup = expenseRepository.findByBill_Group_Id(groupId);
        for (Expense expense : allExpensesInGroup) {
            UUID paidById = expense.getPaidBy().getId();
            totalPaidMap.put(paidById, totalPaidMap.get(paidById).add(expense.getAmount()));
        }

        // 2. Tính tổng số tiền MỖI NGƯỜI LẼ RA PHẢI TRẢ (đọc từ shareAmount)
        for (ExpenseShare share : allSharesInGroup) {
            UUID userId = share.getUser().getId();
            BigDecimal shareAmount = share.getShareAmount();
            
            // (Dự phòng nếu shareAmount bị null do dữ liệu cũ)
            if (shareAmount == null) {
                shareAmount = share.getExpense().getAmount()
                                .multiply(share.getPercentage())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            
            totalOwedMap.put(userId, totalOwedMap.get(userId).add(shareAmount));
        }

        // 3. Tính số dư (Công nợ)
        // Net Balance = (Tổng Đã Trả) - (Tổng Lẽ Ra Phải Trả)
        return membersInGroup.stream()
                .map(user -> {
                    BigDecimal totalPaid = totalPaidMap.get(user.getId());
                    BigDecimal totalOwed = totalOwedMap.get(user.getId());
                    BigDecimal netBalance = totalPaid.subtract(totalOwed);
                    
                    return new BalanceDTO(
                            user.getId(),
                            user.getUserName(),
                            netBalance // Số âm (nợ), số dương (được trả)
                    );
                })
                .collect(Collectors.toList());
    }

    //RequestPayment
    @Override
    @Transactional
    public VietQrDTO requestPayment(UUID debtId, String email) {

        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new RuntimeException("Debt not found"));

        User payer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!debt.getAmountFrom().getId().equals(payer.getId())) {
            throw new RuntimeException("Bạn không phải người trả nợ");
        }

        if (debt.getStatus() != DebtStatus.UNSETTLED) {
            throw new RuntimeException("Khoản nợ không hợp lệ");
        }

        // 1️⃣ tạo VietQR
        VietQrDTO qr = vietQrService.generateDebtVietQr(debt);

        // 2️⃣ cập nhật trạng thái
        debt.setStatus(DebtStatus.PENDING_CONFIRMATION);
        debtRepository.save(debt);

        // 3️⃣ notification
        notificationService.createNotification(
                debt.getAmountTo().getId(),
                "Yêu cầu xác nhận thanh toán",
                payer.getUserName() + " đã chuyển tiền, vui lòng xác nhận",
                NotificationType.DEBT_PAYMENT_REQUEST,
                debt.getId().toString()
        );

        // 4️⃣ trả QR cho FE
        return qr;
    }


    //ConfirmPayment
    @Override
    public DebtDTO confirmPayment(UUID debtId, String email) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new RuntimeException("Debt not found"));

        User creditor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!debt.getAmountTo().getId().equals(creditor.getId())) {
            throw new RuntimeException("Bạn không phải chủ nợ");
        }

        if (debt.getStatus() != DebtStatus.PENDING_CONFIRMATION) {
            throw new RuntimeException("Khoản nợ chưa được yêu cầu thanh toán");
        }

        // 👉 GỌI LẠI LOGIC CŨ (rất hay)
        return markAsSettled(debtId);
    }

    //rejectPayment

    @Override
    public DebtDTO rejectPayment(UUID debtId, String email) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new RuntimeException("Debt not found"));

        User creditor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!debt.getAmountTo().getId().equals(creditor.getId())) {
            throw new RuntimeException("Bạn không phải chủ nợ");
        }

        if (debt.getStatus() != DebtStatus.PENDING_CONFIRMATION) {
            throw new RuntimeException("Khoản nợ không ở trạng thái chờ xác nhận");
        }

        debt.setStatus(DebtStatus.UNSETTLED);
        debtRepository.save(debt);

        // 🔔 Thông báo cho con nợ
        notificationService.createNotification(
                debt.getAmountFrom().getId(),
                "Từ chối thanh toán",
                creditor.getUserName() + " đã từ chối xác nhận khoản nợ",
                NotificationType.DEBT_PAYMENT_REJECTED,
                debt.getId().toString()
        );

        return debtMapper.toDTO(debt);
    }


}
