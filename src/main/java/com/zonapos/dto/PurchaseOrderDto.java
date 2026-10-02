package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDto {
    private Long id;
    private String poNo;
    private Long tenantId;
    private Long supplierId;
    private String supplierName;
    private Long storageId;
    private String storageName;
    private Long outletId;
    private String outletName;
    private Long productId;
    private String productName;
    private Long variantId;
    private String variantName;
    private Long quantity;
    private Double purchasePrice;
    private Double totalCost;
    private String invoiceNo;
    private String remarks;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}
