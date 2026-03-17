package com.busgallery.busgallery.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VehicleComment {
    private Long id;
    private Long vehicleId;
    private Long userId;
    private String username;
    private String displayName;
    private String content;
    private LocalDateTime createdAt;
}
