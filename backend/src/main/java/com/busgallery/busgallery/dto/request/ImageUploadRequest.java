package com.busgallery.busgallery.dto.request;

import lombok.Data;

@Data
public class ImageUploadRequest {

    private String title;
    private String description;
    private String uploadUser;
}