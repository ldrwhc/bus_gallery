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
public class CommentCreatedEvent {

    private Long commentId;

    private Long vehicleId;

    private String plateNumber;

    private Long userId;

    private String username;

    private String displayName;

    private String content;

    private LocalDateTime createdAt;
}

