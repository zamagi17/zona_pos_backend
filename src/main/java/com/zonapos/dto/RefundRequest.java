package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefundRequest {
    @NotBlank(message = "Alasan refund wajib diisi")
    private String reason;
}
