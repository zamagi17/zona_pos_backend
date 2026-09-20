package com.zonapos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotNull(message = "Tenant ID wajib diisi")
    private Long tenantId;

    private Long outletId;

    @NotBlank(message = "Nama wajib diisi")
    private String name;

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Password wajib diisi")
    private String password;

    private String phone;

    @NotNull(message = "Role ID wajib diisi")
    private Integer roleId;
}
