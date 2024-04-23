package org.ecosia.utils;

import androidx.annotation.NonNull;

public interface Logger {

    void logDebug(@NonNull final String tag, @NonNull final String message);

    void logDebug(@NonNull final String tag, @NonNull final String message, @NonNull final Throwable throwable);

    void logDebug(@NonNull final Object instance, @NonNull final String message);

    void logDebug(@NonNull final Object instance, @NonNull final String message, @NonNull final Throwable throwable);

    void logError(@NonNull final String tag, @NonNull final String message);

    void logError(@NonNull final String tag, @NonNull final String message, @NonNull final Throwable throwable);

    void logError(@NonNull final Object instance, @NonNull final String message);

    void logError(@NonNull final Object instance, @NonNull final String message, @NonNull final Throwable throwable);

}
