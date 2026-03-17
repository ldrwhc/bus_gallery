package com.busgallery.busgallery.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * ExifUtils类用于封装ExifUtils相关的领域职责（所在包：com.busgallery.busgallery.util）。
 */
public final class ExifUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * ExifUtils构造器用于初始化对象状态。
     * @return 构造器无返回值。
     */
    private ExifUtils() {
    }

    /**
     * toJson方法用于处理toJson相关的业务逻辑。
     * @param exif exif参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
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

    /**
     * fromJson方法用于处理fromJson相关的业务逻辑。
     * @param json json参数，详见调用方上下文。
     * @return 返回String>类型结果。
     */
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
