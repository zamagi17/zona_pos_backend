package com.zonapos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloseShiftRequest {
    @NotNull(message = "Uang aktual di laci kasir wajib diisi")
    private Double actualCash;
}
