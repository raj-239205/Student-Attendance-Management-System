package com.college.attendance.config;

/**
 * Holds database configuration values.
 * Reads from environment variables with sensible defaults for non-sensitive settings.
 *
 * <p><b>Required environment variables for credentials:</b></p>
 * <ul>
 *   <li>{@code DB_USER}     — MySQL username  (default: root)</li>
 *   <li>{@code DB_PASSWORD} — MySQL password  (no default; must be set)</li>
 * </ul>
 *
 * <p><b>Optional environment variables:</b></p>
 * <ul>
 *   <li>{@code DB_HOST} — default: localhost</li>
 *   <li>{@code DB_PORT} — default: 3306</li>
 *   <li>{@code DB_NAME} — default: college_attendance_db</li>
 * </ul>
 */
public final class DatabaseConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "college_attendance_db";
    private static final String DEFAULT_USER = "root";

    private final String host;
    private final String port;
    private final String databaseName;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        this.host = getEnvOrDefault("DB_HOST", DEFAULT_HOST);
        this.port = getEnvOrDefault("DB_PORT", DEFAULT_PORT);
        this.databaseName = getEnvOrDefault("DB_NAME", DEFAULT_DB_NAME);
        this.user = getEnvOrDefault("DB_USER", DEFAULT_USER);
        this.password = getEnvOrDefault("DB_PASSWORD", null);
    }

    /** JDBC connection URL for the configured database. */
    public String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
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

    /** Returns true if the password has been configured (non-null and non-blank). */
    public boolean isPasswordConfigured() {
        return password != null && !password.isBlank();
    }

    /**
     * Returns a safe description of the config for logging.
     * Never includes the password.
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

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return defaultValue;
    }
}
