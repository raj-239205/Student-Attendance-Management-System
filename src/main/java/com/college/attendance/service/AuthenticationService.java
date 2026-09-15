package com.college.attendance.service;

import com.college.attendance.dao.UserDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Session;
import com.college.attendance.model.User;
import com.college.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating authentication workflows for ADMIN, FACULTY, and STUDENT users.
 *
 * <p>
 * Decouples the UI layer from database access and cryptographic hashing.
 * Enforces secure credentials handling and identity validation:
 * <ul>
 * <li>Generic error messaging for failed attempts to prevent user enumeration.</li>
 * <li>Ensures STUDENT accounts reference an existing, valid student record.</li>
 * <li>Ensures ADMIN and FACULTY accounts have no student_id link.</li>
 * <li>Clears raw password character buffers immediately after verification.</li>
 * <li>Graceful handling of database unavailability.</li>
 * </ul>
 * </p>
 */
public class AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());

    private static final String GENERIC_AUTH_ERROR = "Invalid username or password.";
    private static final String EMPTY_USERNAME_ERROR = "Please enter your username.";
    private static final String EMPTY_PASSWORD_ERROR = "Please enter your password.";
    private static final String DB_UNAVAILABLE_ERROR = "Database service unavailable. Please check connection.";

    private final DatabaseConnection dbConnection;
    private final UserDAO userDAO;

    public AuthenticationService(DatabaseConnection dbConnection) {
        this(dbConnection, new UserDAO());
    }

    public AuthenticationService(DatabaseConnection dbConnection, UserDAO userDAO) {
        this.dbConnection = dbConnection;
        this.userDAO = userDAO;
    }

    /**
     * Result container representing authentication outcome.
     */
    public static final class AuthResult {
        private final boolean success;
        private final String errorMessage;
        private final Session session;

        private AuthResult(boolean success, String errorMessage, Session session) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.session = session;
        }

        public static AuthResult success(Session session) {
            return new AuthResult(true, null, session);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Session getSession() {
            return session;
        }
    }

    /**
     * Authenticates a user given their username and password character array.
     * The password array is wiped with zeroes upon completion.
     *
     * @param username the username input
     * @param password the password input as char[]
     * @return an {@link AuthResult} indicating success or failure
     */
    public AuthResult login(String username, char[] password) {
        // 1. Input validation
        if (username == null || username.trim().isEmpty()) {
            return AuthResult.failure(EMPTY_USERNAME_ERROR);
        }
        if (password == null || password.length == 0) {
            return AuthResult.failure(EMPTY_PASSWORD_ERROR);
        }

        String trimmedUser = username.trim();
        String candidatePassword = new String(password);

        try {
            // 2. Obtain database connection
            try (Connection conn = dbConnection.getConnection()) {
                // 3. Query user via DAO
                Optional<User> userOpt = userDAO.findByUsername(conn, trimmedUser);

                if (userOpt.isEmpty()) {
                    LOGGER.warning("Authentication failed: User not found (generic error returned).");
                    return AuthResult.failure(GENERIC_AUTH_ERROR);
                }

                User user = userOpt.get();

                // 4. Verify password hash using BCrypt
                boolean matched = PasswordUtil.verifyPassword(candidatePassword, user.getPassword());
                if (!matched) {
                    LOGGER.warning("Authentication failed: Password mismatch for user '" + trimmedUser + "'.");
                    return AuthResult.failure(GENERIC_AUTH_ERROR);
                }

                // 5. Role-specific student linkage verification
                if (user.getRole() == User.Role.STUDENT) {
                    if (user.getStudentId() == null) {
                        LOGGER.warning("Authentication failed: STUDENT account '" + trimmedUser + "' has no linked student_id.");
                        return AuthResult.failure(GENERIC_AUTH_ERROR);
                    }
                    if (!userDAO.studentRecordExists(conn, user.getStudentId())) {
                        LOGGER.warning("Authentication failed: STUDENT account '" + trimmedUser + "' references non-existent student ID " + user.getStudentId());
                        return AuthResult.failure(GENERIC_AUTH_ERROR);
                    }
                } else {
                    // ADMIN and FACULTY must NOT be linked to a student
                    if (user.getStudentId() != null) {
                        LOGGER.warning("Authentication failed: Non-student role account '" + trimmedUser + "' unexpectedly linked to student_id.");
                        return AuthResult.failure(GENERIC_AUTH_ERROR);
                    }
                }

                // 6. Initialize session
                Session session = Session.start(user);
                LOGGER.info("Authentication successful: " + user.getUsername() + " [" + user.getRole()
                        + (user.getStudentId() != null ? ", studentId=" + user.getStudentId() : "") + "].");
                return AuthResult.success(session);

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during authentication: " + e.getMessage());
                return AuthResult.failure(DB_UNAVAILABLE_ERROR);
            }
        } finally {
            // Securely wipe password array from memory
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Logs out the current user by destroying the active session.
     */
    public void logout() {
        Session current = Session.getCurrent();
        if (current != null) {
            LOGGER.info("User logged out: " + current.getUsername());
        }
        Session.destroy();
    }
}
