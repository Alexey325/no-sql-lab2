package com.example.nosqllab2.util;

import com.example.nosqllab2.exception.custom.InvalidShowDateException;
import com.google.cloud.Timestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShowDateParser {

    private static final Pattern SHOW_DATE_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2}) (\\d{2}):(\\d{2})$");

    public static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private ShowDateParser() {
    }

    // год в формате DD.MM HH:mm не передаётся, подставляется текущий
    public static LocalDateTime parse(String value) {
        Matcher matcher = SHOW_DATE_PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new InvalidShowDateException();
        }

        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int hours = Integer.parseInt(matcher.group(3));
        int minutes = Integer.parseInt(matcher.group(4));

        try {
            return LocalDateTime.of(now().getYear(), month, day, hours, minutes);
        } catch (java.time.DateTimeException ex) {
            throw new InvalidShowDateException();
        }
    }

    // ключ лока и флага правок — дата показа в формате YYYY-MM-DD
    public static String toDateKey(LocalDateTime showDate) {
        return showDate.toLocalDate().toString();
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static Timestamp toStoredValue(LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(ZONE).toInstant();

        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    public static LocalDateTime fromStoredValue(Timestamp value) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(value.getSeconds(), value.getNanos()), ZONE);
    }
}
