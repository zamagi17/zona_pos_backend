package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletDto {
    private Long id;
    private Long tenantId;
    @NotBlank(message = "Nama outlet tidak boleh kosong")
    private String name;
    @NotBlank(message = "Alamat tidak boleh kosong")
    private String address;
    private String managerName;
    private String managerPhone;
    private String managerEmail;
}
