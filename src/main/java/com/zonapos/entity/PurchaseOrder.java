package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_no", nullable = false, unique = true, length = 50)
    private String poNo;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "storage_id")
    private Long storageId;

    @Column(name = "outlet_id")
    private Long outletId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "purchase_price", nullable = false)
    @Builder.Default
    private Double purchasePrice = 0.0;

    @Column(name = "total_cost", nullable = false)
    @Builder.Default
    private Double totalCost = 0.0;

    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
