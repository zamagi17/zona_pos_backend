package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_id", nullable = false)
    private Long trxId;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "base_price", nullable = false)
    @Builder.Default
    private Double basePrice = 0.0; // modal / purchase price

    @Column(name = "unit_price", nullable = false)
    @Builder.Default
    private Double unitPrice = 0.0; // selling price

    @Column(nullable = false)
    @Builder.Default
    private Double discount = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double tax = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double subtotal = 0.0;

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
