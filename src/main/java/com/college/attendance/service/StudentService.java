package com.college.attendance.service;

import com.college.attendance.dao.StudentDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Student;
import com.college.attendance.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating student management business logic, validation,
 * and role-based permissions enforcement.
 */
public class StudentService {

    private static final Logger LOGGER = Logger.getLogger(StudentService.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final DatabaseConnection dbConnection;
    private final StudentDAO studentDAO;

    public StudentService(DatabaseConnection dbConnection) {
        this(dbConnection, new StudentDAO());
    }

    public StudentService(DatabaseConnection dbConnection, StudentDAO studentDAO) {
        this.dbConnection = dbConnection;
        this.studentDAO = studentDAO;
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
     * Retrieves all enrolled students. Allowed for ADMIN and FACULTY.
     */
    public List<Student> getAllStudents(User.Role callerRole) {
        if (!canViewStudents(callerRole)) {
            LOGGER.warning("Unauthorized access to getAllStudents by role: " + callerRole);
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return studentDAO.getAllStudents(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve students: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches students by keyword. Allowed for ADMIN and FACULTY.
     */
    public List<Student> searchStudents(String keyword, User.Role callerRole) {
        if (!canViewStudents(callerRole)) {
            LOGGER.warning("Unauthorized access to searchStudents by role: " + callerRole);
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return studentDAO.searchStudents(conn, keyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to search students: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single student by primary key ID. Allowed for ADMIN and FACULTY.
     */
    public Student getStudentById(int studentId, User.Role callerRole) {
        if (!canViewStudents(callerRole)) {
            LOGGER.warning("Unauthorized access to getStudentById by role: " + callerRole);
            return null;
        }
        if (studentId <= 0) {
            return null;
        }

        try (Connection conn = dbConnection.getConnection()) {
            return studentDAO.getStudentById(conn, studentId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch student by ID: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Enrolls a new student. Restricted to ADMIN only.
     */
    public ServiceResult<Student> createStudent(Student student, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can create new student records.");
        }

        String validationError = validateStudent(student);
        if (validationError != null) {
            return ServiceResult.failure(validationError);
        }

        try (Connection conn = dbConnection.getConnection()) {
            // Check roll number uniqueness
            if (studentDAO.rollNumberExists(conn, student.getRollNo().trim(), null)) {
                return ServiceResult.failure("A student with Roll Number '" + student.getRollNo().trim() + "' already exists.");
            }

            // Check email uniqueness if email provided
            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                if (studentDAO.emailExists(conn, student.getEmail().trim(), null)) {
                    return ServiceResult.failure("A student with Email '" + student.getEmail().trim() + "' already exists.");
                }
            }

            int generatedId = studentDAO.createStudent(conn, student);
            if (generatedId > 0) {
                student.setId(generatedId);
                LOGGER.info("Student successfully created with ID: " + generatedId);
                return ServiceResult.success("Student enrolled successfully.", student);
            } else {
                return ServiceResult.failure("Failed to save student record to database.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during student creation: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not complete student enrollment.");
        }
    }

    /**
     * Updates an existing student record. Restricted to ADMIN only.
     */
    public ServiceResult<Student> updateStudent(Student student, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can update student records.");
        }
        if (student == null || student.getId() <= 0) {
            return ServiceResult.failure("Invalid student ID for update operation.");
        }

        String validationError = validateStudent(student);
        if (validationError != null) {
            return ServiceResult.failure(validationError);
        }

        try (Connection conn = dbConnection.getConnection()) {
            // Check student exists
            Student existing = studentDAO.getStudentById(conn, student.getId());
            if (existing == null) {
                return ServiceResult.failure("Student record not found.");
            }

            // Check roll number uniqueness excluding current student
            if (studentDAO.rollNumberExists(conn, student.getRollNo().trim(), student.getId())) {
                return ServiceResult.failure("Another student with Roll Number '" + student.getRollNo().trim() + "' already exists.");
            }

            // Check email uniqueness excluding current student
            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                if (studentDAO.emailExists(conn, student.getEmail().trim(), student.getId())) {
                    return ServiceResult.failure("Another student with Email '" + student.getEmail().trim() + "' already exists.");
                }
            }

            boolean updated = studentDAO.updateStudent(conn, student);
            if (updated) {
                LOGGER.info("Student successfully updated for ID: " + student.getId());
                return ServiceResult.success("Student updated successfully.", student);
            } else {
                return ServiceResult.failure("Failed to update student in database.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during student update: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not complete student update.");
        }
    }

    /**
     * Deletes a student. Restricted to ADMIN only.
     * Enforces foreign-key safety and prevents orphan references.
     */
    public ServiceResult<Void> deleteStudent(int studentId, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can delete student records.");
        }
        if (studentId <= 0) {
            return ServiceResult.failure("Invalid student ID for deletion.");
        }

        try (Connection conn = dbConnection.getConnection()) {
            Student existing = studentDAO.getStudentById(conn, studentId);
            if (existing == null) {
                return ServiceResult.failure("Student record not found.");
            }

            // Guard against foreign key violation: Check attendance records
            if (studentDAO.hasAttendanceRecords(conn, studentId)) {
                return ServiceResult.failure("Cannot delete student '" + existing.getName() +
                        "': Attendance records exist for this student. Delete related attendance logs first.");
            }

            // Guard against foreign key violation: Check user login accounts
            if (studentDAO.hasUserAccount(conn, studentId)) {
                return ServiceResult.failure("Cannot delete student '" + existing.getName() +
                        "': A login user account is linked to this student record. Remove user account first.");
            }

            boolean deleted = studentDAO.deleteStudent(conn, studentId);
            if (deleted) {
                LOGGER.info("Student successfully deleted for ID: " + studentId);
                return ServiceResult.success("Student deleted successfully.", null);
            } else {
                return ServiceResult.failure("Failed to delete student from database.");
            }
        } catch (SQLException e) {
            // Catch MySQL Foreign Key Constraint violation (error 1451 / SQLState 23000)
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1451) {
                return ServiceResult.failure("Cannot delete student: Dependent records exist in the database.");
            }
            LOGGER.log(Level.SEVERE, "Database error during student deletion: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not delete student.");
        }
    }

    private boolean canViewStudents(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.FACULTY;
    }

    /**
     * Validates field integrity of student entities.
     *
     * @param student student instance
     * @return error message if invalid, or {@code null} if valid
     */
    public String validateStudent(Student student) {
        if (student == null) {
            return "Student data cannot be null.";
        }

        // Full Name
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            return "Student full name is required.";
        }
        if (student.getName().trim().length() > 100) {
            return "Student full name cannot exceed 100 characters.";
        }

        // Roll Number
        if (student.getRollNo() == null || student.getRollNo().trim().isEmpty()) {
            return "Roll number is required.";
        }
        if (student.getRollNo().trim().length() > 20) {
            return "Roll number cannot exceed 20 characters.";
        }

        // Branch
        if (student.getBranch() == null || student.getBranch().trim().isEmpty()) {
            return "Branch / Department is required.";
        }
        if (student.getBranch().trim().length() > 50) {
            return "Branch cannot exceed 50 characters.";
        }

        // Semester
        if (student.getSemester() <= 0) {
            return "Semester must be a positive integer (e.g. 1 to 8).";
        }
        if (student.getSemester() > 12) {
            return "Semester exceeds maximum academic threshold (12).";
        }

        // Section
        if (student.getSection() == null || student.getSection().trim().isEmpty()) {
            return "Section is required.";
        }
        if (student.getSection().trim().length() > 10) {
            return "Section cannot exceed 10 characters.";
        }

        // Email (optional, but validate format if present)
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            String trimmedEmail = student.getEmail().trim();
            if (trimmedEmail.length() > 100) {
                return "Email address cannot exceed 100 characters.";
            }
            if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                return "Please enter a valid email address format.";
            }
        }

        // Phone (optional, check length)
        if (student.getPhone() != null && !student.getPhone().isBlank()) {
            if (student.getPhone().trim().length() > 15) {
                return "Phone number cannot exceed 15 characters.";
            }
        }

        return null;
    }
}
