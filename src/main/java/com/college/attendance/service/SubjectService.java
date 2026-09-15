package com.college.attendance.service;

import com.college.attendance.dao.SubjectDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Subject;
import com.college.attendance.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing course catalog and curriculum operations, validation,
 * and role-based permissions enforcement.
 */
public class SubjectService {

    private static final Logger LOGGER = Logger.getLogger(SubjectService.class.getName());

    private final DatabaseConnection dbConnection;
    private final SubjectDAO subjectDAO;

    public SubjectService(DatabaseConnection dbConnection) {
        this(dbConnection, new SubjectDAO());
    }

    public SubjectService(DatabaseConnection dbConnection, SubjectDAO subjectDAO) {
        this.dbConnection = dbConnection;
        this.subjectDAO = subjectDAO;
    }

    /**
     * Standard outcome wrapper for business layer operations.
     */
    public static final class ServiceResult<T> {
        private final boolean success;
        private final String message;
        private final T data;

        private ServiceResult(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ServiceResult<T> success(String message, T data) {
            return new ServiceResult<>(true, message, data);
        }

        public static <T> ServiceResult<T> success(T data) {
            return new ServiceResult<>(true, "Operation successful", data);
        }

        public static <T> ServiceResult<T> failure(String message) {
            return new ServiceResult<>(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }

    /**
     * Retrieves all subjects. Allowed for ADMIN and FACULTY.
     */
    public List<Subject> getAllSubjects(User.Role callerRole) {
        if (!canViewSubjects(callerRole)) {
            LOGGER.warning("Unauthorized access to getAllSubjects by role: " + callerRole);
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return subjectDAO.getAllSubjects(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve subjects: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches subjects by keyword. Allowed for ADMIN and FACULTY.
     */
    public List<Subject> searchSubjects(String keyword, User.Role callerRole) {
        if (!canViewSubjects(callerRole)) {
            LOGGER.warning("Unauthorized access to searchSubjects by role: " + callerRole);
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return subjectDAO.searchSubjects(conn, keyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to search subjects: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single subject by ID. Allowed for ADMIN and FACULTY.
     */
    public Subject getSubjectById(int subjectId, User.Role callerRole) {
        if (!canViewSubjects(callerRole)) {
            LOGGER.warning("Unauthorized access to getSubjectById by role: " + callerRole);
            return null;
        }
        if (subjectId <= 0) {
            return null;
        }

        try (Connection conn = dbConnection.getConnection()) {
            return subjectDAO.getSubjectById(conn, subjectId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch subject by ID: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new subject. Restricted strictly to ADMIN.
     */
    public ServiceResult<Subject> createSubject(Subject subject, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can create new course subjects.");
        }

        String validationError = validateSubject(subject);
        if (validationError != null) {
            return ServiceResult.failure(validationError);
        }

        try (Connection conn = dbConnection.getConnection()) {
            // Check subject code uniqueness
            if (subjectDAO.subjectCodeExists(conn, subject.getSubjectCode().trim(), null)) {
                return ServiceResult.failure("A course with Code '" + subject.getSubjectCode().trim() + "' already exists.");
            }

            int generatedId = subjectDAO.createSubject(conn, subject);
            if (generatedId > 0) {
                subject.setId(generatedId);
                LOGGER.info("Subject successfully created with ID: " + generatedId);
                return ServiceResult.success("Course subject created successfully.", subject);
            } else {
                return ServiceResult.failure("Failed to save subject record to database.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during subject creation: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not complete course subject creation.");
        }
    }

    /**
     * Updates an existing subject. Restricted strictly to ADMIN.
     */
    public ServiceResult<Subject> updateSubject(Subject subject, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can modify course subjects.");
        }
        if (subject == null || subject.getId() <= 0) {
            return ServiceResult.failure("Invalid subject ID for update operation.");
        }

        String validationError = validateSubject(subject);
        if (validationError != null) {
            return ServiceResult.failure(validationError);
        }

        try (Connection conn = dbConnection.getConnection()) {
            Subject existing = subjectDAO.getSubjectById(conn, subject.getId());
            if (existing == null) {
                return ServiceResult.failure("Subject record not found.");
            }

            // Check subject code uniqueness excluding current subject
            if (subjectDAO.subjectCodeExists(conn, subject.getSubjectCode().trim(), subject.getId())) {
                return ServiceResult.failure("Another course with Code '" + subject.getSubjectCode().trim() + "' already exists.");
            }

            boolean updated = subjectDAO.updateSubject(conn, subject);
            if (updated) {
                LOGGER.info("Subject successfully updated for ID: " + subject.getId());
                return ServiceResult.success("Course subject updated successfully.", subject);
            } else {
                return ServiceResult.failure("Failed to update course subject in database.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during subject update: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not complete course subject update.");
        }
    }

    /**
     * Deletes a subject. Restricted strictly to ADMIN.
     * Enforces foreign-key safety against referenced attendance records.
     */
    public ServiceResult<Void> deleteSubject(int subjectId, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can delete course subjects.");
        }
        if (subjectId <= 0) {
            return ServiceResult.failure("Invalid subject ID for deletion.");
        }

        try (Connection conn = dbConnection.getConnection()) {
            Subject existing = subjectDAO.getSubjectById(conn, subjectId);
            if (existing == null) {
                return ServiceResult.failure("Course subject not found.");
            }

            // Check if referenced in attendance table
            if (subjectDAO.hasAttendanceRecords(conn, subjectId)) {
                return ServiceResult.failure("Cannot delete course '" + existing.getSubjectName() +
                        "': Attendance records reference this subject. Clear related attendance records first.");
            }

            boolean deleted = subjectDAO.deleteSubject(conn, subjectId);
            if (deleted) {
                LOGGER.info("Subject successfully deleted for ID: " + subjectId);
                return ServiceResult.success("Course subject deleted successfully.", null);
            } else {
                return ServiceResult.failure("Failed to delete course subject from database.");
            }
        } catch (SQLException e) {
            // Catch MySQL Foreign Key Constraint failure (error 1451 / SQLState 23000)
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1451) {
                return ServiceResult.failure("Cannot delete course: Attendance records reference this course.");
            }
            LOGGER.log(Level.SEVERE, "Database error during subject deletion: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not delete course subject.");
        }
    }

    private boolean canViewSubjects(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.FACULTY;
    }

    /**
     * Validates field integrity of subject entities based on schema constraints.
     */
    public String validateSubject(Subject subject) {
        if (subject == null) {
            return "Course subject data cannot be null.";
        }

        // Subject Code
        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            return "Course / Subject code is required.";
        }
        if (subject.getSubjectCode().trim().length() > 20) {
            return "Subject code cannot exceed 20 characters.";
        }

        // Subject Name
        if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return "Subject name is required.";
        }
        if (subject.getSubjectName().trim().length() > 100) {
            return "Subject name cannot exceed 100 characters.";
        }

        // Branch
        if (subject.getBranch() == null || subject.getBranch().trim().isEmpty()) {
            return "Branch / Department is required.";
        }
        if (subject.getBranch().trim().length() > 50) {
            return "Branch cannot exceed 50 characters.";
        }

        // Semester
        if (subject.getSemester() <= 0) {
            return "Semester must be a positive integer (e.g. 1 to 8).";
        }
        if (subject.getSemester() > 12) {
            return "Semester exceeds maximum academic threshold (12).";
        }

        return null;
    }
}
