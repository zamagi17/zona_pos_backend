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
public class StorageDto {
    private Long id;
    private Long tenantId;
    @NotBlank(message = "Kode gudang tidak boleh kosong")
    private String code;
    @NotBlank(message = "Nama gudang tidak boleh kosong")
    private String name;
}
