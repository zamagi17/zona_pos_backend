package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_id", nullable = false)
    private Long trxId;

    @Column(name = "trx_no", nullable = false, length = 100)
    private String trxNo;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false)
    @Builder.Default
    private Double subtotal = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double discount = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double tax = 0.0;

    @Column(name = "grand_total", nullable = false)
    @Builder.Default
    private Double grandTotal = 0.0;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
