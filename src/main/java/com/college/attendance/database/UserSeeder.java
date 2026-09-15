package com.college.attendance.database;

import com.college.attendance.dao.UserDAO;
import com.college.attendance.model.User;
import com.college.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for seeding initial development/test user accounts (ADMIN, FACULTY, STUDENT).
 *
 * <p>
 * Creates initial accounts with salted BCrypt password hashes only if they
 * do not already exist. Existing accounts are never modified or overwritten.
 * </p>
 */
public final class UserSeeder {

    private static final Logger LOGGER = Logger.getLogger(UserSeeder.class.getName());

    private UserSeeder() {
        // prevent instantiation
    }

    /**
     * Seeds default admin, faculty, and student test accounts if they are not already present.
     *
     * @param connection      an active JDBC connection
     * @param initialPassword the plain-text password to hash with BCrypt for initial setup
     * @return number of new users seeded
     */
    public static int seedDefaultUsers(Connection connection, String initialPassword) {
        if (connection == null || initialPassword == null || initialPassword.isBlank()) {
            return 0;
        }

        UserDAO userDAO = new UserDAO();
        int createdCount = 0;

        try {
            // 1. Check & seed Admin user
            Optional<User> adminOpt = userDAO.findByUsername(connection, "admin");
            if (adminOpt.isEmpty()) {
                String adminHash = PasswordUtil.hashPassword(initialPassword);
                User admin = new User("admin", adminHash, "System Administrator", User.Role.ADMIN, null);
                userDAO.createUser(connection, admin);
                LOGGER.info("Seeded initial account: admin [ADMIN]");
                createdCount++;
            } else {
                LOGGER.fine("User 'admin' already exists — seeding skipped.");
            }

            // 2. Check & seed Faculty user
            Optional<User> facultyOpt = userDAO.findByUsername(connection, "faculty");
            if (facultyOpt.isEmpty()) {
                String facultyHash = PasswordUtil.hashPassword(initialPassword);
                User faculty = new User("faculty", facultyHash, "Prof. Rajesh Sharma", User.Role.FACULTY, null);
                userDAO.createUser(connection, faculty);
                LOGGER.info("Seeded initial account: faculty [FACULTY]");
                createdCount++;
            } else {
                LOGGER.fine("User 'faculty' already exists — seeding skipped.");
            }

            // 3. Check & seed Student user (linked to first existing student in students table)
            Optional<User> studentOpt = userDAO.findByUsername(connection, "student");
            if (studentOpt.isEmpty()) {
                // Find existing student record (e.g. id=1 or first available)
                Integer targetStudentId = null;
                String studentName = "Aarav Sharma";

                String findStudentSql = "SELECT id, name FROM students ORDER BY id ASC LIMIT 1";
                try (PreparedStatement ps = connection.prepareStatement(findStudentSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        targetStudentId = rs.getInt("id");
                        studentName = rs.getString("name");
                    }
                }

                if (targetStudentId != null) {
                    String studentHash = PasswordUtil.hashPassword(initialPassword);
                    User studentUser = new User("student", studentHash, studentName, User.Role.STUDENT, targetStudentId);
                    userDAO.createUser(connection, studentUser);
                    LOGGER.info("Seeded initial account: student [STUDENT] -> linked to student_id=" + targetStudentId);
                    createdCount++;
                } else {
                    LOGGER.warning("Could not seed 'student' account: no records found in 'students' table.");
                }
            } else {
                LOGGER.fine("User 'student' already exists — seeding skipped.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed seeding default test users: " + e.getMessage());
        }

        return createdCount;
    }
}
