package com.zonapos.controller;

import com.zonapos.dto.CreateUserRequest;
import com.zonapos.dto.ResetPasswordRequest;
import com.zonapos.dto.UpdateUserRequest;
import com.zonapos.dto.UserDto;
import com.zonapos.entity.User;
import com.zonapos.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<List<UserDto>> getUsers(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long outletId) {
        Long targetOutlet = "ROLE_OUTLET_MANAGER".equals(currentUser.getRole().getName())
                ? currentUser.getOutletId() : outletId;
        return ResponseEntity.ok(userService.getUsersByTenant(currentUser.getTenantId(), targetOutlet));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<UserDto> createUser(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateUserRequest request) {
        request.setTenantId(currentUser.getTenantId());
        return ResponseEntity.ok(userService.createUser(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request,
            @AuthenticationPrincipal User currentUser) {
        userService.resetPassword(id, request, currentUser);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<UserDto> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.toggleStatus(id, currentUser));
    }
}
