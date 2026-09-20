package com.zonapos.service;

import com.zonapos.dto.CustomerDto;
import com.zonapos.dto.TransactionResponse;
import com.zonapos.entity.Customer;
import com.zonapos.entity.Transaction;
import com.zonapos.entity.User;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.CustomerRepository;
import com.zonapos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public List<CustomerDto> getCustomersByTenant(Long tenantId) {
        return customerRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    public CustomerDto getCustomerById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pelanggan tidak ditemukan"));
        return mapToDto(c);
    }

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto, User currentUser) {
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (customerRepository.findByTenantIdAndPhone(currentUser.getTenantId(), dto.getPhone()).isPresent()) {
                throw new BadRequestException("Nomor telepon pelanggan sudah terdaftar");
            }
        }

        Customer customer = Customer.builder()
                .tenantId(currentUser.getTenantId())
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .build();
        customer = customerRepository.save(customer);
        return mapToDto(customer);
    }

    public List<TransactionResponse> getCustomerTransactions(Long customerId) {
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return transactions.stream()
                .map(t -> transactionService.getTransactionDetail(t.getId()))
                .collect(Collectors.toList());
    }

    private CustomerDto mapToDto(Customer c) {
        return CustomerDto.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
