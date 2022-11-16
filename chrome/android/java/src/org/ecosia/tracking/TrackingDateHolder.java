package org.ecosia.tracking;

import java.util.Calendar;

public class TrackingDateHolder {

    private final int mYear;
    private final int mDayOfYear;

    public TrackingDateHolder(final Calendar calendar) {
        mYear = calendar.get(Calendar.YEAR);
        mDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
    }

    public boolean isAtLeastOneDayLaterAs(final TrackingDateHolder comparedDateHolder) {
        return mYear > comparedDateHolder.mYear || mDayOfYear > comparedDateHolder.mDayOfYear;
    }

}
