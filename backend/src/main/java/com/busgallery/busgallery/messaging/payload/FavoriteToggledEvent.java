package com.busgallery.busgallery.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteToggledEvent {

    private Long vehicleId;

    private String plateNumber;

    private Long userId;

    private boolean liked;

    private long totalFavorites;

    private LocalDateTime createdAt;
}

