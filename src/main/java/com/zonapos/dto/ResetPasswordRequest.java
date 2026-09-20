package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Password baru tidak boleh kosong")
    private String newPassword;

    @NotBlank(message = "Konfirmasi password tidak boleh kosong")
    private String confirmPassword;
}
