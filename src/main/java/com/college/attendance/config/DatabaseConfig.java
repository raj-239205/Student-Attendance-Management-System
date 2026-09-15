package com.college.attendance.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Holds database configuration values.
 *
 * <p>
 * Configuration resolution precedence:
 * <ol>
 * <li>Environment variable (e.g. {@code DB_USER}, {@code DB_PASSWORD})</li>
 * <li>JVM System property (e.g. {@code -DDB_PASSWORD=...})</li>
 * <li>Optional local configuration file ({@code .env} or
 * {@code local.properties})</li>
 * <li>Sensible defaults for host, port, db name, and user</li>
 * </ol>
 * </p>
 *
 * <p>
 * <b>Security:</b> Passwords are never logged or included in {@link #toString()}.
 * </p>
 */
public final class DatabaseConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "college_attendance_db";
    private static final String DEFAULT_USER = "attendance_app";

    private final String host;
    private final String port;
    private final String databaseName;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        Properties localProps = loadLocalProperties();

        this.host = resolveValue("DB_HOST", localProps, DEFAULT_HOST);
        this.port = resolveValue("DB_PORT", localProps, DEFAULT_PORT);
        this.databaseName = resolveValue("DB_NAME", localProps, DEFAULT_DB_NAME);
        this.user = resolveValue("DB_USER", localProps, DEFAULT_USER);
        this.password = resolveValue("DB_PASSWORD", localProps, null);
    }

    /**
     * Parameterized constructor for custom testing or programmatic overrides.
     */
    public DatabaseConfig(String host, String port, String databaseName, String user, String password) {
        this.host = (host != null && !host.isBlank()) ? host.trim() : DEFAULT_HOST;
        this.port = (port != null && !port.isBlank()) ? port.trim() : DEFAULT_PORT;
        this.databaseName = (databaseName != null && !databaseName.isBlank()) ? databaseName.trim() : DEFAULT_DB_NAME;
        this.user = (user != null && !user.isBlank()) ? user.trim() : DEFAULT_USER;
        this.password = (password != null && !password.isBlank()) ? password.trim() : null;
    }

    /** JDBC connection URL for the configured database. */
    public String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Returns true if the password has been configured (non-null and non-blank).
     */
    public boolean isPasswordConfigured() {
        return password != null && !password.isBlank();
    }

    /**
     * Safe string representation for logs. Never includes the actual password.
     */
    @Override
    public String toString() {
        return "DatabaseConfig{"
                + "host='" + host + '\''
                + ", port='" + port + '\''
                + ", database='" + databaseName + '\''
                + ", user='" + user + '\''
                + ", passwordSet=" + isPasswordConfigured()
                + '}';
    }

    private static String resolveValue(String key, Properties fileProps, String defaultValue) {
        // 1. Environment variable
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }

        // 2. System property (e.g. -DDB_PASSWORD=...)
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = System.getProperty(key.toLowerCase());
        if (val != null && !val.isBlank()) {
            return val.trim();
        }

        // 3. File properties (.env or local.properties)
        if (fileProps != null) {
            val = fileProps.getProperty(key);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
            val = fileProps.getProperty(key.toLowerCase());
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }

        return defaultValue;
    }

    private static Properties loadLocalProperties() {
        Properties props = new Properties();

        // Check local.properties first, then .env
        File[] candidateFiles = {
                new File("local.properties"),
                new File(".env")
        };

        for (File f : candidateFiles) {
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    props.load(fis);
                    return props;
                } catch (IOException ignored) {
                    // Ignore file read error and continue
                }
            }
        }
        return props;
    }
}
