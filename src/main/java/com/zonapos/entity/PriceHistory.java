package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price_id", nullable = false)
    private Long priceId;

    @Column(name = "selling_price", nullable = false)
    private Double sellingPrice;

    @Column(name = "purchase_price", nullable = false)
    private Double purchasePrice;

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

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
