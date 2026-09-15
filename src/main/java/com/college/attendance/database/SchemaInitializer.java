package com.college.attendance.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads SQL scripts from the classpath and executes them against the database.
 * Used to initialize the schema (tables, indexes, constraints) on startup.
 *
 * <p>
 * Scripts must use semicolons ({@code ;}) as statement delimiters.
 * Lines starting with {@code --} are treated as comments and skipped.
 * </p>
 */
public final class SchemaInitializer {

    private static final Logger LOGGER = Logger.getLogger(SchemaInitializer.class.getName());

    private SchemaInitializer() {
        // prevent instantiation
    }

    /**
     * Executes the given SQL script resource against the provided connection.
     *
     * @param connection   a live JDBC connection
     * @param resourcePath classpath resource path (e.g. {@code "/schema.sql"})
     * @return true if all statements executed successfully
     */
    public static boolean runScript(Connection connection, String resourcePath) {
        LOGGER.info("Running SQL script: " + resourcePath);

        String sql = loadResource(resourcePath);
        if (sql == null) {
            LOGGER.severe("Could not load SQL script: " + resourcePath);
            return false;
        }

        // Split on semicolons to get individual statements
        String[] statements = sql.split(";");
        int executed = 0;

        try (Statement stmt = connection.createStatement()) {
            for (String raw : statements) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                stmt.execute(trimmed);
                executed++;
            }
            LOGGER.info("SQL script executed successfully (" + executed + " statements).");
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL script execution failed at statement #" + (executed + 1)
                    + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a text resource from the classpath.
     */
    private static String loadResource(String resourcePath) {
        try (InputStream is = SchemaInitializer.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.severe("Resource not found on classpath: " + resourcePath);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();
                    // Skip comment-only lines
                    if (trimmedLine.startsWith("--") || trimmedLine.isEmpty()) {
                        continue;
                    }
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read resource: " + resourcePath, e);
            return null;
        }
    }
}
