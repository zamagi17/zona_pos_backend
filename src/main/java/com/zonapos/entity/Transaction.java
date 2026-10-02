package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_no", nullable = false, unique = true, length = 100)
    private String trxNo;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(nullable = false)
    @Builder.Default
    private Double subtotal = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double discount = 0.0;

    @Column(name = "order_discount", nullable = false)
    @Builder.Default
    private Double orderDiscount = 0.0;

    @Column(name = "order_discount_type", length = 20)
    private String orderDiscountType; // PERCENT, FIXED

    @Column(name = "order_discount_rate")
    @Builder.Default
    private Double orderDiscountRate = 0.0;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "voucher_discount", nullable = false)
    @Builder.Default
    private Double voucherDiscount = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double tax = 0.0;

    @Column(name = "grand_total", nullable = false)
    @Builder.Default
    private Double grandTotal = 0.0;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, BOOKED, CONFIRMED, COMPLETED, CANCELED, REFUNDED

    @Column(name = "due_date")
    private LocalDateTime dueDate;

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
