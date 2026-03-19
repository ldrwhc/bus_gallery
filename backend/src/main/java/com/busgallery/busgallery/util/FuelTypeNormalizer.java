package com.busgallery.busgallery.util;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

public final class FuelTypeNormalizer {

    private static final Map<String, String> FUEL_MAP = Map.ofEntries(
            Map.entry("gasoline", "汽油"),
            Map.entry("diesel", "柴油"),
            Map.entry("electric", "纯电"),
            Map.entry("hybrid", "混动"),
            Map.entry("gas", "燃气"),
            Map.entry("diesel_electric", "柴油+电"),
            Map.entry("cng", "压缩天然气"),
            Map.entry("cng_electric", "压缩天然气+电"),
            Map.entry("lng", "液化天然气"),
            Map.entry("lng_electric", "液化天然气+电"),
            Map.entry("hydrogen_electric", "压缩氢气+电"),
            Map.entry("compressed_hydrogen_electric", "压缩氢气+电")
    );

    private FuelTypeNormalizer() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String trimmed = raw.trim();
        String mapped = FUEL_MAP.get(trimmed.toLowerCase(Locale.ROOT));
        return mapped != null ? mapped : trimmed;
    }
}
