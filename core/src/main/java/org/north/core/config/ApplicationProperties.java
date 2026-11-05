package org.north.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.function.Function;

public class ApplicationProperties {
    private static ApplicationProperties instance;
    private final Properties properties;

    private ApplicationProperties() throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        try (InputStream resourceStream = classLoader.getResourceAsStream("north.properties")) {
            this.properties = new Properties();
            this.properties.load(resourceStream);
        }
    }

    public static String getProperty(String property) {
        if (instance == null) {
            try {
                instance = new ApplicationProperties();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return instance.properties.getProperty(property);
    }

    private static <Result> Result getAndParseProperty(String property, Function<String, Result> function) {
        String value = getProperty(property);
        try {
            return function.apply(value);
        } catch (RuntimeException e) {
            e.initCause(new RuntimeException(String.format("Unable to parse property '%s' with incorrect value '%s'", property, value)));
            throw e;
        }
    }

    public static boolean getBoolean(String property) {
        return Boolean.parseBoolean(getProperty(property));
    }

    public static byte getByte(String property) {
        return getAndParseProperty(property, Byte::parseByte);
    }

    public static int getInt(String property) {
        return getAndParseProperty(property, Integer::parseInt);
    }

    public static long getLong(String property) {
        return getAndParseProperty(property, Long::parseLong);
    }

    public static float getFloat(String property) {
        return getAndParseProperty(property, Float::parseFloat);
    }

    public static double getDouble(String property) {
        return getAndParseProperty(property, Double::parseDouble);
    }

    public static LocalDateTime getDateTime(String property) {
        return getAndParseProperty(property, LocalDateTime::parse);
    }
}
