package com.zonapos.controller;

import com.zonapos.dto.CustomerDto;
import com.zonapos.dto.TransactionResponse;
import com.zonapos.entity.User;
import com.zonapos.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getCustomers(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerService.getCustomersByTenant(currentUser.getTenantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(
            @Valid @RequestBody CustomerDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerService.createCustomer(dto, currentUser));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getCustomerTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerTransactions(id));
    }
}
