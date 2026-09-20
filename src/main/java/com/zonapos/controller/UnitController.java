package com.zonapos.controller;

import com.zonapos.dto.UnitDto;
import com.zonapos.entity.User;
import com.zonapos.service.MasterCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class UnitController {

    private final MasterCatalogService masterCatalogService;

    @GetMapping
    public ResponseEntity<List<UnitDto>> getUnits(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(masterCatalogService.getUnits(currentUser.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<UnitDto> createUnit(
            @Valid @RequestBody UnitDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(masterCatalogService.createUnit(dto, currentUser));
    }
}
