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
 * Stage 2.1 behaviour:
 * </p>
 * <ol>
 * <li>Loads database configuration from environment variables.</li>
 * <li>Tests the JDBC connection to MySQL.</li>
 * <li>Initializes the hardened database schema (tables, constraints, indexes).</li>
 * <li>Safely skips automatic sample data seeding unless explicitly requested via
 *     {@code --seed-sample-data} or {@code -Dseed.sample.data=true}.</li>
 * <li>Validates the schema (PKs, FKs, unique duplicate prevention, invalid status rejection,
 *     and 75% defaulter boundary condition).</li>
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
        LOGGER.info("Stage 2.1 — Database Hardening & Final Validation");
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
            // Run schema.sql (uses CREATE TABLE IF NOT EXISTS — safe to re-run)
            boolean schemaCreated = SchemaInitializer.runScript(conn, "/schema.sql");
            if (!schemaCreated) {
                LOGGER.severe("Schema initialization failed.");
                new SmokeTestWindow(true, false).show();
                return;
            }

            // Explicit sample data seeding check (prevents unexpected duplicate test data)
            boolean shouldSeedSample = false;
            if (args != null) {
                for (String arg : args) {
                    if ("--seed-sample-data".equalsIgnoreCase(arg)) {
                        shouldSeedSample = true;
                        break;
                    }
                }
            }
            if (Boolean.getBoolean("seed.sample.data")) {
                shouldSeedSample = true;
            }

            if (shouldSeedSample) {
                LOGGER.info("Explicit sample data seeding requested — executing sample_data.sql...");
                SchemaInitializer.runScript(conn, "/sample_data.sql");
            } else {
                LOGGER.info("Automatic sample data seeding SKIPPED (use --seed-sample-data to run sample_data.sql).");
            }

            // Validate hardened schema
            schemaValid = SchemaValidator.validate(conn);

            LOGGER.info("");
            LOGGER.info("Default defaulter threshold: "
                    + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "%");

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
