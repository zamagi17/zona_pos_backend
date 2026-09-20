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
public class CategoryDto {
    private Long id;
    private Long tenantId;
    @NotBlank(message = "Kode kategori tidak boleh kosong")
    private String code;
    @NotBlank(message = "Nama kategori tidak boleh kosong")
    private String name;
    private Long parentId;
    private Boolean isParent;
    private String desc;
}
