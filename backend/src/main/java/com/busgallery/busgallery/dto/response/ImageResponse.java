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
    private String vehiclePlateNumber;
    private String modelName;
    private Map<String, String> exif;

    public static ImageResponse fromEntity(Image image) {
        return fromEntity(image, null);
    }

    public static ImageResponse fromEntity(Image image, com.busgallery.busgallery.entity.Vehicle vehicle) {
        if (image == null) {
            return null;
        }
        String plate = null;
        String model = null;
        if (vehicle != null) {
            plate = vehicle.getPlateNumber();
            if (vehicle.getModel() != null) {
                model = vehicle.getModel().getName();
            }
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
                plate,
                model,
                ExifUtils.fromJson(image.getExifJson())
        );
    }
}
