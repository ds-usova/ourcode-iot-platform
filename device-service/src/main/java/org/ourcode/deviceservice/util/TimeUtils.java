package org.ourcode.deviceservice.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtils {

    private static ZoneId ZONE_ID = ZoneId.of("UTC");

    private TimeUtils() {
        // Utility class
    }

    public static long now() {
        return ZonedDateTime.now(ZONE_ID).toInstant().toEpochMilli();
    }

}
