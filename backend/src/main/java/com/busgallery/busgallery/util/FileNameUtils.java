package com.busgallery.busgallery.util;

import java.util.UUID;

/**
 * FileNameUtils类用于封装FileNameUtils相关的领域职责（所在包：com.busgallery.busgallery.util）。
 */
public final class FileNameUtils {

    /**
     * FileNameUtils构造器用于初始化对象状态。
     * @return 构造器无返回值。
     */
    private FileNameUtils() {
    }

    /**
     * randomFileName方法用于处理randomFileName相关的业务逻辑。
     * @param originalFilename originalFilename参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    public static String randomFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}