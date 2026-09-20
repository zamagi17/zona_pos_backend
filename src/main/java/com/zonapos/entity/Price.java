package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "prices")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "selling_price", nullable = false)
    @Builder.Default
    private Double sellingPrice = 0.0;

    @Column(name = "purchase_price", nullable = false)
    @Builder.Default
    private Double purchasePrice = 0.0;

    @Column(name = "discount_percentage", nullable = false)
    @Builder.Default
    private Short discountPercentage = 0;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Double discountAmount = 0.0;

    @Column(name = "tax_percentage", nullable = false)
    @Builder.Default
    private Short taxPercentage = 0;

    @Column(name = "tax_amount", nullable = false)
    @Builder.Default
    private Double taxAmount = 0.0;

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
