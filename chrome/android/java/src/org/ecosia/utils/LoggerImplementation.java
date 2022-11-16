package org.ecosia.utils;

import androidx.annotation.NonNull;

import android.util.Log;

/*
    So we can test logger invocations and/or avoid those calls during tests.
    If we want to use some kind of a third-party remote logging provider we could also place that here.
 */
public class LoggerImplementation implements Logger {

    private static Logger sLogger;

    // Intended for testing purposes
    @NonNull
    public static void setLogger(@NonNull final Logger logger) {
        sLogger = logger;
    }

    @NonNull
    public static Logger getLogger() {
        if (sLogger == null) {
            sLogger = new LoggerImplementation();
        }

        return sLogger;
    }

    @Override
    public void logDebug(@NonNull final String tag, @NonNull final String message) {
        Log.d(tag, message);
    }

    @Override
    public void logDebug(@NonNull final String tag, @NonNull final String message, @NonNull final Throwable throwable) {
        Log.d(tag, message, throwable);
    }

    @Override
    public void logDebug(@NonNull final Object instance, @NonNull final String message) {
        final String logTag = instance.getClass().getSimpleName();
        logDebug(logTag, message);
    }

    @Override
    public void logDebug(@NonNull final Object instance, String message, @NonNull final Throwable throwable) {
        final String logTag = instance.getClass().getSimpleName();
        logDebug(logTag, message, throwable);
    }

    @Override
    public void logError(@NonNull final String tag, @NonNull final String message) {
        Log.e(tag, message);
    }

    @Override
    public void logError(@NonNull final String tag, @NonNull final String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }

    @Override
    public void logError(@NonNull final Object instance, @NonNull final String message) {
        final String logTag = instance.getClass().getSimpleName();
        logError(logTag, message);
    }

    @Override
    public void logError(@NonNull Object instance, @NonNull final String message, @NonNull final Throwable throwable) {
        final String logTag = instance.getClass().getSimpleName();
        logError(logTag, message, throwable);
    }
}
