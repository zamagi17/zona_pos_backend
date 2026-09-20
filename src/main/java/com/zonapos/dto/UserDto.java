package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private Long tenantId;
    private Long outletId;
    private String outletName;
    private String name;
    private String email;
    private String phone;
    private Boolean isActive;
    private String role;
    private Integer roleId;
    private LocalDateTime createdAt;
}
