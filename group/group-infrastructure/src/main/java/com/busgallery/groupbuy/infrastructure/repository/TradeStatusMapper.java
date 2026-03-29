package com.busgallery.groupbuy.infrastructure.repository;

/**
 * Mapping utility between DB numeric status and domain textual status.
 */
final class TradeStatusMapper {
    private TradeStatusMapper() {
    }

    static String toTeamStatus(int value) {
        return switch (value) {
            case 1 -> "SUCCESS";
            case 2 -> "FAILED";
            case 3 -> "CLOSED";
            default -> "GROUPING";
        };
    }

    static int toTeamStatusValue(String status) {
        if ("SUCCESS".equals(status)) {
            return 1;
        }
        if ("FAILED".equals(status)) {
            return 2;
        }
        if ("CLOSED".equals(status)) {
            return 3;
        }
        return 0;
    }

    static String toOrderStatus(int value) {
        return switch (value) {
            case 1 -> "PAID";
            case 2 -> "REFUNDED";
            case 3 -> "CANCELLED";
            case 4 -> "TIMEOUT";
            default -> "LOCKED";
        };
    }

    static int toOrderStatusValue(String status) {
        if ("PAID".equals(status)) {
            return 1;
        }
        if ("REFUNDED".equals(status)) {
            return 2;
        }
        if ("CANCELLED".equals(status)) {
            return 3;
        }
        if ("TIMEOUT".equals(status)) {
            return 4;
        }
        return 0;
    }
}
