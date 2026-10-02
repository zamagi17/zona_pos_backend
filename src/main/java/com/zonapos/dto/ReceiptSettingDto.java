package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptSettingDto {
    private Long id;
    private Long tenantId;
    private Long outletId;
    private String businessName;
    private String logoUrl;
    private String address;
    private String phone;
    private String instagram;
    private String website;
    private String footerNote;
    private String paperSize; // "58mm", "80mm"
    private Boolean showLogo;
    private Boolean showSocialMedia;
}
