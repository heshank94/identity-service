package com.dreamstartlabs.dreamlink.identity.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Heshan Karunaratne
 */
public final class TimeUtils {
    private TimeUtils() {
    }

    private static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatTimestamp(Instant instant) {
        if (instant == null) {
            return "null";
        }
        return DEFAULT_TIMESTAMP_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static long elapsedMillis(Instant startTime) {
        return Duration.between(startTime, Instant.now()).toMillis();
    }
}
