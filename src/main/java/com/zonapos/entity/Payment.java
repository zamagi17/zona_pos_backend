package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_id", nullable = false)
    private Long trxId;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // CASH, QRIS, Transfer, Debit Card

    @Column(nullable = false)
    @Builder.Default
    private Double amount = 0.0;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PAID"; // UNPAID, PARTIAL, PAID, REFUNDED

    @Column(length = 150)
    private String reference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
