package org.ecosia.utils;

import android.os.Build;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimestampHelpers {
    // https://developer.android.com/reference/java/text/SimpleDateFormat.html#date-and-time-patterns
    // X for TimeZone is not available below API_24
    private static final String DATE_FORMAT_COMPLETE_API_LEVEL_24 = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String DATE_FORMAT_COMPLETE_API_LEVEL_1 = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String DATE_FORMAT_COMPACT = "d MMM";

    public static long parseTimestamp(String timeString) {
        String timeStringWithoutMs = sanitizeMilliSecondsAndTimezone(timeString);
        DateFormat formatter = new SimpleDateFormat(getCompleteDateFormat(), Locale.US);

        try {
            Date date = formatter.parse(timeStringWithoutMs);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getCompleteFormattedDate(long timestamp) {
        return getFormattedDate(timestamp, getCompleteDateFormat());
    }

    public static String getCompactFormattedDate(long timestamp) {
        return getFormattedDate(timestamp, DATE_FORMAT_COMPACT);
    }

    private static String getFormattedDate(long timestamp, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        Date date = calendar.getTime();
        DateFormat formatter = new SimpleDateFormat(format, Locale.US);
        return formatter.format(date);
    }

    private static String getCompleteDateFormat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return DATE_FORMAT_COMPLETE_API_LEVEL_24;
        } else {
            return DATE_FORMAT_COMPLETE_API_LEVEL_1;
        }
    }

    private static String sanitizeMilliSecondsAndTimezone(String timeString) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return timeString.replaceFirst("\\.\\d+", "");
        } else {
            //this assumes all time strings are UTC based
            return timeString.replaceFirst("(\\.|Z)+.*", "+0000");
        }
    }
}
