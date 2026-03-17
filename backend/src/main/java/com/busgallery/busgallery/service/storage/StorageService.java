package com.busgallery.busgallery.service.storage;

import java.io.InputStream;

/**
 * StorageService接口用于封装StorageService相关的领域职责（所在包：com.busgallery.busgallery.service.storage）。
 */
public interface StorageService {

    /**
     * upload方法用于处理upload相关的业务逻辑。
     * @param objectName objectName参数，详见调用方上下文。
     * @param inputStream inputStream参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @param contentType contentType参数，详见调用方上下文。
     * @return 返回StorageObject类型结果。
     */
    StorageObject upload(String objectName, InputStream inputStream, long size, String contentType);

    /**
     * remove方法用于处理remove相关的业务逻辑。
     * @param objectUrl objectUrl参数，详见调用方上下文。
     * @return 无返回值。
     */
    void remove(String objectUrl);

    /**
     * buildUrl方法用于处理buildUrl相关的业务逻辑。
     * @param objectName objectName参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    String buildUrl(String objectName);
}