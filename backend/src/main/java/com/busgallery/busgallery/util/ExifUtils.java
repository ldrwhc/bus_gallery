package com.busgallery.busgallery.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

public final class ExifUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ExifUtils() {
    }

    public static String toJson(Map<String, String> exif) {
        if (exif == null || exif.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(exif);
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
