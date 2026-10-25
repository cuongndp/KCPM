package com.hospital.management.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private BusinessTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}