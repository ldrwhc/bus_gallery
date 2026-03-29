package com.busgallery.groupbuy.types.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for generating business identifiers.
 */
public final class IdFactory {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private IdFactory() {
    }

    /**
     * Generate order id.
     *
     * @return order id
     */
    public static String nextOrderId() {
        return "ORD" + TS.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * Generate team id.
     *
     * @return team id
     */
    public static String nextTeamId() {
        return "TEAM" + TS.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(100, 999);
    }

    /**
     * Generate event id.
     *
     * @return event id
     */
    public static String nextEventId() {
        return "EVT" + TS.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * Generate user trade record id.
     *
     * @return record id
     */
    public static String nextRecordId() {
        return "REC" + TS.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * Generate user message id.
     *
     * @return message id
     */
    public static String nextMessageId() {
        return "MSG" + TS.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
