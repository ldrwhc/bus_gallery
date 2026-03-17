package com.busgallery.busgallery.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DateUtils类用于封装DateUtils相关的领域职责（所在包：com.busgallery.busgallery.util）。
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * DateUtils构造器用于初始化对象状态。
     * @return 构造器无返回值。
     */
    private DateUtils() {
    }

    /**
     * format方法用于处理format相关的业务逻辑。
     * @param date date参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    public static String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /**
     * format方法用于处理format相关的业务逻辑。
     * @param dateTime dateTime参数，详见调用方上下文。
     * @return 返回String类型结果。
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
    }
}