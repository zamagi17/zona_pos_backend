package com.zonapos.service;

import com.zonapos.dto.PurchaseOrderDto;
import com.zonapos.dto.SupplierDto;
import com.zonapos.entity.*;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StorageRepository storageRepository;
    private final OutletRepository outletRepository;
    private final UserRepository userRepository;

    public List<SupplierDto> getSuppliers(Long tenantId, Boolean onlyActive) {
        List<Supplier> list;
        if (Boolean.TRUE.equals(onlyActive)) {
            list = supplierRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
        } else {
            list = supplierRepository.findByTenantIdOrderByIsActiveDescNameAsc(tenantId);
        }
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public SupplierDto getSupplierById(Long id, Long tenantId) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier tidak ditemukan dengan ID: " + id));
        return mapToDto(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(SupplierDto dto, User currentUser) {
        if (supplierRepository.existsByTenantIdAndNameIgnoreCase(currentUser.getTenantId(), dto.getName().trim())) {
            throw new BadRequestException("Supplier dengan nama '" + dto.getName() + "' sudah terdaftar");
        }

        Supplier supplier = Supplier.builder()
                .tenantId(currentUser.getTenantId())
                .name(dto.getName().trim())
                .contactPerson(dto.getContactPerson())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .paymentTerms(dto.getPaymentTerms() != null ? dto.getPaymentTerms() : "COD")
                .notes(dto.getNotes())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        supplier = supplierRepository.save(supplier);
        return mapToDto(supplier);
    }

    @Transactional
    public SupplierDto updateSupplier(Long id, SupplierDto dto, User currentUser) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, currentUser.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier tidak ditemukan dengan ID: " + id));

        if (!supplier.getName().equalsIgnoreCase(dto.getName().trim()) &&
                supplierRepository.existsByTenantIdAndNameIgnoreCase(currentUser.getTenantId(), dto.getName().trim())) {
            throw new BadRequestException("Supplier dengan nama '" + dto.getName() + "' sudah ada");
        }

        supplier.setName(dto.getName().trim());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        if (dto.getPaymentTerms() != null) supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setNotes(dto.getNotes());
        if (dto.getIsActive() != null) supplier.setIsActive(dto.getIsActive());

        supplier = supplierRepository.save(supplier);
        return mapToDto(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id, Long tenantId) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier tidak ditemukan dengan ID: " + id));
        supplierRepository.delete(supplier);
    }

    @Transactional
    public SupplierDto toggleStatus(Long id, Long tenantId) {
        Supplier supplier = supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier tidak ditemukan dengan ID: " + id));
        supplier.setIsActive(!Boolean.TRUE.equals(supplier.getIsActive()));
        supplier = supplierRepository.save(supplier);
        return mapToDto(supplier);
    }

    public List<PurchaseOrderDto> getPurchaseHistory(Long tenantId, Long productId, Long variantId) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findHistoryByProduct(tenantId, productId, variantId);
        return mapPurchaseOrdersToDto(pos);
    }

    public List<PurchaseOrderDto> getAllPurchaseOrders(
            Long tenantId,
            Long supplierId,
            Long storageId,
            Long outletId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findPurchaseOrdersFiltered(
                tenantId, supplierId, storageId, outletId, startDate, endDate
        );
        List<PurchaseOrderDto> dtos = mapPurchaseOrdersToDto(pos);

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            return dtos.stream().filter(po ->
                    (po.getPoNo() != null && po.getPoNo().toLowerCase().contains(q)) ||
                    (po.getInvoiceNo() != null && po.getInvoiceNo().toLowerCase().contains(q)) ||
                    (po.getProductName() != null && po.getProductName().toLowerCase().contains(q)) ||
                    (po.getSupplierName() != null && po.getSupplierName().toLowerCase().contains(q)) ||
                    (po.getRemarks() != null && po.getRemarks().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        return dtos;
    }

    private List<PurchaseOrderDto> mapPurchaseOrdersToDto(List<PurchaseOrder> pos) {
        if (pos == null || pos.isEmpty()) return List.of();

        // Batch lookups to prevent N+1 queries
        Map<Long, String> supplierMap = supplierRepository.findAllById(
                pos.stream().map(PurchaseOrder::getSupplierId).filter(java.util.Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName));

        Map<Long, String> productMap = productRepository.findAllById(
                pos.stream().map(PurchaseOrder::getProductId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Product::getId, Product::getName));

        Map<Long, String> variantMap = productVariantRepository.findAllById(
                pos.stream().map(PurchaseOrder::getVariantId).filter(java.util.Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ProductVariant::getId, ProductVariant::getName));

        Map<Long, String> storageMap = storageRepository.findAllById(
                pos.stream().map(PurchaseOrder::getStorageId).filter(java.util.Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Storage::getId, Storage::getName));

        Map<Long, String> outletMap = outletRepository.findAllById(
                pos.stream().map(PurchaseOrder::getOutletId).filter(java.util.Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Outlet::getId, Outlet::getName));

        Map<Long, String> userMap = userRepository.findAllById(
                pos.stream().map(PurchaseOrder::getUserId).filter(java.util.Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getName));

        return pos.stream().map(po -> PurchaseOrderDto.builder()
                .id(po.getId())
                .poNo(po.getPoNo())
                .tenantId(po.getTenantId())
                .supplierId(po.getSupplierId())
                .supplierName(po.getSupplierId() != null ? supplierMap.getOrDefault(po.getSupplierId(), "Supplier") : "-")
                .storageId(po.getStorageId())
                .storageName(po.getStorageId() != null ? storageMap.getOrDefault(po.getStorageId(), "-") : null)
                .outletId(po.getOutletId())
                .outletName(po.getOutletId() != null ? outletMap.getOrDefault(po.getOutletId(), "-") : null)
                .productId(po.getProductId())
                .productName(productMap.getOrDefault(po.getProductId(), "-"))
                .variantId(po.getVariantId())
                .variantName(po.getVariantId() != null ? variantMap.getOrDefault(po.getVariantId(), null) : null)
                .quantity(po.getQuantity())
                .purchasePrice(po.getPurchasePrice())
                .totalCost(po.getTotalCost())
                .invoiceNo(po.getInvoiceNo())
                .remarks(po.getRemarks())
                .userId(po.getUserId())
                .userName(po.getUserId() != null ? userMap.getOrDefault(po.getUserId(), "-") : "-")
                .createdAt(po.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    public Double getLastPurchasePrice(Long tenantId, Long productId, Long variantId) {
        return purchaseOrderRepository.findLastPurchase(tenantId, productId, variantId)
                .map(PurchaseOrder::getPurchasePrice)
                .orElse(null);
    }

    private SupplierDto mapToDto(Supplier s) {
        return SupplierDto.builder()
                .id(s.getId())
                .tenantId(s.getTenantId())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .paymentTerms(s.getPaymentTerms())
                .notes(s.getNotes())
                .isActive(s.getIsActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
