package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.database.DemoDataSeeder;
import com.college.attendance.database.SchemaInitializer;
import com.college.attendance.database.SchemaValidator;
import com.college.attendance.database.UserSeeder;
import com.college.attendance.service.AuthenticationService;
import com.college.attendance.ui.LoginWindow;
import com.college.attendance.ui.SmokeTestWindow;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point for the College Attendance Management System.
 *
 * <p>
 * Standard application lifecycle (Stage 3):
 * </p>
 * <ol>
 * <li>Load database configuration securely from environment variables /
 * .env.</li>
 * <li>Establish and verify database connectivity.</li>
 * <li>Seed initial test users (admin / faculty) if the {@code users} table is
 * empty.</li>
 * <li>Launch the modern {@link LoginWindow} on the Swing Event Dispatch
 * Thread.</li>
 * </ol>
 *
 * <p>
 * Diagnostic / developer flags (optional):
 * <ul>
 * <li>{@code --validate-schema} : Runs automated schema validation.</li>
 * <li>{@code --seed-sample-data} : Runs sample_data.sql for testing.</li>
 * <li>{@code --smoke-test} : Displays the Stage 2.1 smoke-test window.</li>
 * </ul>
 * </p>
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
        // prevent instantiation
    }

    public static void main(String[] args) {
        LOGGER.info("=== College Attendance Management System ===");
        LOGGER.info("Stage 3 — Authentication & Login System");
        LOGGER.info("");

        // 1. Load database configuration
        DatabaseConfig config = new DatabaseConfig();
        LOGGER.info("Database configuration loaded: " + config);

        if (!config.isPasswordConfigured()) {
            LOGGER.warning("DB_PASSWORD is not configured. Please set the DB_PASSWORD environment variable or .env file.");
        }

        // 2. Initialize connection
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        boolean connected = dbConnection.testConnection();

        if (!connected) {
            LOGGER.severe("Cannot connect to database. Starting LoginWindow in offline mode.");
        }

        // 3. Handle optional development flags
        boolean runSmokeTest = hasFlag(args, "--smoke-test");
        boolean runValidation = hasFlag(args, "--validate-schema");
        boolean runSampleSeed = hasFlag(args, "--seed-sample-data");
        boolean runDemoSeed = hasFlag(args, "--seed-demo-data");

        if (connected) {
            try (Connection conn = dbConnection.getConnection()) {
                // Seed default admin/faculty test accounts if configured and not yet present
                String seedPassword = config.getPassword();
                if (seedPassword != null && !seedPassword.isBlank()) {
                    UserSeeder.seedDefaultUsers(conn, seedPassword);
                    // Populate realistic demonstration dataset for evaluator presentation
                    DemoDataSeeder.seedDemoData(conn, seedPassword);
                }

                if (runSampleSeed) {
                    LOGGER.info("Executing sample_data.sql as requested...");
                    SchemaInitializer.runScript(conn, "/sample_data.sql");
                }

                if (runDemoSeed) {
                    LOGGER.info("Explicitly executing DemoDataSeeder as requested via flag...");
                    DemoDataSeeder.seedDemoData(conn, (seedPassword != null) ? seedPassword : "Admin@123");
                }

                if (runValidation) {
                    LOGGER.info("Executing SchemaValidator as requested...");
                    boolean valid = SchemaValidator.validate(conn);
                    LOGGER.info("Schema Validation: " + (valid ? "PASSED" : "FAILED"));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Database setup check encountered an error: " + e.getMessage());
            }
        }

        // 4. Launch UI
        if (runSmokeTest) {
            LOGGER.info("Launching SmokeTestWindow (requested via flag)...");
            SwingUtilities.invokeLater(() -> new SmokeTestWindow(connected, true).show());
        } else {
            LOGGER.info("Launching LoginWindow...");
            AuthenticationService authService = new AuthenticationService(dbConnection);
            SwingUtilities.invokeLater(() -> {
                LoginWindow loginWindow = new LoginWindow(authService);
                loginWindow.setVisible(true);
            });
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }
}
