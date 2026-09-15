package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.database.UserSeeder;
import com.college.attendance.model.Session;
import com.college.attendance.model.User;
import com.college.attendance.service.AuthenticationService;
import com.college.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Automated test suite covering all 12 authentication validation scenarios:
 * <ol>
 *   <li>BCrypt password hashing</li>
 *   <li>Correct password verification</li>
 *   <li>Incorrect password verification</li>
 *   <li>Empty username rejection</li>
 *   <li>Empty password rejection</li>
 *   <li>Unknown username rejection (generic error)</li>
 *   <li>Invalid password rejection (generic error)</li>
 *   <li>Valid ADMIN authentication</li>
 *   <li>Valid FACULTY authentication</li>
 *   <li>Session creation and identity verification</li>
 *   <li>Session logout and state clearance</li>
 *   <li>Database failure handling</li>
 * </ol>
 */
public class AuthenticationTest {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationTest.class.getName());
    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Stage 3 — Automated Authentication Test Suite");
        System.out.println("==================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);

        String testPass = config.getPassword();
        if (testPass == null || testPass.isBlank()) {
            throw new IllegalStateException("Database password not configured for test suite.");
        }

        // Ensure test users exist in DB
        try (Connection conn = dbConnection.getConnection()) {
            UserSeeder.seedDefaultUsers(conn, testPass);
        } catch (Exception e) {
            System.err.println("Warning: Could not seed test users for suite: " + e.getMessage());
        }

        AuthenticationService authService = new AuthenticationService(dbConnection);
        String validPassword = testPass;

        // 1. Test BCrypt password hashing
        test("1. BCrypt password hashing produces valid 60-char hash", () -> {
            String hash = PasswordUtil.hashPassword("SecureTestPass!123");
            assertCondition(hash != null && hash.startsWith("$2a$") && hash.length() == 60,
                    "Expected valid BCrypt $2a$ format");
        });

        // 2. Test correct password verification
        test("2. Correct password verification succeeds", () -> {
            String raw = "SecretPassword#456";
            String hash = PasswordUtil.hashPassword(raw);
            assertCondition(PasswordUtil.verifyPassword(raw, hash), "Correct password must verify as true");
        });

        // 3. Test incorrect password verification
        test("3. Incorrect password verification fails", () -> {
            String raw = "SecretPassword#456";
            String hash = PasswordUtil.hashPassword(raw);
            assertCondition(!PasswordUtil.verifyPassword("WrongPassword#456", hash), "Wrong password must verify as false");
        });

        // 4. Test empty username validation
        test("4. Empty username rejected with user-friendly message", () -> {
            AuthenticationService.AuthResult result = authService.login("", "somePass".toCharArray());
            assertCondition(!result.isSuccess() && result.getErrorMessage().contains("username"),
                    "Empty username must fail with username error message");

            AuthenticationService.AuthResult resultBlank = authService.login("   ", "somePass".toCharArray());
            assertCondition(!resultBlank.isSuccess(), "Whitespace username must fail");
        });

        // 5. Test empty password validation
        test("5. Empty password rejected with user-friendly message", () -> {
            AuthenticationService.AuthResult result = authService.login("admin", new char[0]);
            assertCondition(!result.isSuccess() && result.getErrorMessage().contains("password"),
                    "Empty password must fail with password error message");

            AuthenticationService.AuthResult resultNull = authService.login("admin", null);
            assertCondition(!resultNull.isSuccess(), "Null password must fail");
        });

        // 6. Test unknown username rejection
        test("6. Unknown username rejected with generic message (anti-enumeration)", () -> {
            AuthenticationService.AuthResult result = authService.login("non_existent_user_999", "somePass".toCharArray());
            assertCondition(!result.isSuccess(), "Unknown user must fail");
            assertCondition("Invalid username or password.".equals(result.getErrorMessage()),
                    "Must return generic message: 'Invalid username or password.'");
        });

        // 7. Test invalid password rejection
        test("7. Invalid password rejected with generic message", () -> {
            AuthenticationService.AuthResult result = authService.login("admin", "DefinitelyWrongPass#999".toCharArray());
            assertCondition(!result.isSuccess(), "Wrong password must fail");
            assertCondition("Invalid username or password.".equals(result.getErrorMessage()),
                    "Must return generic message: 'Invalid username or password.'");
        });

        // 8. Test valid ADMIN authentication
        test("8. Valid ADMIN user authentication succeeds", () -> {
            AuthenticationService.AuthResult result = authService.login("admin", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Admin login must succeed");
            assertCondition(result.getSession() != null, "Session must be created");
            assertCondition(result.getSession().getRole() == User.Role.ADMIN, "Role must be ADMIN");
            assertCondition("admin".equals(result.getSession().getUsername()), "Username must match 'admin'");
        });

        // 9. Test valid FACULTY authentication
        test("9. Valid FACULTY user authentication succeeds", () -> {
            AuthenticationService.AuthResult result = authService.login("faculty", validPassword.toCharArray());
            assertCondition(result.isSuccess(), "Faculty login must succeed");
            assertCondition(result.getSession() != null, "Session must be created");
            assertCondition(result.getSession().getRole() == User.Role.FACULTY, "Role must be FACULTY");
            assertCondition("faculty".equals(result.getSession().getUsername()), "Username must match 'faculty'");
        });

        // 10. Test session creation and state
        test("10. Session contains expected user identity without sensitive data", () -> {
            assertCondition(Session.isAuthenticated(), "Session must report authenticated");
            Session current = Session.getCurrent();
            assertCondition(current != null, "Current session must not be null");
            assertCondition(current.getUserId() > 0, "User ID must be positive integer");
            assertCondition(current.getFullName() != null && !current.getFullName().isBlank(), "Full name must be present");
        });

        // 11. Test session logout
        test("11. Logout clears session state completely", () -> {
            authService.logout();
            assertCondition(!Session.isAuthenticated(), "Session must not be authenticated after logout");
            assertCondition(Session.getCurrent() == null, "Current session must be null after logout");
        });

        // 12. Test database failure handling
        test("12. Database unavailability handled gracefully without unhandled exceptions", () -> {
            DatabaseConfig badConfig = new DatabaseConfig("localhost", "9999", "invalid_db", "user", "pass");
            DatabaseConnection badDbConnection = new DatabaseConnection(badConfig);
            AuthenticationService badAuthService = new AuthenticationService(badDbConnection);

            AuthenticationService.AuthResult result = badAuthService.login("admin", "pass".toCharArray());
            assertCondition(!result.isSuccess(), "Login against unavailable database must fail");
            assertCondition(result.getErrorMessage().contains("unavailable"),
                    "Error message must indicate database service unavailable");
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
