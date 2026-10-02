package com.zonapos.service;

import com.zonapos.dto.PromotionDto;
import com.zonapos.dto.PromotionValidationResponse;
import com.zonapos.entity.Promotion;
import com.zonapos.entity.User;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionValidationResponse validateVoucher(Long tenantId, Long outletId, String code, Double cartSubtotal) {
        if (code == null || code.isBlank()) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .message("Kode voucher tidak boleh kosong.")
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();
        Promotion p = promotionRepository.findByTenantIdAndCodeIgnoreCase(tenantId, cleanCode)
                .orElse(null);

        if (p == null) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Kode voucher '" + cleanCode + "' tidak ditemukan.")
                    .build();
        }

        if (Boolean.FALSE.equals(p.getIsActive())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(p.getCode())
                    .message("Voucher promo '" + p.getCode() + "' sudah tidak aktif.")
                    .build();
        }

        if (p.getOutletId() != null && outletId != null && !p.getOutletId().equals(outletId)) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(p.getCode())
                    .message("Voucher promo ini hanya berlaku untuk cabang outlet tertentu.")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (p.getStartDate() != null && now.isBefore(p.getStartDate())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(p.getCode())
                    .message("Voucher promo ini belum memasuki periode berlaku.")
                    .build();
        }

        if (p.getEndDate() != null && now.isAfter(p.getEndDate())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(p.getCode())
                    .message("Voucher promo ini sudah kedaluwarsa.")
                    .build();
        }

        if (p.getUsageLimit() != null && p.getTimesUsed() >= p.getUsageLimit()) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(p.getCode())
                    .message("Kuota pemakaian voucher promo '" + p.getCode() + "' sudah habis.")
                    .build();
        }

        double subtotal = cartSubtotal != null ? cartSubtotal : 0.0;
        double minRequired = p.getMinOrderAmount() != null ? p.getMinOrderAmount() : 0.0;
        if (subtotal < minRequired) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(p.getCode())
                    .minOrderAmount(minRequired)
                    .message(String.format("Minimal belanja untuk voucher ini adalah Rp %,.0f (Subtotal saat ini: Rp %,.0f)", minRequired, subtotal))
                    .build();
        }

        double discountAmount = 0.0;
        if ("PERCENT".equalsIgnoreCase(p.getDiscountType())) {
            discountAmount = (subtotal * (p.getDiscountValue() != null ? p.getDiscountValue() : 0.0)) / 100.0;
            if (p.getMaxDiscountAmount() != null && p.getMaxDiscountAmount() > 0 && discountAmount > p.getMaxDiscountAmount()) {
                discountAmount = p.getMaxDiscountAmount();
            }
            discountAmount = Math.round(discountAmount);
        } else {
            discountAmount = Math.min(subtotal, p.getDiscountValue() != null ? p.getDiscountValue() : 0.0);
        }

        return PromotionValidationResponse.builder()
                .valid(true)
                .code(p.getCode())
                .name(p.getName())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .discountAmount(discountAmount)
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .message(String.format("Voucher '%s' aktif! Hemat Rp %,.0f", p.getCode(), discountAmount))
                .build();
    }

    public List<PromotionDto> getActivePromotions(Long tenantId, Long outletId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .filter(p -> (p.getOutletId() == null || outletId == null || p.getOutletId().equals(outletId)))
                .filter(p -> (p.getStartDate() == null || !now.isBefore(p.getStartDate())))
                .filter(p -> (p.getEndDate() == null || !now.isAfter(p.getEndDate())))
                .filter(p -> (p.getUsageLimit() == null || p.getTimesUsed() < p.getUsageLimit()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PromotionDto> getAllPromotions(Long tenantId) {
        return promotionRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromotionDto createPromotion(PromotionDto dto, User currentUser) {
        if (dto.getCode() == null || dto.getCode().trim().isBlank()) {
            throw new BadRequestException("Kode voucher tidak boleh kosong.");
        }
        if (dto.getName() == null || dto.getName().trim().isBlank()) {
            throw new BadRequestException("Nama promosi tidak boleh kosong.");
        }
        if (dto.getDiscountValue() == null || dto.getDiscountValue() <= 0) {
            throw new BadRequestException("Nilai diskon harus lebih besar dari 0.");
        }
        String type = dto.getDiscountType() != null ? dto.getDiscountType().toUpperCase() : "PERCENT";
        if ("PERCENT".equalsIgnoreCase(type) && dto.getDiscountValue() > 100.0) {
            throw new BadRequestException("Nilai diskon persen tidak boleh melebihi 100%.");
        }
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("Tanggal berakhir tidak boleh mendahului tanggal mulai.");
        }

        String cleanCode = dto.getCode().trim().toUpperCase();
        if (promotionRepository.existsByTenantIdAndCodeIgnoreCase(currentUser.getTenantId(), cleanCode)) {
            throw new BadRequestException("Kode voucher '" + cleanCode + "' sudah terdaftar untuk bisnis ini.");
        }

        Promotion p = Promotion.builder()
                .tenantId(currentUser.getTenantId())
                .outletId(dto.getOutletId())
                .code(cleanCode)
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .discountType(type)
                .discountValue(dto.getDiscountValue())
                .minOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : 0.0)
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .timesUsed(0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdBy(currentUser.getName())
                .build();

        return mapToDto(promotionRepository.save(p));
    }

    @Transactional
    public PromotionDto updatePromotion(Long id, PromotionDto dto, User currentUser) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher promo tidak ditemukan"));

        if (!p.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak untuk promo ini.");
        }

        if (dto.getCode() == null || dto.getCode().trim().isBlank()) {
            throw new BadRequestException("Kode voucher tidak boleh kosong.");
        }
        if (dto.getName() == null || dto.getName().trim().isBlank()) {
            throw new BadRequestException("Nama promosi tidak boleh kosong.");
        }
        if (dto.getDiscountValue() == null || dto.getDiscountValue() <= 0) {
            throw new BadRequestException("Nilai diskon harus lebih besar dari 0.");
        }
        String type = dto.getDiscountType() != null ? dto.getDiscountType().toUpperCase() : "PERCENT";
        if ("PERCENT".equalsIgnoreCase(type) && dto.getDiscountValue() > 100.0) {
            throw new BadRequestException("Nilai diskon persen tidak boleh melebihi 100%.");
        }
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("Tanggal berakhir tidak boleh mendahului tanggal mulai.");
        }

        String cleanCode = dto.getCode().trim().toUpperCase();
        if (!p.getCode().equalsIgnoreCase(cleanCode) &&
                promotionRepository.existsByTenantIdAndCodeIgnoreCase(currentUser.getTenantId(), cleanCode)) {
            throw new BadRequestException("Kode voucher '" + cleanCode + "' sudah digunakan oleh promo lain.");
        }

        p.setCode(cleanCode);
        p.setName(dto.getName().trim());
        p.setDescription(dto.getDescription());
        p.setOutletId(dto.getOutletId());
        p.setDiscountType(type);
        p.setDiscountValue(dto.getDiscountValue());
        p.setMinOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : 0.0);
        p.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        p.setUsageLimit(dto.getUsageLimit());
        if (dto.getIsActive() != null) {
            p.setIsActive(dto.getIsActive());
        }
        p.setUpdatedBy(currentUser.getName());

        return mapToDto(promotionRepository.save(p));
    }

    @Transactional
    public void deletePromotion(Long id, User currentUser) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher promo tidak ditemukan"));

        if (!p.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak untuk promo ini.");
        }

        promotionRepository.delete(p);
    }

    @Transactional
    public PromotionDto togglePromotionStatus(Long id, User currentUser) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher promo tidak ditemukan"));

        if (!p.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Akses ditolak untuk promo ini.");
        }

        p.setIsActive(!Boolean.TRUE.equals(p.getIsActive()));
        p.setUpdatedBy(currentUser.getName());
        return mapToDto(promotionRepository.save(p));
    }

    private PromotionDto mapToDto(Promotion p) {
        return PromotionDto.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .outletId(p.getOutletId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .usageLimit(p.getUsageLimit())
                .timesUsed(p.getTimesUsed())
                .isActive(p.getIsActive())
                .build();
    }
}
