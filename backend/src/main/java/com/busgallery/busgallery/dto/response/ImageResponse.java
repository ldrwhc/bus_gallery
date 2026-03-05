package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {

    private Long id;
    private String url;
    private String thumbnailUrl;
    private String title;
    private String description;
    private String uploadUser;
    private LocalDateTime createTime;

    public static ImageResponse fromEntity(Image image) {
        if (image == null) {
            return null;
        }
        return new ImageResponse(
                image.getId(),
                image.getUrl(),
                image.getThumbnailUrl(),
                image.getTitle(),
                image.getDescription(),
                image.getUploadUser(),
                image.getCreateTime()
        );
    }
}