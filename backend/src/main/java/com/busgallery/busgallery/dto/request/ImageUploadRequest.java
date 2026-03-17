package com.busgallery.busgallery.dto.request;

import lombok.Data;

/**
 * ImageUploadRequest类用于封装ImageUploadRequest相关的领域职责（所在包：com.busgallery.busgallery.dto.request）。
 */
@Data
public class ImageUploadRequest {

    private String title;
    private String description;
    private String uploadUser;
}