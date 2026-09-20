package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cashier_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_cash", nullable = false)
    @Builder.Default
    private Double startCash = 0.0;

    @Column(name = "expected_cash")
    @Builder.Default
    private Double expectedCash = 0.0;

    @Column(name = "actual_cash")
    @Builder.Default
    private Double actualCash = 0.0;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "OPEN"; // OPEN, CLOSED

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
