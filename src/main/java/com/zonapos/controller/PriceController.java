package com.zonapos.controller;

import com.zonapos.dto.PriceDto;
import com.zonapos.entity.PriceHistory;
import com.zonapos.entity.User;
import com.zonapos.service.PriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<PriceDto> setOutletPrice(
            @Valid @RequestBody PriceDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(priceService.setOutletPrice(dto, currentUser));
    }

    @GetMapping("/history/{priceId}")
    public ResponseEntity<List<PriceHistory>> getPriceHistory(@PathVariable Long priceId) {
        return ResponseEntity.ok(priceService.getPriceHistory(priceId));
    }
}
