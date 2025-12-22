package com.anygroup.splitfair.service.impl;

import com.anygroup.splitfair.dto.BillDTO;
import com.anygroup.splitfair.enums.BillStatus;
import com.anygroup.splitfair.mapper.BillMapper;
import com.anygroup.splitfair.model.*;
import com.anygroup.splitfair.repository.*;
import com.anygroup.splitfair.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final GroupRepository groupRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BillMapper billMapper;

    private final ExpenseRepository expenseRepository;

    @Override
    public BillDTO createBill(BillDTO dto) {
        Bill bill = billMapper.toEntity(dto);

        // Mặc định: Bill mới tạo chưa có Expense → tổng tiền = 0
        bill.setTotalAmount(BigDecimal.ZERO);

        //Nếu status chưa có, gán DRAFT
        if (bill.getStatus() == null) {
            bill.setStatus(BillStatus.DRAFT);
        }

        // Nếu chưa có createdTime, tạo mới
        if (bill.getCreatedTime() == null) {
            bill.setCreatedTime(java.time.Instant.now());
        }

        // Liên kết Group, Category, User
        if (dto.getGroupId() != null) {
            Group group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + dto.getGroupId()));
            bill.setGroup(group);
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
            bill.setCategory(category);
        }

        if (dto.getCreatedBy() != null) {
            User creator = userRepository.findById(dto.getCreatedBy())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getCreatedBy()));
            bill.setCreatedBy(creator);
        }

        bill = billRepository.save(bill);
        return billMapper.toDTO(bill);
    }

    @Override
    public BillDTO getBillById(UUID id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with id: " + id));
        return billMapper.toDTO(bill);
    }

    @Override
    public List<BillDTO> getBillsByGroup(UUID groupId) {
        return billRepository.findByGroup_Id(groupId)
                .stream()
                .map(billMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BillDTO> getBillsByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return billRepository.findByCreatedBy(user)
                .stream()
                .map(billMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BillDTO> getAllBills() {
        return billRepository.findAll()
                .stream()
                .map(billMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BillDTO updateBill(BillDTO dto) {
        Bill existing = billRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Bill not found with id: " + dto.getId()));

        //  Chỉ cập nhật khi có giá trị mới
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }

        if (dto.getTotalAmount() != null) { // tránh ghi đè null
            existing.setTotalAmount(dto.getTotalAmount());
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
            existing.setCategory(category);
        }

        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        Bill updated = billRepository.save(existing);
        return billMapper.toDTO(updated);
    }

    @Override
    @Transactional 
    public void deleteBill(UUID id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with id: " + id));
        // Lấy danh sách expense để xóa (kích hoạt cascade xóa Debt & Share)
        List<Expense> expenses = expenseRepository.findByBill(bill);
        expenseRepository.deleteAll(expenses);
        billRepository.delete(bill);
    }
}


