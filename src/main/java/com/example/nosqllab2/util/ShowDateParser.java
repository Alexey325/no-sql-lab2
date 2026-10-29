package com.example.nosqllab2.util;

import com.example.nosqllab2.exception.custom.InvalidShowDateException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShowDateParser {

    private static final Pattern SHOW_DATE_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2}) (\\d{2}):(\\d{2})$");

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
            return LocalDateTime.of(LocalDate.now().getYear(), month, day, hours, minutes);
        } catch (java.time.DateTimeException ex) {
            throw new InvalidShowDateException();
        }
    }

    // ключ лока и флага правок — дата показа в формате YYYY-MM-DD
    public static String toDateKey(LocalDateTime showDate) {
        return showDate.toLocalDate().toString();
    }
}
