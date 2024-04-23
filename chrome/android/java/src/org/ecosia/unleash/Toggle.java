package org.ecosia.unleash;

import androidx.annotation.Nullable;

public class Toggle {
    public enum Name {
        // Add other enum constants, for example:
        EXAMPLE_CONSTANT("example_value");

        private final String value;

        Name(final String value) {
            this.value = value;
        }

        public String getName() {
            return value;
        }
    }

    public static class Variant {
        public String name;
        public boolean enabled;

        @Nullable
        public Payload payload;
    }

    public static class Payload {
        public String type;
        public String value;
    }

    public String name;
    public boolean enabled;
    public Variant variant;
}

