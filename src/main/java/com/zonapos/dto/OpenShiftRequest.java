package com.zonapos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenShiftRequest {
    @NotNull(message = "Outlet ID wajib diisi")
    private Long outletId;

    @NotNull(message = "Modal awal wajib diisi")
    private Double startCash;
}
