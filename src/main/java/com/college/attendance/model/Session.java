package com.college.attendance.model;

/**
 * Lightweight, thread-safe session container for the currently authenticated
 * user.
 *
 * <p>
 * Holds only non-sensitive identity attributes: ID, username, full name, and
 * role.
 * Passwords and hashes are strictly excluded.
 * </p>
 */
public final class Session {

    private static volatile Session instance;

    private final int userId;
    private final String username;
    private final String fullName;
    private final User.Role role;

    private Session(int userId, String username, String fullName, User.Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    /**
     * Initializes the global session upon successful login.
     *
     * @param user the authenticated {@link User} entity
     * @return the new {@link Session} instance
     */
    public static synchronized Session start(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null when starting a session.");
        }
        instance = new Session(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
        return instance;
    }

    /**
     * Retrieves the current active session.
     *
     * @return the current {@link Session}, or {@code null} if no user is
     *         authenticated
     */
    public static Session getCurrent() {
        return instance;
    }

    /**
     * Destroys and clears the current user session (logout).
     */
    public static synchronized void destroy() {
        instance = null;
    }

    /**
     * Checks whether an active authenticated session exists.
     *
     * @return {@code true} if authenticated, {@code false} otherwise
     */
    public static boolean isAuthenticated() {
        return instance != null;
    }

    // --- Instance Getters ---

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public User.Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "Session{"
                + "userId=" + userId
                + ", username='" + username + '\''
                + ", fullName='" + fullName + '\''
                + ", role=" + role
                + '}';
    }
}
