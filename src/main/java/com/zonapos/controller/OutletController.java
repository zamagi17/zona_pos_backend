package com.zonapos.controller;

import com.zonapos.dto.OutletDto;
import com.zonapos.entity.User;
import com.zonapos.service.OutletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/outlets")
@RequiredArgsConstructor
public class OutletController {

    private final OutletService outletService;

    @GetMapping
    public ResponseEntity<List<OutletDto>> getOutlets(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(outletService.getOutletsByTenant(currentUser.getTenantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutletDto> getOutletById(@PathVariable Long id) {
        return ResponseEntity.ok(outletService.getOutletById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<OutletDto> createOutlet(
            @Valid @RequestBody OutletDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(outletService.createOutlet(dto, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<OutletDto> updateOutlet(
            @PathVariable Long id,
            @Valid @RequestBody OutletDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(outletService.updateOutlet(id, dto, currentUser));
    }
}
