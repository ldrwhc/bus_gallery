package com.busgallery.busgallery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * PageResponse类用于封装PageResponse相关的领域职责（所在包：com.busgallery.busgallery.dto.response）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> records = Collections.emptyList();
    private long total;
    private int page;
    private int size;

    /**
     * of方法用于处理of相关的业务逻辑。
     * @param records records参数，详见调用方上下文。
     * @param total total参数，详见调用方上下文。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回PageResponse<T>类型结果。
     */
    public static <T> PageResponse<T> of(List<T> records, long total, int page, int size) {
        return new PageResponse<>(records, total, page, size);
    }
}