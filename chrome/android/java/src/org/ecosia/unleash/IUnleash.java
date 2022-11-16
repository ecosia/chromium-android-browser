package org.ecosia.unleash;

import javax.annotation.Nullable;

public interface IUnleash {
    boolean isReady();
    boolean isEnabled(Toggle.Name toggle);
    @Nullable Toggle.Variant getVariant(Toggle.Name toggle);
    void onStartOrResume();
    void addListener(UnleashCallback callback);
    void removeListener(UnleashCallback callback);
}
