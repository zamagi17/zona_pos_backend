package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private Long id;
    private Long tenantId;

    @NotBlank(message = "Nama supplier / vendor wajib diisi")
    private String name;

    private String contactPerson;
    private String phone;
    private String email;
    private String address;

    @Builder.Default
    private String paymentTerms = "COD"; // COD, NET_7, NET_14, NET_30, TEMPO

    private String notes;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
