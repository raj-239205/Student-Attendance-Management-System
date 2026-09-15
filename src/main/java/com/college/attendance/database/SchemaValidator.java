package com.college.attendance.database;

import com.college.attendance.config.AppConstants;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates that the database schema is correctly set up and hardened.
 *
 * <p>Performs checks for:
 * <ul>
 *   <li>Required table existence (users, students, subjects, attendance)</li>
 *   <li>Insertion of valid student and subject</li>
 *   <li>Insertion of PRESENT and ABSENT attendance</li>
 *   <li>Duplicate attendance rejection (unique constraint)</li>
 *   <li>Foreign key constraint enforcement (invalid student and subject)</li>
 *   <li>Invalid attendance status rejection (CHECK / ENUM constraints)</li>
 *   <li>Dynamic attendance percentage calculation</li>
 *   <li>Boundary testing for the 75% defaulter threshold:
 *     <ul>
 *       <li>Students with exactly 75% are NOT defaulters (percentage &lt; 75 is false)</li>
 *       <li>Students below 75% ARE defaulters (percentage &lt; 75 is true)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>All test records use the prefix {@code __TEST_} and are strictly cleaned up afterward.
 * Unrelated existing data is never modified or deleted.</p>
 */
public final class SchemaValidator {

    private static final Logger LOGGER = Logger.getLogger(SchemaValidator.class.getName());

    private static final String[] REQUIRED_TABLES = { "users", "students", "subjects", "attendance" };

    private static final String TEST_STUDENT_PREFIX = "__TEST_";
    private static final String TEST_STUDENT_A = "__TEST_STU_75__";
    private static final String TEST_STUDENT_B = "__TEST_STU_50__";
    private static final String TEST_SUBJECT = "__TEST_SUB__";

    private SchemaValidator() {
        // prevent instantiation
    }

    /**
     * Runs all database schema hardening and validation tests.
     *
     * @param connection a live JDBC connection to college_attendance_db
     * @return true if all validations pass
     */
    public static boolean validate(Connection connection) {
        LOGGER.info("========================================");
        LOGGER.info("Starting Database Schema Validation");
        LOGGER.info("========================================");

        boolean allPassed = true;

        try {
            // 1. Check required tables exist
            allPassed &= testTablesExist(connection);
            if (!allPassed) {
                LOGGER.severe("FAIL: Table existence check failed — aborting remaining tests.");
                return false;
            }

            // 2. Test valid student and subject insertion, plus PRESENT and ABSENT attendance
            allPassed &= testValidEntitiesAndAttendance(connection);

            // 3. Test duplicate attendance prevention (composite unique constraint)
            allPassed &= testDuplicateAttendancePrevention(connection);

            // 4. Test foreign key constraints
            allPassed &= testForeignKeyConstraints(connection);

            // 5. Test invalid attendance status rejection
            allPassed &= testInvalidAttendanceStatus(connection);

            // 6. Test attendance calculation and 75% defaulter boundary condition
            allPassed &= testDefaulterBoundaryConditions(connection);

        } finally {
            cleanupTestData(connection);
        }

        LOGGER.info("========================================");
        LOGGER.info("Schema Validation Result: " + (allPassed ? "PASSED" : "FAILED"));
        LOGGER.info("========================================");

        return allPassed;
    }

