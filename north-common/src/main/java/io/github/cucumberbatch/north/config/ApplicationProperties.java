package io.github.cucumberbatch.north.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

public class ApplicationProperties {
    private static final String DEFAULT_RESOURCE = "north.properties";

    private static ApplicationProperties instance;
    private final Properties properties;

    private ApplicationProperties() throws IOException {
        this(loadDefaultProperties());
    }

    private ApplicationProperties(Properties properties) {
        this.properties = properties;
    }

    private static Properties loadDefaultProperties() throws IOException {
        ClassLoader classLoader = ApplicationProperties.class.getClassLoader();
        try (InputStream resourceStream = classLoader.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (resourceStream == null) {
                throw new IOException("Missing required classpath resource '" + DEFAULT_RESOURCE + "'");
            }
            Properties properties = new Properties();
            properties.load(resourceStream);
            return properties;
        }
    }

    /**
     * Installs a configuration explicitly. This is intended for launchers and tests
     * that need to use a file other than the default classpath resource.
     */
    public static synchronized void install(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        Properties copy = new Properties();
        copy.putAll(properties);
        instance = new ApplicationProperties(copy);
    }

    /**
     * Loads configuration from a stream. The caller owns the stream.
     */
    public static synchronized void load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        Properties properties = new Properties();
        properties.load(input);
        install(properties);
    }

    /**
     * Loads configuration from a file.
     */
    public static synchronized void load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            load(input);
        }
    }

    /**
     * Clears an explicitly installed configuration and restores lazy loading from
     * the default classpath resource.
     */
    public static synchronized void reset() {
        instance = null;
    }

    public static synchronized boolean isInstalled() {
        return instance != null;
    }

    public static String getProperty(String property) {
        return getInstance().properties.getProperty(property);
    }

    private static synchronized ApplicationProperties getInstance() {
        if (instance == null) {
            try {
                instance = new ApplicationProperties();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load '" + DEFAULT_RESOURCE + "'", e);
            }
        }
        return instance;
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
        return getAndParseProperty(property, Boolean::parseBoolean);
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
