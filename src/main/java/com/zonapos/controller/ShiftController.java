package com.zonapos.controller;

import com.zonapos.dto.*;
import com.zonapos.entity.User;
import com.zonapos.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping("/open")
    public ResponseEntity<ShiftResponse> openShift(
            @Valid @RequestBody OpenShiftRequest request,
            @AuthenticationPrincipal User cashier) {
        return ResponseEntity.ok(shiftService.openShift(request, cashier));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ShiftResponse> closeShift(
            @PathVariable Long id,
            @Valid @RequestBody CloseShiftRequest request,
            @AuthenticationPrincipal User cashier) {
        return ResponseEntity.ok(shiftService.closeShift(id, request, cashier));
    }

    @GetMapping("/active")
    public ResponseEntity<ShiftResponse> getActiveShift(@AuthenticationPrincipal User cashier) {
        ShiftResponse shift = shiftService.getActiveShift(cashier.getId());
        return shift != null ? ResponseEntity.ok(shift) : ResponseEntity.noContent().build();
    }

    @GetMapping("/outlet/{outletId}")
    public ResponseEntity<List<ShiftResponse>> getShiftsByOutlet(@PathVariable Long outletId) {
        return ResponseEntity.ok(shiftService.getShiftsByOutlet(outletId));
    }

    @PostMapping("/movements")
    public ResponseEntity<CashMovementResponse> recordCashMovement(
            @Valid @RequestBody CreateCashMovementRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(shiftService.recordCashMovement(request, user));
    }

    @GetMapping("/{shiftId}/movements")
    public ResponseEntity<List<CashMovementResponse>> getCashMovements(
            @PathVariable Long shiftId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(shiftService.getCashMovements(shiftId, user));
    }

    @GetMapping("/active/movements")
    public ResponseEntity<List<CashMovementResponse>> getActiveShiftMovements(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(shiftService.getActiveShiftMovements(user));
    }
}
