package com.busgallery.busgallery.util;

/**
 * PageUtils类用于封装PageUtils相关的领域职责（所在包：com.busgallery.busgallery.util）。
 */
public final class PageUtils {

    /**
     * PageUtils构造器用于初始化对象状态。
     * @return 构造器无返回值。
     */
    private PageUtils() {
    }

    /**
     * normalizePage方法用于处理normalizePage相关的业务逻辑。
     * @param page page参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    public static int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * normalizeSize方法用于处理normalizeSize相关的业务逻辑。
     * @param size size参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    public static int normalizeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }

    /**
     * calcOffset方法用于处理calcOffset相关的业务逻辑。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    public static int calcOffset(int page, int size) {
        return (normalizePage(page) - 1) * normalizeSize(size);
    }
}