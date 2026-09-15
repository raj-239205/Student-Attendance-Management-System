package com.college.attendance.dao;

import com.college.attendance.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for user account operations.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Query user identity and credentials by username using
 * {@link PreparedStatement}.</li>
 * <li>Insert new user accounts with pre-hashed passwords.</li>
 * <li>Safe resource management with try-with-resources.</li>
 * </ul>
 * </p>
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    public UserDAO() {
    }

    /**
     * Finds a user account by their unique username.
     *
     * @param connection an active JDBC connection
     * @param username   the username to look up
     * @return an {@link Optional} containing the user if found, or empty if no
     *         match
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> findByUsername(Connection connection, String username) throws SQLException {
        if (connection == null || username == null || username.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT id, username, password, full_name, role, created_at, updated_at "
                + "FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setFullName(rs.getString("full_name"));
                    user.setRole(User.Role.valueOf(rs.getString("role")));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        user.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        user.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserts a new user into the {@code users} table.
     * The password field MUST already be hashed using BCrypt.
     *
     * @param connection an active JDBC connection
     * @param user       the user entity with hashed password
     * @return the generated primary key ID, or -1 on failure
     * @throws SQLException if an integrity constraint violation or SQL error occurs
     */
    public int createUser(Connection connection, User user) throws SQLException {
        if (connection == null || user == null) {
            throw new IllegalArgumentException("Connection and User cannot be null.");
        }

        String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername().trim());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getFullName().trim());
            stmt.setString(4, user.getRole().name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    user.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the total number of registered users.
     *
     * @param connection an active JDBC connection
     * @return count of rows in the {@code users} table
     */
    public int countUsers(Connection connection) {
        if (connection == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error counting users: " + e.getMessage());
        }
        return 0;
    }
}
