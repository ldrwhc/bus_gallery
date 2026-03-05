package com.busgallery.busgallery.util;

import java.util.UUID;

public final class FileNameUtils {

    private FileNameUtils() {
    }

    public static String randomFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}