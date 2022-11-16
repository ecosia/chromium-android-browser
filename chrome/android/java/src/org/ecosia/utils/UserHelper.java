package org.ecosia.utils;

import android.content.Context;

import java.util.UUID;

public class UserHelper {
    private static final String PREF_USER_ID = "com.ecosia.PREF_USER_ID";

    public static String getUserId(Context context) {
        String uid = SharedPreferencesHelpers.getString(context, PREF_USER_ID, "");
        if (uid.isEmpty()) {
            uid = UUID.randomUUID().toString();
            SharedPreferencesHelpers.putString(context, PREF_USER_ID, uid);
        }
        return uid;
    }
}
