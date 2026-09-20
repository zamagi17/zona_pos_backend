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
public class CustomerDto {
    private Long id;
    private Long tenantId;
    @NotBlank(message = "Nama pelanggan tidak boleh kosong")
    private String name;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
}
