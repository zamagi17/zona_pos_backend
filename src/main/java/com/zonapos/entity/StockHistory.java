package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "stock_in_out", nullable = false)
    private Long stockInOut;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false, length = 50)
    private String status; // IN, OUT

    @Column(nullable = false, length = 50)
    private String type; // SALE, PURCHASE, ADJUSTMENT, TRANSFER, REFUND

    @Column(length = 255)
    private String remarks;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
