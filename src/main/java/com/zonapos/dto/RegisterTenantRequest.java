package com.zonapos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterTenantRequest {
    @NotBlank(message = "Nama tenant tidak boleh kosong")
    private String tenantName;

    @NotBlank(message = "Alamat tidak boleh kosong")
    private String address;

    @NotBlank(message = "Nama owner tidak boleh kosong")
    private String ownerName;

    @NotBlank(message = "Nomor telepon tidak boleh kosong")
    private String ownerPhone;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String ownerEmail;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;
}
