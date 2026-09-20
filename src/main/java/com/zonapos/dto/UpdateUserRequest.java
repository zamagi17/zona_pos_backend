package com.zonapos.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String phone;
    private Boolean isActive;
    private Integer roleId;
    private Long outletId;
}
