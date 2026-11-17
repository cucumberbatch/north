package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.config.ApplicationProperties;

import java.nio.file.Paths;

public class Application {
    public static void main(String[] args) throws Exception {
        String configPath = resolveConfigPath(args);
        if (configPath != null) {
            ApplicationProperties.load(Paths.get(configPath));
        }
        new Engine().run();
    }

    private static String resolveConfigPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--config".equals(arg)) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("--config requires a path");
                }
                return args[i + 1];
            }
            if (arg.startsWith("--config=")) {
                return arg.substring("--config=".length());
            }
        }

        String fromProperty = System.getProperty("north.config");
        if (hasText(fromProperty)) {
            return fromProperty;
        }

        String fromEnvironment = System.getenv("NORTH_CONFIG");
        if (hasText(fromEnvironment)) {
            return fromEnvironment;
        }

        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
