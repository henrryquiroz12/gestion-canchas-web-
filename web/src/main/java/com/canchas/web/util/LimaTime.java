package com.canchas.web.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class LimaTime {
    
    public static final ZoneId ZONE_ID = ZoneId.of("America/Lima");

    private LimaTime() {}

    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(ZONE_ID);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }

    /**
     * Helper de presentación para rangos de slots operativos:
     * Cuando el slot finaliza a medianoche (LocalTime.MIDNIGHT / 00:00),
     * se presenta visualmente como "24:00" para claridad de los usuarios.
     * Ejemplo: 23:00 - 00:00 -> "23:00 - 24:00"
     */
    public static String formatSlotRange(LocalTime start, LocalTime end) {
        if (start == null) return "";
        String startStr = start.toString();
        if (startStr.length() > 5) startStr = startStr.substring(0, 5);
        String endStr;
        if (end == null || end.equals(LocalTime.MIDNIGHT)) {
            endStr = "24:00";
        } else {
            endStr = end.toString();
            if (endStr.length() > 5) endStr = endStr.substring(0, 5);
        }
        return startStr + " - " + endStr;
    }
}