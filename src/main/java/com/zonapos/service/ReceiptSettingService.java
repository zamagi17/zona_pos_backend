package com.zonapos.service;

import com.zonapos.dto.ReceiptSettingDto;
import com.zonapos.entity.Outlet;
import com.zonapos.entity.ReceiptSetting;
import com.zonapos.entity.User;
import com.zonapos.repository.OutletRepository;
import com.zonapos.repository.ReceiptSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReceiptSettingService {

    private final ReceiptSettingRepository receiptSettingRepository;
    private final OutletRepository outletRepository;

    public ReceiptSettingDto getReceiptSetting(Long tenantId, Long outletId) {
        Optional<ReceiptSetting> settingOpt = Optional.empty();

        if (outletId != null) {
            settingOpt = receiptSettingRepository.findByTenantIdAndOutletId(tenantId, outletId);
        }

        if (settingOpt.isEmpty()) {
            settingOpt = receiptSettingRepository.findByTenantIdAndOutletIdIsNull(tenantId);
        }

        if (settingOpt.isPresent()) {
            return mapToDto(settingOpt.get());
        }

        // Generate intelligent default setting if none exists yet
        String defaultName = "ZONA POS UMKM";
        String defaultAddress = "Outlet Cabang";
        String defaultPhone = "";

        if (outletId != null) {
            Optional<Outlet> outletOpt = outletRepository.findById(outletId);
            if (outletOpt.isPresent()) {
                Outlet o = outletOpt.get();
                defaultName = o.getName();
                defaultAddress = o.getAddress() != null ? o.getAddress() : defaultAddress;
                defaultPhone = o.getManagerPhone() != null ? o.getManagerPhone() : "";
            }
        }

        return ReceiptSettingDto.builder()
                .tenantId(tenantId)
                .outletId(outletId)
                .businessName(defaultName)
                .logoUrl(null)
                .address(defaultAddress)
                .phone(defaultPhone)
                .instagram("@zonapos.id")
                .website(null)
                .footerNote("Barang yang sudah dibeli tidak dapat ditukar atau dikembalikan.\nTerima kasih atas kunjungan Anda!")
                .paperSize("58mm")
                .showLogo(true)
                .showSocialMedia(true)
                .build();
    }

    @Transactional
    public ReceiptSettingDto saveReceiptSetting(ReceiptSettingDto dto, User currentUser) {
        Long tenantId = currentUser.getTenantId();
        Long targetOutletId = dto.getOutletId();

        Optional<ReceiptSetting> existingOpt;
        if (targetOutletId != null) {
            existingOpt = receiptSettingRepository.findByTenantIdAndOutletId(tenantId, targetOutletId);
        } else {
            existingOpt = receiptSettingRepository.findByTenantIdAndOutletIdIsNull(tenantId);
        }

        ReceiptSetting setting = existingOpt.orElseGet(() -> ReceiptSetting.builder()
                .tenantId(tenantId)
                .outletId(targetOutletId)
                .createdBy(currentUser.getName())
                .build());

        setting.setBusinessName(dto.getBusinessName());
        setting.setLogoUrl(dto.getLogoUrl());
        setting.setAddress(dto.getAddress());
        setting.setPhone(dto.getPhone());
        setting.setInstagram(dto.getInstagram());
        setting.setWebsite(dto.getWebsite());
        setting.setFooterNote(dto.getFooterNote());
        setting.setPaperSize(dto.getPaperSize() != null && "80mm".equalsIgnoreCase(dto.getPaperSize()) ? "80mm" : "58mm");
        setting.setShowLogo(dto.getShowLogo() != null ? dto.getShowLogo() : true);
        setting.setShowSocialMedia(dto.getShowSocialMedia() != null ? dto.getShowSocialMedia() : true);
        setting.setUpdatedBy(currentUser.getName());

        return mapToDto(receiptSettingRepository.save(setting));
    }

    private ReceiptSettingDto mapToDto(ReceiptSetting s) {
        return ReceiptSettingDto.builder()
                .id(s.getId())
                .tenantId(s.getTenantId())
                .outletId(s.getOutletId())
                .businessName(s.getBusinessName())
                .logoUrl(s.getLogoUrl())
                .address(s.getAddress())
                .phone(s.getPhone())
                .instagram(s.getInstagram())
                .website(s.getWebsite())
                .footerNote(s.getFooterNote())
                .paperSize(s.getPaperSize())
                .showLogo(s.getShowLogo())
                .showSocialMedia(s.getShowSocialMedia())
                .build();
    }
}
