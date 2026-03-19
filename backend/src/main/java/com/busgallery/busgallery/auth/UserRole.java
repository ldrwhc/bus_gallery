package com.busgallery.busgallery.auth;

public enum UserRole {
    STATION,
    REVIEWER,
    USER;

    public static UserRole fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return USER;
        }
        try {
            return UserRole.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return USER;
        }
    }
}
