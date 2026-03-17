package com.busgallery.busgallery.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VehicleFavorite {
    private Long id;
    private Long vehicleId;
    private Long userId;
    private LocalDateTime createdAt;
}
