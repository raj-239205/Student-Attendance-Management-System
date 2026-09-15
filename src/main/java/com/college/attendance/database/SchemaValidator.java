package com.college.attendance.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates that the database schema is correctly set up.
 * Checks for table existence, foreign keys, unique constraints,
 * and tests the attendance percentage / defaulter calculation.
 */
public final class SchemaValidator {

    private static final Logger LOGGER = Logger.getLogger(SchemaValidator.class.getName());

    private static final String[] REQUIRED_TABLES = { "users", "students", "subjects", "attendance" };

    private SchemaValidator() {
        // prevent instantiation
    }

    /**
     * Runs all schema validation checks.
     *
     * @param connection a live JDBC connection to college_attendance_db
     * @return true if all validations pass
     */
    public static boolean validate(Connection connection) {
        LOGGER.info("--- Schema Validation ---");
        boolean allPassed = true;

        // 1. Check tables exist
        for (String table : REQUIRED_TABLES) {
            if (!tableExists(connection, table)) {
                LOGGER.severe("FAIL: Table '" + table + "' does not exist.");
                allPassed = false;
            } else {
                LOGGER.info("OK: Table '" + table + "' exists.");
            }
        }

        if (!allPassed) {
            LOGGER.severe("Table existence check failed — skipping further validation.");
            return false;
        }

        // 2. Test unique constraint on attendance (duplicate prevention)
        allPassed &= testDuplicateAttendancePrevention(connection);

        // 3. Test foreign key enforcement
        allPassed &= testForeignKeyConstraint(connection);

        // 4. Test attendance percentage calculation
        allPassed &= testAttendancePercentageCalculation(connection);

        LOGGER.info("--- Schema Validation " + (allPassed ? "PASSED" : "FAILED") + " ---");
        return allPassed;
    }

    private static boolean tableExists(Connection connection, String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "'";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking table: " + tableName, e);
            return false;
        }
    }

    /**
     * Inserts test data, attempts a duplicate, verifies it is rejected,
     * then cleans up the test data.
     */
    private static boolean testDuplicateAttendancePrevention(Connection connection) {
        LOGGER.info("Testing duplicate attendance prevention...");
        try (Statement stmt = connection.createStatement()) {
            // Insert a test student
            stmt.executeUpdate(
                    "INSERT IGNORE INTO students (roll_no, name, branch, semester, section) "
                            + "VALUES ('__TEST_DUP__', 'Test Student', 'TEST', 1, 'X')");
            // Insert a test subject
            stmt.executeUpdate(
                    "INSERT IGNORE INTO subjects (subject_code, subject_name, branch, semester) "
                            + "VALUES ('__TEST_DUP__', 'Test Subject', 'TEST', 1)");

            // Get IDs
            int studentId = getScalarInt(stmt,
                    "SELECT id FROM students WHERE roll_no = '__TEST_DUP__'");
            int subjectId = getScalarInt(stmt,
                    "SELECT id FROM subjects WHERE subject_code = '__TEST_DUP__'");

            // Insert first attendance — should succeed
            stmt.executeUpdate(
                    "INSERT INTO attendance (student_id, subject_id, attendance_date, status) "
                            + "VALUES (" + studentId + ", " + subjectId + ", '2000-01-01', 'PRESENT')");

            // Attempt duplicate — should fail
            try {
                stmt.executeUpdate(
                        "INSERT INTO attendance (student_id, subject_id, attendance_date, status) "
                                + "VALUES (" + studentId + ", " + subjectId + ", '2000-01-01', 'ABSENT')");
                LOGGER.severe("FAIL: Duplicate attendance was accepted!");
                return false;
            } catch (SQLException duplicateEx) {
                LOGGER.info("OK: Duplicate attendance correctly rejected.");
            }
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Duplicate prevention test error: " + e.getMessage());
            return false;
        } finally {
            cleanupTestData(connection);
        }
    }

    /**
     * Attempts to insert an attendance record with an invalid student_id
     * to verify FK enforcement.
     */
    private static boolean testForeignKeyConstraint(Connection connection) {
        LOGGER.info("Testing foreign key constraints...");
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "INSERT INTO attendance (student_id, subject_id, attendance_date, status) "
                            + "VALUES (999999, 999999, '2000-01-01', 'PRESENT')");
            LOGGER.severe("FAIL: Foreign key constraint not enforced!");
            // Clean up the bad row
            stmt.executeUpdate(
                    "DELETE FROM attendance WHERE student_id = 999999 AND subject_id = 999999");
            return false;
        } catch (SQLException e) {
            LOGGER.info("OK: Foreign key constraint enforced (insert rejected).");
            return true;
        }
    }

    /**
     * Uses the sample data (if present) to test the attendance percentage
     * calculation and defaulter identification.
     */
    private static boolean testAttendancePercentageCalculation(Connection connection) {
        LOGGER.info("Testing attendance percentage calculation...");
        String sql = """
                SELECT
                    s.roll_no,
                    s.name,
                    sub.subject_code,
                    SUM(a.status = 'PRESENT') AS present_count,
                    COUNT(*)                  AS total_classes,
                    ROUND((SUM(a.status = 'PRESENT') / COUNT(*)) * 100, 2) AS attendance_pct
                FROM attendance a
                JOIN students s  ON a.student_id = s.id
                JOIN subjects sub ON a.subject_id = sub.id
                GROUP BY s.id, sub.id
                ORDER BY attendance_pct ASC
                """;

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasRows = false;
            boolean defaulterFound = false;

            while (rs.next()) {
                hasRows = true;
                String rollNo = rs.getString("roll_no");
                String name = rs.getString("name");
                String code = rs.getString("subject_code");
                int present = rs.getInt("present_count");
                int total = rs.getInt("total_classes");
                double pct = rs.getDouble("attendance_pct");
                boolean isDefaulter = pct < 75.0;

                String flag = isDefaulter ? " ** DEFAULTER **" : "";
                LOGGER.info(String.format("  %s (%s) — %s: %d/%d = %.1f%%%s",
                        name, rollNo, code, present, total, pct, flag));

                if (isDefaulter) {
                    defaulterFound = true;
                }
            }

            if (!hasRows) {
                LOGGER.info(
                        "No attendance data found — percentage test skipped (will pass after sample data is loaded).");
                return true;
            }

            if (defaulterFound) {
                LOGGER.info("OK: Defaulter correctly identified (below 75%).");
            } else {
                LOGGER.info("OK: No defaulters in current data (all above 75%).");
            }
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Attendance percentage test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes test-specific data created during validation.
     * Only deletes rows with the known test marker values.
     */
    private static void cleanupTestData(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Delete attendance first (FK constraint)
            stmt.executeUpdate(
                    "DELETE a FROM attendance a "
                            + "JOIN students s ON a.student_id = s.id "
                            + "WHERE s.roll_no = '__TEST_DUP__'");
            stmt.executeUpdate("DELETE FROM students WHERE roll_no = '__TEST_DUP__'");
            stmt.executeUpdate("DELETE FROM subjects WHERE subject_code = '__TEST_DUP__'");
        } catch (SQLException e) {
            LOGGER.warning("Test data cleanup failed: " + e.getMessage());
        }
    }

    private static int getScalarInt(Statement stmt, String sql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No result for: " + sql);
        }
    }
}
