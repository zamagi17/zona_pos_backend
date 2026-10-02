package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cash_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String type; // CASH_IN, CASH_OUT

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 100)
    private String category; // Operasional, Bahan, Tambah Modal, Kurir, Lainnya

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
