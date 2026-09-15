package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.UserDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.database.UserSeeder;
import com.college.attendance.model.Session;
import com.college.attendance.model.User;
import com.college.attendance.service.AuthenticationService;
import com.college.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Automated test suite covering Stage 3.1 authentication and student linkage:
 * <ol>
 *   <li>ADMIN login succeeds</li>
 *   <li>FACULTY login succeeds</li>
 *   <li>STUDENT login succeeds</li>
 *   <li>ADMIN studentId is NULL</li>
 *   <li>FACULTY studentId is NULL</li>
 *   <li>STUDENT studentId is populated</li>
 *   <li>STUDENT references a valid students.id</li>
 *   <li>STUDENT with null/invalid student_id cannot authenticate</li>
 *   <li>Existing ADMIN still works</li>
 *   <li>Existing FACULTY still works</li>
 *   <li>Logout clears studentId from session</li>
 *   <li>Student password is BCrypt verified</li>
 *   <li>Wrong student password fails</li>
 *   <li>Invalid username returns generic error</li>
 *   <li>Student cannot specify arbitrary student_id (foreign key + DAO isolation)</li>
 * </ol>
 */
public class AuthenticationTest {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationTest.class.getName());
    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Stage 3.1 — Automated Authentication & Linkage Suite");
        System.out.println("==================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);

        String testPass = config.getPassword();
        if (testPass == null || testPass.isBlank()) {
            throw new IllegalStateException("Database password not configured for test suite.");
        }

        // 1. Ensure default accounts (admin, faculty, student) exist in DB
        try (Connection conn = dbConnection.getConnection()) {
            UserSeeder.seedDefaultUsers(conn, testPass);
        } catch (Exception e) {
            System.err.println("Warning: Could not seed test users for suite: " + e.getMessage());
        }

        AuthenticationService authService = new AuthenticationService(dbConnection);
        UserDAO userDAO = new UserDAO();
        String validPassword = testPass;

        // Test 1: ADMIN login succeeds
        test("1. ADMIN login succeeds", () -> {
            AuthenticationService.AuthResult result = authService.login("admin", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Admin login must succeed");
            assertCondition(result.getSession() != null, "Admin session must be present");
        });

        // Test 2: FACULTY login succeeds
        test("2. FACULTY login succeeds", () -> {
            AuthenticationService.AuthResult result = authService.login("faculty", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Faculty login must succeed");
            assertCondition(result.getSession() != null, "Faculty session must be present");
        });

        // Test 3: STUDENT login succeeds
        test("3. STUDENT login succeeds", () -> {
            AuthenticationService.AuthResult result = authService.login("student", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Student login must succeed");
            assertCondition(result.getSession() != null, "Student session must be present");
        });

        // Test 4: ADMIN studentId is NULL
        test("4. ADMIN studentId is NULL", () -> {
            AuthenticationService.AuthResult result = authService.login("admin", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Admin login required");
            assertCondition(result.getSession().getRole() == User.Role.ADMIN, "Role must be ADMIN");
            assertCondition(result.getSession().getStudentId() == null, "Admin studentId must be null");
        });

        // Test 5: FACULTY studentId is NULL
        test("5. FACULTY studentId is NULL", () -> {
            AuthenticationService.AuthResult result = authService.login("faculty", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Faculty login required");
            assertCondition(result.getSession().getRole() == User.Role.FACULTY, "Role must be FACULTY");
            assertCondition(result.getSession().getStudentId() == null, "Faculty studentId must be null");
        });

        // Test 6: STUDENT studentId is populated
        test("6. STUDENT studentId is populated", () -> {
            AuthenticationService.AuthResult result = authService.login("student", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Student login required");
            assertCondition(result.getSession().getRole() == User.Role.STUDENT, "Role must be STUDENT");
            assertCondition(result.getSession().getStudentId() != null, "Student studentId must not be null");
            assertCondition(result.getSession().getStudentId() > 0, "Student studentId must be a positive integer");
        });

        // Test 7: STUDENT references a valid students.id
        test("7. STUDENT references a valid students.id in students table", () -> {
            AuthenticationService.AuthResult result = authService.login("student", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Student login required");
            int studentId = result.getSession().getStudentId();

            try (Connection conn = dbConnection.getConnection()) {
                boolean exists = userDAO.studentRecordExists(conn, studentId);
                assertCondition(exists, "Referenced student ID " + studentId + " must exist in students table");
            }
        });

        // Test 8: STUDENT with invalid / null student_id cannot authenticate
        test("8. STUDENT with null or nonexistent student_id cannot authenticate", () -> {
            try (Connection conn = dbConnection.getConnection()) {
                // Test 8a: Foreign key prevents pointing to non-existent student_id=999999
                String badFkSql = "INSERT INTO users (username, password, full_name, role, student_id) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(badFkSql)) {
                    ps.setString(1, "__TEST_BAD_FK_USER__");
                    ps.setString(2, PasswordUtil.hashPassword(testPass));
                    ps.setString(3, "Bad FK Student");
                    ps.setString(4, "STUDENT");
                    ps.setInt(5, 999999);
                    ps.executeUpdate();
                    assertCondition(false, "Foreign key must reject student_id=999999");
                } catch (SQLException expectedFkEx) {
                    // Correctly rejected by fk_users_student
                }

                // Test 8b: Service layer rejects STUDENT if student_id is null
                // Clean up any old test record first
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = '__TEST_NULL_STU__'")) {
                    ps.executeUpdate();
                }

                String nullStuSql = "INSERT INTO users (username, password, full_name, role, student_id) VALUES (?, ?, ?, ?, NULL)";
                try (PreparedStatement ps = conn.prepareStatement(nullStuSql)) {
                    ps.setString(1, "__TEST_NULL_STU__");
                    ps.setString(2, PasswordUtil.hashPassword(testPass));
                    ps.setString(3, "Null Student");
                    ps.setString(4, "STUDENT");
                    ps.executeUpdate();
                }

                AuthenticationService.AuthResult result = authService.login("__TEST_NULL_STU__", testPass.toCharArray());
                assertCondition(!result.isSuccess(), "STUDENT user with null student_id must be rejected");

                // Clean up
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = '__TEST_NULL_STU__'")) {
                    ps.executeUpdate();
                }
            }
        });

        // Test 9: Existing ADMIN still works
        test("9. Existing ADMIN still works after migration", () -> {
            AuthenticationService.AuthResult result = authService.login("admin", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Admin must authenticate");
            assertCondition("admin".equals(result.getSession().getUsername()), "Username must be admin");
        });

        // Test 10: Existing FACULTY still works
        test("10. Existing FACULTY still works after migration", () -> {
            AuthenticationService.AuthResult result = authService.login("faculty", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Faculty must authenticate");
            assertCondition("faculty".equals(result.getSession().getUsername()), "Username must be faculty");
        });

        // Test 11: Logout clears studentId from session
        test("11. Logout clears studentId and session completely", () -> {
            authService.login("student", validPassword.toCharArray());
            assertCondition(Session.isAuthenticated(), "Must be authenticated before logout");
            assertCondition(Session.getCurrent().getStudentId() != null, "Student ID must be present in session");

            authService.logout();
            assertCondition(!Session.isAuthenticated(), "Must not be authenticated after logout");
            assertCondition(Session.getCurrent() == null, "Session must be null after logout");
        });

        // Test 12: Student password is BCrypt verified
        test("12. Student password is stored as secure BCrypt hash", () -> {
            try (Connection conn = dbConnection.getConnection()) {
                Optional<User> studentOpt = userDAO.findByUsername(conn, "student");
                assertCondition(studentOpt.isPresent(), "Student user record must exist");
                String hash = studentOpt.get().getPassword();
                assertCondition(hash != null && hash.startsWith("$2a$") && hash.length() == 60,
                        "Password must be standard 60-char BCrypt hash");
                assertCondition(PasswordUtil.verifyPassword(validPassword, hash),
                        "BCrypt verification must succeed with correct password");
            }
        });

        // Test 13: Wrong student password fails
        test("13. Wrong student password fails with generic error", () -> {
            AuthenticationService.AuthResult result = authService.login("student", "IncorrectPassword#999".toCharArray());
            assertCondition(!result.isSuccess(), "Wrong password must fail");
            assertCondition("Invalid username or password.".equals(result.getErrorMessage()),
                    "Generic error expected");
        });

        // Test 14: Invalid username returns generic error
        test("14. Invalid username returns generic error", () -> {
            AuthenticationService.AuthResult result = authService.login("__NON_EXISTENT_USER__", validPassword.toCharArray());
            assertCondition(!result.isSuccess(), "Unknown user must fail");
            assertCondition("Invalid username or password.".equals(result.getErrorMessage()),
                    "Generic error expected");
        });

        // Test 15: Student cannot create arbitrary student_id through authentication
        test("15. Student cannot forge arbitrary student_id (enforced by DB relationship and Session)", () -> {
            // The login interface accepts ONLY credentials (username, password)
            // studentId is populated strictly from the authoritative database record
            AuthenticationService.AuthResult result = authService.login("student", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Student login required");

            try (Connection conn = dbConnection.getConnection()) {
                Optional<User> dbUser = userDAO.findByUsername(conn, "student");
                assertCondition(dbUser.isPresent(), "Database record must exist");
                assertCondition(result.getSession().getStudentId().equals(dbUser.get().getStudentId()),
                        "Session studentId must strictly match database studentId without client override capability");
            }
        });

        System.out.println("==================================================");
        System.out.printf("Test Suite Finished: %d PASSED, %d FAILED%n", passedCount, failedCount);
        System.out.println("==================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void test(String name, TestRunnable runnable) {
        try {
            runnable.run();
            System.out.println("  [PASS] " + name);
            passedCount++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] " + name + " -> " + t.getMessage());
            failedCount++;
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface TestRunnable {
        void run() throws Throwable;
    }
}
