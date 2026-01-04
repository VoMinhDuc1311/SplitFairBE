package com.anygroup.splitfair.controller;

import com.anygroup.splitfair.dto.BalanceDTO;
import com.anygroup.splitfair.dto.DebtDTO;
import com.anygroup.splitfair.dto.VietQrDTO;
import com.anygroup.splitfair.service.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;


    @GetMapping
    public ResponseEntity<List<DebtDTO>> getAllDebts() {
        return ResponseEntity.ok(debtService.getAllDebts());
    }


    @GetMapping("/{id}")
    public ResponseEntity<DebtDTO> getDebtById(@PathVariable UUID id) {
        return ResponseEntity.ok(debtService.getDebtById(id));
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DebtDTO>> getDebtsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(debtService.getDebtsByUser(userId));
    }


    @PostMapping
    public ResponseEntity<DebtDTO> createDebt(@RequestBody DebtDTO dto) {
        return ResponseEntity.ok(debtService.createDebt(dto));
    }


    @PutMapping("/{id}")
    public ResponseEntity<DebtDTO> updateDebt(@PathVariable UUID id, @RequestBody DebtDTO dto) {
        dto.setId(id);
        return ResponseEntity.ok(debtService.updateDebt(dto));
    }


    @PatchMapping("/{id}/settle")
    public ResponseEntity<DebtDTO> markAsSettled(@PathVariable UUID id) {
        return ResponseEntity.ok(debtService.markAsSettled(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDebt(@PathVariable UUID id) {
        debtService.deleteDebt(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/balances")
    public ResponseEntity<Map<UUID, BigDecimal>> getNetBalances() {
        return ResponseEntity.ok(debtService.getNetBalances());
    }


    @GetMapping("/balances/readable")
    public ResponseEntity<List<String>> getReadableBalances() {
        return ResponseEntity.ok(debtService.getReadableBalances());
    }


    //
    @GetMapping("/group/{groupId}/net-balances")
    public ResponseEntity<List<BalanceDTO>> getNetBalancesByGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(debtService.getNetBalancesByGroup(groupId));
    }

    //thêm
    @PostMapping("/settle-batch")
    public ResponseEntity<Void> settleBatchDebts(@RequestBody List<UUID> debtIds) {
        debtService.markBatchAsSettled(debtIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<VietQrDTO> payDebt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                debtService.requestPayment(id, authentication.getName())
        );
    }


    @PostMapping("/{id}/confirm")
    public ResponseEntity<DebtDTO> confirmDebt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                debtService.confirmPayment(id, authentication.getName())
        );
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DebtDTO> rejectDebt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                debtService.rejectPayment(id, authentication.getName())
        );
    }

}
