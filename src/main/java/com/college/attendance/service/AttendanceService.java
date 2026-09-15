package com.college.attendance.service;

import com.college.attendance.dao.AttendanceDAO;
import com.college.attendance.dao.DashboardDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceRecord;
import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer coordinating academic attendance logging, business rules,
 * duplicate handling, percentage calculations, and strict programmatic RBAC.
 */
public class AttendanceService {

    private static final Logger LOGGER = Logger.getLogger(AttendanceService.class.getName());

    private final DatabaseConnection dbConnection;
    private final AttendanceDAO attendanceDAO;
    private final DashboardDAO dashboardDAO;

    public AttendanceService(DatabaseConnection dbConnection) {
        this(dbConnection, new AttendanceDAO(), new DashboardDAO());
    }

    public AttendanceService(DatabaseConnection dbConnection, AttendanceDAO attendanceDAO, DashboardDAO dashboardDAO) {
        this.dbConnection = dbConnection;
        this.attendanceDAO = attendanceDAO;
        this.dashboardDAO = dashboardDAO;
    }

    /**
     * Standard operation result wrapper for service-level responses.
     */
    public static class ServiceResult<T> {
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
     * Validates and submits a batch of attendance entries for a course on a date in a single transaction.
     * Enforces that callers must have ADMIN or FACULTY privileges.
     */
    public ServiceResult<Integer> markAttendanceBatch(int subjectId, LocalDate date, List<Attendance> entries, User.Role callerRole) {
        // RBAC check
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            return ServiceResult.failure("Access Denied: Only faculty and administrators can mark attendance.");
        }

        if (subjectId <= 0) {
            return ServiceResult.failure("Invalid subject selection.");
        }

        if (date == null) {
            return ServiceResult.failure("Attendance date is required.");
        }

        if (date.isAfter(LocalDate.now())) {
            return ServiceResult.failure("Attendance date cannot be in the future.");
        }

        if (entries == null || entries.isEmpty()) {
            return ServiceResult.failure("No student attendance records provided to save.");
        }

        for (Attendance att : entries) {
            if (att.getStudentId() <= 0) {
                return ServiceResult.failure("Invalid student record detected in submission.");
            }
            if (att.getStatus() == null) {
                return ServiceResult.failure("Every student must have an attendance status (PRESENT or ABSENT).");
            }
            att.setSubjectId(subjectId);
            att.setAttendanceDate(date);
        }

        try (Connection conn = dbConnection.getConnection()) {
            int savedCount = attendanceDAO.saveAttendanceBatch(conn, entries);
            LOGGER.info(String.format("Attendance batch saved: %d records for subject %d on %s", savedCount, subjectId, date));
            return ServiceResult.success(String.format("Successfully saved attendance for %d students.", savedCount), savedCount);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during batch attendance marking: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not complete attendance marking.");
        }
    }

    /**
     * Updates an existing attendance status. Accessible by ADMIN and FACULTY.
     */
    public ServiceResult<Void> updateAttendanceStatus(int id, Attendance.Status status, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            return ServiceResult.failure("Access Denied: You are not authorized to modify attendance records.");
        }

        if (id <= 0) {
            return ServiceResult.failure("Invalid attendance record ID.");
        }

        if (status == null) {
            return ServiceResult.failure("Attendance status cannot be null.");
        }

        try (Connection conn = dbConnection.getConnection()) {
            boolean updated = attendanceDAO.updateStatus(conn, id, status);
            if (updated) {
                LOGGER.info("Attendance record " + id + " updated to " + status);
                return ServiceResult.success("Attendance status updated successfully.", null);
            } else {
                return ServiceResult.failure("Attendance record not found.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during attendance status update: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not update attendance record.");
        }
    }

    /**
     * Deletes an attendance record. Restricted strictly to ADMIN.
     */
    public ServiceResult<Void> deleteAttendance(int id, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN) {
            return ServiceResult.failure("Access Denied: Only administrators can delete attendance records.");
        }

        if (id <= 0) {
            return ServiceResult.failure("Invalid attendance record ID for deletion.");
        }

        try (Connection conn = dbConnection.getConnection()) {
            boolean deleted = attendanceDAO.deleteAttendance(conn, id);
            if (deleted) {
                LOGGER.info("Attendance record " + id + " deleted by administrator.");
                return ServiceResult.success("Attendance record deleted successfully.", null);
            } else {
                return ServiceResult.failure("Attendance record not found.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during attendance deletion: {0}", e.getMessage());
            return ServiceResult.failure("Database error: Could not delete attendance record.");
        }
    }

    /**
     * Retrieves enrolled students for a selected subject to populate the marking grid.
     */
    public List<Student> getStudentsForSubject(int subjectId, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            return Collections.emptyList();
        }
        if (subjectId <= 0) {
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return attendanceDAO.getStudentsForSubject(conn, subjectId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error fetching students for subject: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves existing attendance records for a specific subject and date (useful for checking prior marking).
     */
    public List<AttendanceRecord> getAttendanceBySubjectAndDate(int subjectId, LocalDate date, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            return Collections.emptyList();
        }
        if (subjectId <= 0 || date == null) {
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return attendanceDAO.getAttendanceBySubjectAndDate(conn, subjectId, date);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error checking existing attendance: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves historical attendance records with optional filters.
     */
    public List<AttendanceRecord> getAttendanceHistory(Integer subjectId, LocalDate date, String keyword, int limit, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            return Collections.emptyList();
        }

        try (Connection conn = dbConnection.getConnection()) {
            return attendanceDAO.getAttendanceHistory(conn, subjectId, date, keyword, limit);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving attendance history: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves personal attendance summary for a student.
     * Enforces student isolation: student caller can only access their own linked studentId.
     */
    public StudentAttendanceSummary getStudentAttendanceSummary(int targetStudentId, User.Role callerRole, Integer callerStudentId) {
        if (callerRole == User.Role.STUDENT) {
            if (callerStudentId == null || callerStudentId != targetStudentId) {
                LOGGER.warning(String.format("Blocked student isolation breach: Caller student %s attempted to query student %d",
                        callerStudentId, targetStudentId));
                return null;
            }
        }

        if (targetStudentId <= 0) {
            return null;
        }

        try (Connection conn = dbConnection.getConnection()) {
            return dashboardDAO.getStudentAttendanceSummary(conn, targetStudentId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error fetching student attendance summary: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculates the attendance percentage given present and total counts.
     */
    public double calculatePercentage(int presentClasses, int totalClasses) {
        if (totalClasses <= 0) {
            return 0.0;
        }
        double pct = ((double) presentClasses / totalClasses) * 100.0;
        return Math.round(pct * 100.0) / 100.0;
    }
}
