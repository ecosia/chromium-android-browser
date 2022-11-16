package org.ecosia.utils;

public class TimestampProviderImplementation implements TimestampProvider {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
