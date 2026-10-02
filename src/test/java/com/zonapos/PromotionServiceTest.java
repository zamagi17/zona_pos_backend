package com.zonapos;

import com.zonapos.dto.PromotionValidationResponse;
import com.zonapos.entity.Promotion;
import com.zonapos.repository.PromotionRepository;
import com.zonapos.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private PromotionService promotionService;

    private Long tenantId = 1L;
    private Long outletId = 1L;

    @Test
    void testValidateVoucher_PercentSuccess() {
        Promotion promo = Promotion.builder()
                .id(1L)
                .tenantId(tenantId)
                .code("HEMAT10")
                .name("Promo Hemat 10%")
                .discountType("PERCENT")
                .discountValue(10.0)
                .minOrderAmount(50000.0)
                .maxDiscountAmount(50000.0)
                .isActive(true)
                .build();

        when(promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "HEMAT10"))
                .thenReturn(Optional.of(promo));

        PromotionValidationResponse res = promotionService.validateVoucher(tenantId, outletId, "HEMAT10", 120000.0);

        assertTrue(res.isValid());
        assertEquals("HEMAT10", res.getCode());
        assertEquals(12000.0, res.getDiscountAmount()); // 10% of 120,000 = 12,000
    }

    @Test
    void testValidateVoucher_FixedSuccess() {
        Promotion promo = Promotion.builder()
                .id(2L)
                .tenantId(tenantId)
                .code("GRANDOPENING")
                .name("Promo Grand Opening Rp 15.000")
                .discountType("FIXED")
                .discountValue(15000.0)
                .minOrderAmount(100000.0)
                .isActive(true)
                .build();

        when(promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "GRANDOPENING"))
                .thenReturn(Optional.of(promo));

        PromotionValidationResponse res = promotionService.validateVoucher(tenantId, outletId, "GRANDOPENING", 150000.0);

        assertTrue(res.isValid());
        assertEquals("GRANDOPENING", res.getCode());
        assertEquals(15000.0, res.getDiscountAmount());
    }

    @Test
    void testValidateVoucher_MinOrderAmountFailed() {
        Promotion promo = Promotion.builder()
                .id(2L)
                .tenantId(tenantId)
                .code("GRANDOPENING")
                .name("Promo Grand Opening Rp 15.000")
                .discountType("FIXED")
                .discountValue(15000.0)
                .minOrderAmount(100000.0)
                .isActive(true)
                .build();

        when(promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "GRANDOPENING"))
                .thenReturn(Optional.of(promo));

        PromotionValidationResponse res = promotionService.validateVoucher(tenantId, outletId, "GRANDOPENING", 60000.0);

        assertFalse(res.isValid());
        assertTrue(res.getMessage().contains("Minimal belanja"));
    }

    @Test
    void testValidateVoucher_MaxDiscountCapping() {
        Promotion promo = Promotion.builder()
                .id(1L)
                .tenantId(tenantId)
                .code("DISKON50")
                .name("Diskon 50% Max 20rb")
                .discountType("PERCENT")
                .discountValue(50.0)
                .minOrderAmount(0.0)
                .maxDiscountAmount(20000.0)
                .isActive(true)
                .build();

        when(promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "DISKON50"))
                .thenReturn(Optional.of(promo));

        PromotionValidationResponse res = promotionService.validateVoucher(tenantId, outletId, "DISKON50", 100000.0);

        assertTrue(res.isValid());
        assertEquals(20000.0, res.getDiscountAmount()); // 50% of 100k is 50k, but capped at 20k
    }

    @Test
    void testValidateVoucher_ExpiredDate() {
        Promotion promo = Promotion.builder()
                .id(3L)
                .tenantId(tenantId)
                .code("EXPIREDPROMO")
                .name("Promo Kedaluwarsa")
                .discountType("PERCENT")
                .discountValue(10.0)
                .minOrderAmount(0.0)
                .endDate(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        when(promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "EXPIREDPROMO"))
                .thenReturn(Optional.of(promo));

        PromotionValidationResponse res = promotionService.validateVoucher(tenantId, outletId, "EXPIREDPROMO", 100000.0);

        assertFalse(res.isValid());
        assertTrue(res.getMessage().contains("kedaluwarsa"));
    }

    @Test
    void testValidateVoucher_UsageLimitReached() {
        Promotion promo = Promotion.builder()
                .id(4L)
                .tenantId(tenantId)
                .code("LIMIT10")
                .name("Promo Terbatas")
                .discountType("FIXED")
                .discountValue(10000.0)
                .usageLimit(5)
                .timesUsed(5)
                .isActive(true)
                .build();

        when(promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "LIMIT10"))
                .thenReturn(Optional.of(promo));

        PromotionValidationResponse res = promotionService.validateVoucher(tenantId, outletId, "LIMIT10", 100000.0);

        assertFalse(res.isValid());
        assertTrue(res.getMessage().contains("Kuota pemakaian"));
    }

    @Test
    void testCreatePromotion_Success() {
        com.zonapos.entity.User user = com.zonapos.entity.User.builder()
                .id(1L)
                .tenantId(tenantId)
                .name("Owner Toko")
                .build();

        com.zonapos.dto.PromotionDto inputDto = com.zonapos.dto.PromotionDto.builder()
                .code("PROMOJUMAT")
                .name("Promo Berkah Jumat")
                .discountType("PERCENT")
                .discountValue(15.0)
                .minOrderAmount(50000.0)
                .maxDiscountAmount(25000.0)
                .usageLimit(100)
                .isActive(true)
                .build();

        when(promotionRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, "PROMOJUMAT"))
                .thenReturn(false);

        when(promotionRepository.save(org.mockito.ArgumentMatchers.any(Promotion.class)))
                .thenAnswer(inv -> {
                    Promotion saved = inv.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });

        com.zonapos.dto.PromotionDto result = promotionService.createPromotion(inputDto, user);

        assertNotNull(result);
        assertEquals("PROMOJUMAT", result.getCode());
        assertEquals("Promo Berkah Jumat", result.getName());
        assertEquals(15.0, result.getDiscountValue());
        assertEquals(0, result.getTimesUsed());
        assertTrue(result.getIsActive());
    }

    @Test
    void testTogglePromotionStatus() {
        com.zonapos.entity.User user = com.zonapos.entity.User.builder()
                .id(1L)
                .tenantId(tenantId)
                .name("Owner Toko")
                .build();

        Promotion promo = Promotion.builder()
                .id(5L)
                .tenantId(tenantId)
                .code("HEMAT10")
                .name("Promo Hemat")
                .isActive(true)
                .build();

        when(promotionRepository.findById(5L)).thenReturn(Optional.of(promo));
        when(promotionRepository.save(org.mockito.ArgumentMatchers.any(Promotion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        com.zonapos.dto.PromotionDto toggled = promotionService.togglePromotionStatus(5L, user);

        assertFalse(toggled.getIsActive());
    }
}
