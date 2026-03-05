package com.busgallery.busgallery.util;

public final class PageUtils {

    private PageUtils() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, 1);
    }

    public static int normalizeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }

    public static int calcOffset(int page, int size) {
        return (normalizePage(page) - 1) * normalizeSize(size);
    }
}