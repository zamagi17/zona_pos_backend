package com.zonapos.controller;

import com.zonapos.dto.StorageDto;
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
@RequestMapping("/api/v1/storages")
@RequiredArgsConstructor
public class StorageController {

    private final MasterCatalogService masterCatalogService;

    @GetMapping
    public ResponseEntity<List<StorageDto>> getStorages(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(masterCatalogService.getStorages(currentUser.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<StorageDto> createStorage(
            @Valid @RequestBody StorageDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(masterCatalogService.createStorage(dto, currentUser));
    }
}
