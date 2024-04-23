package org.ecosia.utils;

import java.util.Calendar;

public class DateHolder {

    private final int mYear;
    private final int mDayOfYear;

    public DateHolder(final Calendar calendar) {
        mYear = calendar.get(Calendar.YEAR);
        mDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
    }

    public boolean isAtLeastOneDayLaterAs(final DateHolder comparedDateHolder) {
        return mYear > comparedDateHolder.mYear || mDayOfYear > comparedDateHolder.mDayOfYear;
    }

}