    /**
     * Verifies that all required tables exist in the current database.
     */
    private static boolean testTablesExist(Connection connection) {
        LOGGER.info("Checking required tables...");
        boolean ok = true;
        for (String table : REQUIRED_TABLES) {
            String sql = "SELECT COUNT(*) FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        LOGGER.info("  [OK] Table '" + table + "' exists.");
                    } else {
                        LOGGER.severe("  [FAIL] Table '" + table + "' does NOT exist.");
                        ok = false;
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error checking table '" + table + "': " + e.getMessage());
                ok = false;
            }
        }
        return ok;
    }

    /**
     * Tests inserting a valid student, a valid subject, and both PRESENT and ABSENT attendance.
     */
    private static boolean testValidEntitiesAndAttendance(Connection connection) {
        LOGGER.info("Testing insertion of valid student, subject, and attendance records...");
        String insertStudent = "INSERT INTO students (roll_no, name, branch, semester, section) VALUES (?, ?, ?, ?, ?)";
        String insertSubject = "INSERT INTO subjects (subject_code, subject_name, branch, semester) VALUES (?, ?, ?, ?)";
        String insertAttendance = "INSERT INTO attendance (student_id, subject_id, attendance_date, status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement psStudent = connection.prepareStatement(insertStudent, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psSubject = connection.prepareStatement(insertSubject, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psAttendance = connection.prepareStatement(insertAttendance)) {

            // 1. Insert student
            psStudent.setString(1, TEST_STUDENT_A);
            psStudent.setString(2, "Test Student 75Pct");
            psStudent.setString(3, "CSE");
            psStudent.setInt(4, 4);
            psStudent.setString(5, "A");
            psStudent.executeUpdate();
            int studentId = getGeneratedId(psStudent);
            LOGGER.info("  [OK] Inserted valid student (id=" + studentId + ").");

            // 2. Insert subject
            psSubject.setString(1, TEST_SUBJECT);
            psSubject.setString(2, "Test Verification Subject");
            psSubject.setString(3, "CSE");
            psSubject.setInt(4, 4);
            psSubject.executeUpdate();
            int subjectId = getGeneratedId(psSubject);
            LOGGER.info("  [OK] Inserted valid subject (id=" + subjectId + ").");

            // 3. Insert PRESENT attendance
            psAttendance.setInt(1, studentId);
            psAttendance.setInt(2, subjectId);
            psAttendance.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 1)));
            psAttendance.setString(4, "PRESENT");
            psAttendance.executeUpdate();
            LOGGER.info("  [OK] Inserted PRESENT attendance record.");

            // 4. Insert ABSENT attendance
            psAttendance.setInt(1, studentId);
            psAttendance.setInt(2, subjectId);
            psAttendance.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 2)));
            psAttendance.setString(4, "ABSENT");
            psAttendance.executeUpdate();
            LOGGER.info("  [OK] Inserted ABSENT attendance record.");

            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "  [FAIL] Entity insertion test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies that the unique constraint on (student_id, subject_id, attendance_date)
     * rejects duplicate records.
     */
    private static boolean testDuplicateAttendancePrevention(Connection connection) {
        LOGGER.info("Testing duplicate attendance prevention...");
        String sql = "INSERT INTO attendance (student_id, subject_id, attendance_date, status) "
                + "VALUES (?, ?, ?, ?)";

        int studentId = getStudentIdByRollNo(connection, TEST_STUDENT_A);
        int subjectId = getSubjectIdByCode(connection, TEST_SUBJECT);

        if (studentId == -1 || subjectId == -1) {
            LOGGER.severe("  [FAIL] Test student or subject not found for duplicate test.");
            return false;
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // Attempt duplicate for date 2000-01-01 which already exists
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 1)));
            ps.setString(4, "ABSENT");
            ps.executeUpdate();

            LOGGER.severe("  [FAIL] Duplicate attendance was unexpectedly ACCEPTED!");
            return false;
        } catch (SQLException e) {
            LOGGER.info("  [OK] Duplicate attendance correctly rejected by MySQL (" + e.getClass().getSimpleName() + ").");
            return true;
        }
    }

    /**
     * Verifies foreign key constraints reject non-existent student_id and subject_id.
     */
    private static boolean testForeignKeyConstraints(Connection connection) {
        LOGGER.info("Testing foreign key constraints...");
        boolean ok = true;
        int validSubjectId = getSubjectIdByCode(connection, TEST_SUBJECT);
        int validStudentId = getStudentIdByRollNo(connection, TEST_STUDENT_A);

        String sql = "INSERT INTO attendance (student_id, subject_id, attendance_date, status) VALUES (?, ?, ?, ?)";

        // 1. Invalid student ID
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, 999999);
            ps.setInt(2, validSubjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 10)));
            ps.setString(4, "PRESENT");
            ps.executeUpdate();
            LOGGER.severe("  [FAIL] Invalid student foreign key was ACCEPTED!");
            ok = false;
        } catch (SQLException e) {
            LOGGER.info("  [OK] Invalid student foreign key correctly rejected.");
        }

        // 2. Invalid subject ID
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, validStudentId);
            ps.setInt(2, 999999);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 10)));
            ps.setString(4, "PRESENT");
            ps.executeUpdate();
            LOGGER.severe("  [FAIL] Invalid subject foreign key was ACCEPTED!");
            ok = false;
        } catch (SQLException e) {
            LOGGER.info("  [OK] Invalid subject foreign key correctly rejected.");
        }

        return ok;
    }

    /**
     * Verifies that invalid attendance status values (anything other than PRESENT/ABSENT)
     * are rejected by MySQL constraints.
     */
    private static boolean testInvalidAttendanceStatus(Connection connection) {
        LOGGER.info("Testing invalid attendance status rejection...");
        int studentId = getStudentIdByRollNo(connection, TEST_STUDENT_A);
        int subjectId = getSubjectIdByCode(connection, TEST_SUBJECT);

        String sql = "INSERT INTO attendance (student_id, subject_id, attendance_date, status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 20)));
            ps.setString(4, "INVALID_STATUS");
            ps.executeUpdate();
            LOGGER.severe("  [FAIL] Invalid status was ACCEPTED!");
            return false;
        } catch (SQLException e) {
            LOGGER.info("  [OK] Invalid status correctly rejected by constraint.");
            return true;
        }
    }

    /**
     * Tests attendance percentage calculation and defaulter boundary conditions:
     * <ul>
     *   <li>Student A: 3 PRESENT / 4 total = exactly 75.0% -> NOT a defaulter</li>
     *   <li>Student B: 2 PRESENT / 4 total = 50.0% (below 75.0%) -> IS a defaulter</li>
     * </ul>
     */
    private static boolean testDefaulterBoundaryConditions(Connection connection) {
        LOGGER.info("Testing attendance percentage and 75% defaulter boundary conditions...");

        int studentAId = getStudentIdByRollNo(connection, TEST_STUDENT_A);
        int subjectId = getSubjectIdByCode(connection, TEST_SUBJECT);

        // Student A currently has 2 records: 1 PRESENT (2000-01-01) and 1 ABSENT (2000-01-02).
        // Let's add 2 more PRESENT records so Student A has 3 PRESENT / 4 TOTAL = 75.0% exactly.
        String insertAttendance = "INSERT INTO attendance (student_id, subject_id, attendance_date, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertAttendance)) {
            ps.setInt(1, studentAId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 3)));
            ps.setString(4, "PRESENT");
            ps.executeUpdate();

            ps.setInt(1, studentAId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 4)));
            ps.setString(4, "PRESENT");
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to prepare Student A attendance: " + e.getMessage());
            return false;
        }

        // Insert Student B: will have 2 PRESENT / 4 TOTAL = 50.0%
        String insertStudent = "INSERT INTO students (roll_no, name, branch, semester, section) VALUES (?, ?, ?, ?, ?)";
        int studentBId;
        try (PreparedStatement ps = connection.prepareStatement(insertStudent, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, TEST_STUDENT_B);
            ps.setString(2, "Test Student 50Pct");
            ps.setString(3, "CSE");
            ps.setInt(4, 4);
            ps.setString(5, "A");
            ps.executeUpdate();
            studentBId = getGeneratedId(ps);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert Student B: " + e.getMessage());
            return false;
        }

        try (PreparedStatement ps = connection.prepareStatement(insertAttendance)) {
            ps.setInt(1, studentBId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 1)));
            ps.setString(4, "PRESENT");
            ps.executeUpdate();

            ps.setInt(1, studentBId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 2)));
            ps.setString(4, "PRESENT");
            ps.executeUpdate();

            ps.setInt(1, studentBId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 3)));
            ps.setString(4, "ABSENT");
            ps.executeUpdate();

            ps.setInt(1, studentBId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(LocalDate.of(2000, 1, 4)));
            ps.setString(4, "ABSENT");
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to prepare Student B attendance: " + e.getMessage());
            return false;
        }

        // Now run the dynamic percentage query and verify results
        String query = """
                SELECT
                    s.roll_no,
                    SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                    COUNT(*) AS total_classes,
                    ROUND((SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) * 100.0) / COUNT(*), 2) AS attendance_pct
                FROM attendance a
                JOIN students s ON a.student_id = s.id
                WHERE s.roll_no IN (?, ?)
                GROUP BY s.id, s.roll_no
                ORDER BY attendance_pct ASC
                """;

        boolean studentAVerified = false;
        boolean studentBVerified = false;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, TEST_STUDENT_A);
            ps.setString(2, TEST_STUDENT_B);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rollNo = rs.getString("roll_no");
                    int present = rs.getInt("present_count");
                    int total = rs.getInt("total_classes");
                    double pct = rs.getDouble("attendance_pct");
                    boolean isDefaulter = pct < AppConstants.DEFAULT_DEFAULTER_THRESHOLD;

                    LOGGER.info(String.format("  Calculation: %s => %d/%d (%.2f%%) | Defaulter: %b",
                            rollNo, present, total, pct, isDefaulter));

                    if (TEST_STUDENT_A.equals(rollNo)) {
                        // Expect 75.0% and isDefaulter == false
                        if (Math.abs(pct - 75.0) < 0.01 && !isDefaulter) {
                            LOGGER.info("  [OK] Student A with exactly 75.0% is correctly NOT a defaulter.");
                            studentAVerified = true;
                        } else {
                            LOGGER.severe("  [FAIL] Student A check failed: pct=" + pct + ", defaulter=" + isDefaulter);
                        }
                    } else if (TEST_STUDENT_B.equals(rollNo)) {
                        // Expect 50.0% and isDefaulter == true
                        if (Math.abs(pct - 50.0) < 0.01 && isDefaulter) {
                            LOGGER.info("  [OK] Student B below 75% (50.0%) is correctly flagged as a defaulter.");
                            studentBVerified = true;
                        } else {
                            LOGGER.severe("  [FAIL] Student B check failed: pct=" + pct + ", defaulter=" + isDefaulter);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed executing calculation query: " + e.getMessage());
            return false;
        }

        return studentAVerified && studentBVerified;
    }

    /**
     * Cleans up ONLY test records created during validation.
     * Preserves all real and sample data.
     */
    private static void cleanupTestData(Connection connection) {
        LOGGER.info("Cleaning up validator test records...");
        String deleteAttendance = "DELETE a FROM attendance a JOIN students s ON a.student_id = s.id WHERE s.roll_no LIKE ?";
        String deleteStudents = "DELETE FROM students WHERE roll_no LIKE ?";
        String deleteSubjects = "DELETE FROM subjects WHERE subject_code LIKE ?";

        try (PreparedStatement psAtt = connection.prepareStatement(deleteAttendance);
             PreparedStatement psStu = connection.prepareStatement(deleteStudents);
             PreparedStatement psSub = connection.prepareStatement(deleteSubjects)) {

            psAtt.setString(1, TEST_STUDENT_PREFIX + "%");
            int attCount = psAtt.executeUpdate();

            psStu.setString(1, TEST_STUDENT_PREFIX + "%");
            int stuCount = psStu.executeUpdate();

            psSub.setString(1, TEST_STUDENT_PREFIX + "%");
            int subCount = psSub.executeUpdate();

            LOGGER.info("  [OK] Cleaned up " + attCount + " test attendance, "
                    + stuCount + " test students, " + subCount + " test subjects.");
        } catch (SQLException e) {
            LOGGER.warning("Test cleanup warning: " + e.getMessage());
        }
    }

    private static int getGeneratedId(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve generated key.");
    }

    private static int getStudentIdByRollNo(Connection connection, String rollNo) {
        String sql = "SELECT id FROM students WHERE roll_no = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to find student by roll_no: " + e.getMessage());
        }
        return -1;
    }

    private static int getSubjectIdByCode(Connection connection, String subjectCode) {
        String sql = "SELECT id FROM subjects WHERE subject_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, subjectCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to find subject by code: " + e.getMessage());
        }
        return -1;
    }
}
