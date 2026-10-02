package com.zonapos.service;

import com.zonapos.dto.StockAdjustmentRequest;
import com.zonapos.dto.StockPurchaseRequest;
import com.zonapos.dto.StockResponse;
import com.zonapos.dto.StockTransferRequest;
import com.zonapos.entity.*;
import com.zonapos.exception.BadRequestException;
import com.zonapos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OutletRepository outletRepository;
    private final StorageRepository storageRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PriceRepository priceRepository;

    @Transactional
    public StockResponse adjustStock(StockAdjustmentRequest request, User currentUser) {
        Stock stock = findOrCreateStock(request.getProductId(), request.getVariantId(),
                request.getOutletId(), request.getStorageId(), currentUser);

        long oldQty = stock.getQuantity() != null ? stock.getQuantity() : 0L;
        long newQty = request.getQuantity();
        long diff = newQty - oldQty;

        stock.setQuantity(newQty);
        if (request.getMinimum() != null) {
            stock.setMinimum(request.getMinimum());
        }
        stock.setUpdatedBy(currentUser.getName());
        stock = stockRepository.save(stock);

        // Record stock history
        StockHistory history = StockHistory.builder()
                .stockId(stock.getId())
                .stockInOut(diff)
                .quantity(newQty)
                .status(diff >= 0 ? "IN" : "OUT")
                .type("ADJUSTMENT")
                .remarks(request.getRemarks() != null ? request.getRemarks() : "Penyesuaian stok manual")
                .userId(currentUser.getId())
                .build();
        stockHistoryRepository.save(history);

        return mapToStockResponse(stock);
    }

    @Transactional
    public StockResponse purchaseStock(StockPurchaseRequest request, User currentUser) {
        Stock stock = findOrCreateStock(request.getProductId(), request.getVariantId(),
                request.getOutletId(), request.getStorageId(), currentUser);

        long currentQty = stock.getQuantity() != null ? stock.getQuantity() : 0L;
        long newQty = currentQty + request.getQuantity();

        stock.setQuantity(newQty);
        stock.setUpdatedBy(currentUser.getName());
        stock = stockRepository.save(stock);

        // Lookup supplier name
        String supplierName = null;
        if (request.getSupplierId() != null) {
            supplierName = supplierRepository.findById(request.getSupplierId())
                    .map(Supplier::getName)
                    .orElse(null);
        }

        double unitPurchasePrice = request.getPurchasePrice() != null ? request.getPurchasePrice() : 0.0;
        double totalCost = unitPurchasePrice * request.getQuantity();

        // 1. Record Purchase Order
        String poNo = "PO-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%04d", (int)(System.currentTimeMillis() % 10000));
        PurchaseOrder po = PurchaseOrder.builder()
                .poNo(poNo)
                .tenantId(currentUser.getTenantId())
                .supplierId(request.getSupplierId())
                .storageId(request.getStorageId())
                .outletId(request.getOutletId())
                .productId(request.getProductId())
                .variantId(request.getVariantId())
                .quantity(request.getQuantity())
                .purchasePrice(unitPurchasePrice)
                .totalCost(totalCost)
                .invoiceNo(request.getInvoiceNo())
                .remarks(request.getRemarks())
                .userId(currentUser.getId())
                .build();
        purchaseOrderRepository.save(po);

        // 2. Update default purchase price in Price table if outlet is provided or for all prices of this product
        if (unitPurchasePrice > 0) {
            if (request.getOutletId() != null) {
                priceRepository.findByProductIdAndOutletId(request.getProductId(), request.getOutletId())
                        .ifPresent(p -> {
                            p.setPurchasePrice(unitPurchasePrice);
                            priceRepository.save(p);
                        });
            }
        }

        // 3. Format detailed remarks for stock history audit
        StringBuilder remarksSb = new StringBuilder();
        if (supplierName != null) {
            remarksSb.append("Supplier: ").append(supplierName);
        } else {
            remarksSb.append("Barang masuk (Purchase)");
        }
        if (request.getInvoiceNo() != null && !request.getInvoiceNo().isBlank()) {
            remarksSb.append(" (Faktur: ").append(request.getInvoiceNo().trim()).append(")");
        }
        if (unitPurchasePrice > 0) {
            remarksSb.append(String.format(" @ Rp %,.0f", unitPurchasePrice));
        }
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            remarksSb.append(" - ").append(request.getRemarks().trim());
        }

        StockHistory history = StockHistory.builder()
                .stockId(stock.getId())
                .stockInOut(request.getQuantity())
                .quantity(newQty)
                .status("IN")
                .type("PURCHASE")
                .remarks(remarksSb.toString())
                .userId(currentUser.getId())
                .build();
        stockHistoryRepository.save(history);

        return mapToStockResponse(stock);
    }

    @Transactional
    public void transferStock(StockTransferRequest request, User currentUser) {
        // 1. Source: Storage
        Stock sourceStock = findOrCreateStock(request.getProductId(), request.getVariantId(),
                null, request.getFromStorageId(), currentUser);

        if (sourceStock.getQuantity() < request.getQuantity()) {
            throw new BadRequestException("Stok di gudang tidak mencukupi untuk transfer. Tersedia: " + sourceStock.getQuantity());
        }

        // 2. Destination: Outlet
        Stock destStock = findOrCreateStock(request.getProductId(), request.getVariantId(),
                request.getToOutletId(), null, currentUser);

        // Deduct source
        long newSourceQty = sourceStock.getQuantity() - request.getQuantity();
        sourceStock.setQuantity(newSourceQty);
        sourceStock.setUpdatedBy(currentUser.getName());
        sourceStock = stockRepository.save(sourceStock);

        StockHistory sourceHistory = StockHistory.builder()
                .stockId(sourceStock.getId())
                .stockInOut(-request.getQuantity())
                .quantity(newSourceQty)
                .status("OUT")
                .type("TRANSFER")
                .remarks(request.getRemarks() != null ? request.getRemarks() : "Transfer ke Outlet ID " + request.getToOutletId())
                .userId(currentUser.getId())
                .build();
        stockHistoryRepository.save(sourceHistory);

        // Add destination
        long newDestQty = destStock.getQuantity() + request.getQuantity();
        destStock.setQuantity(newDestQty);
        destStock.setUpdatedBy(currentUser.getName());
        destStock = stockRepository.save(destStock);

        StockHistory destHistory = StockHistory.builder()
                .stockId(destStock.getId())
                .stockInOut(request.getQuantity())
                .quantity(newDestQty)
                .status("IN")
                .type("TRANSFER")
                .remarks(request.getRemarks() != null ? request.getRemarks() : "Terima dari Gudang ID " + request.getFromStorageId())
                .userId(currentUser.getId())
                .build();
        stockHistoryRepository.save(destHistory);
    }

    public List<StockResponse> getStocksByOutlet(Long outletId) {
        return stockRepository.findByOutletId(outletId).stream()
                .map(this::mapToStockResponse).collect(Collectors.toList());
    }

    public List<StockResponse> getStocksByStorage(Long storageId) {
        return stockRepository.findByStorageId(storageId).stream()
                .map(this::mapToStockResponse).collect(Collectors.toList());
    }

    public List<StockResponse> getLowStockAlert(Long outletId) {
        return stockRepository.findLowStockByOutletId(outletId).stream()
                .map(this::mapToStockResponse).collect(Collectors.toList());
    }

    public List<StockHistory> getStockHistory(Long stockId) {
        return stockHistoryRepository.findByStockIdOrderByCreatedAtDesc(stockId);
    }

    private Stock findOrCreateStock(Long productId, Long variantId, Long outletId, Long storageId, User currentUser) {
        Optional<Stock> stockOpt;
        if (outletId != null) {
            if (variantId != null) {
                stockOpt = stockRepository.findByProductIdAndVariantIdAndOutletId(productId, variantId, outletId);
            } else {
                stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(productId, outletId);
            }
        } else {
            if (variantId != null) {
                stockOpt = stockRepository.findByProductIdAndVariantIdAndStorageId(productId, variantId, storageId);
            } else {
                stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndStorageId(productId, storageId);
            }
        }

        return stockOpt.orElseGet(() -> {
            Stock newStock = Stock.builder()
                    .productId(productId)
                    .variantId(variantId)
                    .outletId(outletId)
                    .storageId(storageId)
                    .quantity(0L)
                    .minimum(5L)
                    .createdBy(currentUser.getName())
                    .build();
            return stockRepository.save(newStock);
        });
    }

    private StockResponse mapToStockResponse(Stock s) {
        String productName = productRepository.findById(s.getProductId()).map(Product::getName).orElse(null);
        String productSku = productRepository.findById(s.getProductId()).map(Product::getSku).orElse(null);
        String variantName = s.getVariantId() != null ?
                productVariantRepository.findById(s.getVariantId()).map(ProductVariant::getName).orElse(null) : null;
        String outletName = s.getOutletId() != null ?
                outletRepository.findById(s.getOutletId()).map(Outlet::getName).orElse(null) : null;
        String storageName = s.getStorageId() != null ?
                storageRepository.findById(s.getStorageId()).map(Storage::getName).orElse(null) : null;

        boolean isLow = s.getQuantity() != null && s.getMinimum() != null && s.getQuantity() <= s.getMinimum();

        return StockResponse.builder()
                .id(s.getId())
                .productId(s.getProductId())
                .productName(productName)
                .productSku(productSku)
                .variantId(s.getVariantId())
                .variantName(variantName)
                .outletId(s.getOutletId())
                .outletName(outletName)
                .storageId(s.getStorageId())
                .storageName(storageName)
                .quantity(s.getQuantity())
                .minimum(s.getMinimum())
                .isLowStock(isLow)
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
