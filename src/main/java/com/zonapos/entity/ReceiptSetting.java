package com.zonapos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "outlet_id")
    private Long outletId;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String instagram;

    @Column(length = 150)
    private String website;

    @Column(name = "footer_note", columnDefinition = "TEXT")
    private String footerNote;

    @Column(name = "paper_size", nullable = false, length = 10)
    @Builder.Default
    private String paperSize = "58mm"; // 58mm, 80mm

    @Column(name = "show_logo", nullable = false)
    @Builder.Default
    private Boolean showLogo = true;

    @Column(name = "show_social_media", nullable = false)
    @Builder.Default
    private Boolean showSocialMedia = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
