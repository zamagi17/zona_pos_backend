package com.zonapos.service;

import com.zonapos.dto.ProductDto;
import com.zonapos.dto.ProductVariantDto;
import com.zonapos.entity.*;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final PriceRepository priceRepository;
    private final StockRepository stockRepository;

    public List<ProductDto> getProducts(Long tenantId, Long outletId) {
        List<Product> products = productRepository.findByTenantId(tenantId);
        return products.stream().map(p -> mapToDto(p, outletId)).collect(Collectors.toList());
    }

    public ProductDto getProductById(Long id, Long outletId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan dengan ID: " + id));
        return mapToDto(product, outletId);
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto, User currentUser) {
        if (productRepository.findByTenantIdAndSku(currentUser.getTenantId(), dto.getSku()).isPresent()) {
            throw new BadRequestException("SKU produk sudah digunakan");
        }

        Product product = Product.builder()
                .tenantId(currentUser.getTenantId())
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .name(dto.getName())
                .desc(dto.getDesc())
                .categoryId(dto.getCategoryId())
                .unitId(dto.getUnitId())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdBy(currentUser.getName())
                .build();
        product = productRepository.save(product);

        // If price details given and outletId is provided, save initial price
        if (currentUser.getOutletId() != null && dto.getSellingPrice() != null) {
            Price price = Price.builder()
                    .productId(product.getId())
                    .outletId(currentUser.getOutletId())
                    .sellingPrice(dto.getSellingPrice())
                    .purchasePrice(dto.getPurchasePrice() != null ? dto.getPurchasePrice() : 0.0)
                    .discountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0)
                    .discountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : 0.0)
                    .taxPercentage(dto.getTaxPercentage() != null ? dto.getTaxPercentage() : 0)
                    .taxAmount(dto.getTaxAmount() != null ? dto.getTaxAmount() : 0.0)
                    .createdBy(currentUser.getName())
                    .build();
            priceRepository.save(price);
        }

        return mapToDto(product, currentUser.getOutletId());
    }

    @Transactional
    public ProductVariantDto addVariant(Long productId, ProductVariantDto dto, User currentUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produk induk tidak ditemukan"));

        if (!product.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak");
        }

        if (productVariantRepository.findBySku(dto.getSku()).isPresent()) {
            throw new BadRequestException("SKU Varian sudah digunakan");
        }

        ProductVariant variant = ProductVariant.builder()
                .productId(productId)
                .sku(dto.getSku())
                .name(dto.getName())
                .createdBy(currentUser.getName())
                .build();
        variant = productVariantRepository.save(variant);
        dto.setId(variant.getId());
        dto.setProductId(productId);
        return dto;
    }

    private ProductDto mapToDto(Product p, Long outletId) {
        String categoryName = p.getCategoryId() != null ?
                categoryRepository.findById(p.getCategoryId()).map(Category::getName).orElse(null) : null;
        String unitName = p.getUnitId() != null ?
                unitRepository.findById(p.getUnitId()).map(Unit::getName).orElse(null) : null;

        Long priceId = null;
        Double sellingPrice = 0.0;
        Double purchasePrice = 0.0;
        Short discountPercentage = 0;
        Double discountAmount = 0.0;
        Short taxPercentage = 0;
        Double taxAmount = 0.0;

        Long stockQuantity = 0L;
        Long stockMinimum = 0L;

        if (outletId != null) {
            Optional<Price> priceOpt = priceRepository.findByProductIdAndOutletId(p.getId(), outletId);
            if (priceOpt.isPresent()) {
                Price pr = priceOpt.get();
                priceId = pr.getId();
                sellingPrice = pr.getSellingPrice();
                purchasePrice = pr.getPurchasePrice();
                discountPercentage = pr.getDiscountPercentage();
                discountAmount = pr.getDiscountAmount();
                taxPercentage = pr.getTaxPercentage();
                taxAmount = pr.getTaxAmount();
            }

            Optional<Stock> stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(p.getId(), outletId);
            if (stockOpt.isPresent()) {
                stockQuantity = stockOpt.get().getQuantity();
                stockMinimum = stockOpt.get().getMinimum();
            }
        }

        List<ProductVariantDto> variants = productVariantRepository.findByProductId(p.getId()).stream()
                .map(v -> {
                    Long varStock = 0L;
                    if (outletId != null) {
                        varStock = stockRepository.findByProductIdAndVariantIdAndOutletId(p.getId(), v.getId(), outletId)
                                .map(Stock::getQuantity).orElse(0L);
                    }
                    return ProductVariantDto.builder()
                            .id(v.getId())
                            .productId(v.getProductId())
                            .sku(v.getSku())
                            .name(v.getName())
                            .stockQuantity(varStock)
                            .build();
                }).collect(Collectors.toList());

        return ProductDto.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .sku(p.getSku())
                .barcode(p.getBarcode())
                .name(p.getName())
                .desc(p.getDesc())
                .categoryId(p.getCategoryId())
                .categoryName(categoryName)
                .unitId(p.getUnitId())
                .unitName(unitName)
                .isActive(p.getIsActive())
                .priceId(priceId)
                .sellingPrice(sellingPrice)
                .purchasePrice(purchasePrice)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .taxPercentage(taxPercentage)
                .taxAmount(taxAmount)
                .stockQuantity(stockQuantity)
                .stockMinimum(stockMinimum)
                .variants(variants)
                .build();
    }
}
