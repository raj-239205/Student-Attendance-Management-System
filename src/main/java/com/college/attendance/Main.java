package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.ui.SmokeTestWindow;

import java.util.logging.Logger;

/**
 * Application entry point for the College Attendance Management System.
 *
 * <p>Stage 1 behaviour:</p>
 * <ol>
 *   <li>Loads database configuration from environment variables.</li>
 *   <li>Tests the JDBC connection to MySQL.</li>
 *   <li>Opens a small Swing window confirming the setup status.</li>
 * </ol>
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
        // prevent instantiation
    }

    public static void main(String[] args) {
        LOGGER.info("=== College Attendance Management System ===");
        LOGGER.info("Stage 1 — Setup Verification");
        LOGGER.info("");

        // 1. Load configuration
        DatabaseConfig config = new DatabaseConfig();
        LOGGER.info("Database configuration loaded.");

        if (!config.isPasswordConfigured()) {
            LOGGER.warning(
                "DB_PASSWORD environment variable is not set. "
                + "The connection will attempt with a null password. "
                + "Set DB_PASSWORD before running if your MySQL user requires a password."
            );
        }

        // 2. Test JDBC connection
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        boolean connected = dbConnection.testConnection();

        LOGGER.info("");
        if (connected) {
            LOGGER.info("Result: Database connection successful.");
        } else {
            LOGGER.warning("Result: Database connection FAILED. See messages above for details.");
        }

        // 3. Launch Swing smoke-test window
        LOGGER.info("Launching Swing smoke-test window...");
        SmokeTestWindow smokeTest = new SmokeTestWindow(connected);
        smokeTest.show();
    }
}
