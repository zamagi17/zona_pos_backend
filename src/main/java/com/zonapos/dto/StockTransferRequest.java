package com.zonapos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockTransferRequest {
    @NotNull(message = "Product ID wajib diisi")
    private Long productId;
    private Long variantId;
    @NotNull(message = "Storage ID asal wajib diisi")
    private Long fromStorageId;
    @NotNull(message = "Outlet ID tujuan wajib diisi")
    private Long toOutletId;
    @NotNull(message = "Jumlah mutasi wajib diisi")
    @Min(value = 1, message = "Kuantitas transfer minimal 1")
    private Long quantity;
    private String remarks;
}
