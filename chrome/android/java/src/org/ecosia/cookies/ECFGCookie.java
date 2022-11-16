package org.ecosia.cookies;

public class ECFGCookie {
    private static final String personalCounterKey = "t";
    private int mPersonalCounter;

    public ECFGCookie(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return;
        }
        mPersonalCounter = 0;
        String[] components = rawValue.split(":");
        for (String component: components) {

            String[] pair = component.split("=");
            if (pair.length != 2) { break; }
            if (pair[0].equals(personalCounterKey)) {
                mPersonalCounter = Integer.parseInt(pair[1]);   // TODO test Number Format Exceptions
            }
        }
    }

    public int getPersonalCounter() {
        return mPersonalCounter;
    }
}
