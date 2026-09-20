package com.zonapos.controller;

import com.zonapos.dto.GrossProfitReportDto;
import com.zonapos.dto.SalesReportDto;
import com.zonapos.entity.User;
import com.zonapos.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<SalesReportDto> getSalesReport(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        Long targetOutlet = "ROLE_OUTLET_MANAGER".equals(currentUser.getRole().getName())
                ? currentUser.getOutletId() : outletId;

        return ResponseEntity.ok(reportService.getSalesReport(currentUser.getTenantId(), targetOutlet, start, end));
    }

    @GetMapping("/gross-profit")
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<GrossProfitReportDto> getGrossProfitReport(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);

        return ResponseEntity.ok(reportService.getGrossProfitReport(currentUser.getTenantId(), outletId, start, end));
    }
}
