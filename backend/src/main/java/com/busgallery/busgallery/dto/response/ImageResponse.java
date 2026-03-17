package com.busgallery.busgallery.dto.response;

import com.busgallery.busgallery.entity.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.busgallery.busgallery.util.ExifUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ImageResponse类用于封装ImageResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
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
    private Long uploaderId;
    private String uploaderUsername;
    private String uploaderDisplayName;
    private LocalDateTime createTime;
    private Long vehicleId;
    private Map<String, String> exif;

    /**
     * fromEntity方法用于处理fromEntity相关的业务逻辑。
     * @param image image参数，详见调用方上下文。
     * @return 返回ImageResponse类型结果。
     */
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
                image.getUploaderId(),
                image.getUploaderUsername(),
                image.getUploaderDisplayName(),
                image.getCreateTime(),
                image.getVehicleId(),
                ExifUtils.fromJson(image.getExifJson())
        );
    }
}
