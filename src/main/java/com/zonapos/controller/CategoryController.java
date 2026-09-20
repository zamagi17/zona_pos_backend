package com.zonapos.controller;

import com.zonapos.dto.CategoryDto;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final MasterCatalogService masterCatalogService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(masterCatalogService.getCategories(currentUser.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CategoryDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(masterCatalogService.createCategory(dto, currentUser));
    }
}
