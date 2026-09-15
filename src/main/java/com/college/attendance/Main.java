package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.database.SchemaInitializer;
import com.college.attendance.database.SchemaValidator;
import com.college.attendance.ui.SmokeTestWindow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Application entry point for the College Attendance Management System.
 *
 * <p>
 * Stage 2 behaviour:
 * </p>
 * <ol>
 * <li>Loads database configuration from environment variables.</li>
 * <li>Tests the JDBC connection to MySQL.</li>
 * <li>Initializes the database schema (tables, indexes, constraints).</li>
 * <li>Validates the schema (FK, unique constraints, attendance
 * calculation).</li>
 * <li>Opens the Swing smoke-test window with status information.</li>
 * </ol>
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
        // prevent instantiation
    }

    public static void main(String[] args) {
        LOGGER.info("=== College Attendance Management System ===");
        LOGGER.info("Stage 2 — Database Design & Setup");
        LOGGER.info("");

        // 1. Load configuration
        DatabaseConfig config = new DatabaseConfig();
        LOGGER.info("Database configuration loaded.");

        if (!config.isPasswordConfigured()) {
            LOGGER.warning(
                    "DB_PASSWORD environment variable is not set. "
                            + "The connection will attempt with a null password. "
                            + "Set DB_PASSWORD before running if your MySQL user requires a password.");
        }

        // 2. Test JDBC connection
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        boolean connected = dbConnection.testConnection();

        if (!connected) {
            LOGGER.severe("Cannot proceed — database connection failed.");
            new SmokeTestWindow(false, false).show();
            return;
        }

        // 3. Initialize schema & validate
        boolean schemaValid = false;
        try (Connection conn = dbConnection.getConnection()) {
            // Run schema.sql
            boolean schemaCreated = SchemaInitializer.runScript(conn, "/schema.sql");
            if (!schemaCreated) {
                LOGGER.severe("Schema initialization failed.");
                new SmokeTestWindow(true, false).show();
                return;
            }

            // Load sample data (optional — uses INSERT IGNORE, safe to re-run)
            SchemaInitializer.runScript(conn, "/sample_data.sql");

            // Validate schema
            schemaValid = SchemaValidator.validate(conn);

            LOGGER.info("");
            LOGGER.info("Default attendance threshold: "
                    + AppConstants.DEFAULT_ATTENDANCE_THRESHOLD + "%");

        } catch (SQLException e) {
            LOGGER.severe("Database error during schema setup: " + e.getMessage());
        }

        LOGGER.info("");
        LOGGER.info("Result: Connection=" + connected + ", Schema=" + (schemaValid ? "VALID" : "INVALID"));

        // 4. Launch Swing smoke-test window
        LOGGER.info("Launching Swing smoke-test window...");
        new SmokeTestWindow(connected, schemaValid).show();
    }
}
