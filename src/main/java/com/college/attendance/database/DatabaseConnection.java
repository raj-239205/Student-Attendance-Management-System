package com.college.attendance.database;

import com.college.attendance.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages JDBC connections to the MySQL database.
 * Uses {@link DatabaseConfig} for connection parameters.
 */
public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private final DatabaseConfig config;

    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Opens and returns a new JDBC connection.
     * The caller is responsible for closing it (prefer try-with-resources).
     *
     * @return a live {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUser(),
                config.getPassword()
        );
    }

    /**
     * Tests the database connection by opening and immediately closing it.
     *
     * @return true if the connection was successful, false otherwise
     */
    public boolean testConnection() {
        LOGGER.info("Testing database connection...");
        LOGGER.info("Configuration: " + config);

        try (Connection connection = getConnection()) {
            if (connection != null && connection.isValid(5)) {
                LOGGER.info("Database connection successful.");
                return true;
            } else {
                LOGGER.warning("Database connection returned but is not valid.");
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection failed: " + getSafeErrorMessage(e));
            logConnectionHelp(e);
            return false;
        }
    }

    /**
     * Builds an error description without exposing credentials.
     */
    private static String getSafeErrorMessage(SQLException e) {
        String message = e.getMessage();
        if (message == null) {
            return "Unknown SQL error (error code: " + e.getErrorCode() + ")";
        }
        return "SQL State: " + e.getSQLState()
                + ", Error Code: " + e.getErrorCode()
                + ", Message: " + message;
    }

    /**
     * Logs common troubleshooting steps based on the error code.
     */
    private static void logConnectionHelp(SQLException e) {
        int code = e.getErrorCode();
        if (code == 1045) {
            LOGGER.info("Hint: Access denied. Verify DB_USER and DB_PASSWORD environment variables.");
        } else if (code == 1049) {
            LOGGER.info("Hint: Database not found. Ensure 'college_attendance_db' exists in MySQL.");
        } else {
            LOGGER.info("Hint: Check that MySQL is running on the configured host and port.");
        }
    }
}
