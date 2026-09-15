package com.college.attendance.service;

import com.college.attendance.config.AppConstants;
import com.college.attendance.dao.DashboardDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.StudentAttendanceSummary;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-level business service for calculating dashboard metrics and personal student summaries.
 */
public class DashboardService {

    private static final Logger LOGGER = Logger.getLogger(DashboardService.class.getName());

    private final DatabaseConnection dbConnection;
    private final DashboardDAO dashboardDAO;

    public DashboardService(DatabaseConnection dbConnection) {
        this(dbConnection, new DashboardDAO());
    }

    public DashboardService(DatabaseConnection dbConnection, DashboardDAO dashboardDAO) {
        this.dbConnection = dbConnection;
        this.dashboardDAO = dashboardDAO;
    }

    /**
     * Metric container for the Administrator dashboard.
     */
    public record AdminDashboardMetrics(int totalStudents, int totalSubjects, int totalAttendanceRecords, int defaultersCount) {
        public static AdminDashboardMetrics empty() {
            return new AdminDashboardMetrics(0, 0, 0, 0);
        }
    }

    /**
     * Metric container for the Faculty dashboard.
     */
    public record FacultyDashboardMetrics(int totalStudents, int totalAttendanceRecords, int defaultersCount) {
        public static FacultyDashboardMetrics empty() {
            return new FacultyDashboardMetrics(0, 0, 0);
        }
    }

    /**
     * Fetches current administrator dashboard metrics.
     */
    public AdminDashboardMetrics getAdminMetrics() {
        try (Connection conn = dbConnection.getConnection()) {
            int students = dashboardDAO.countStudents(conn);
            int subjects = dashboardDAO.countSubjects(conn);
            int attendance = dashboardDAO.countAttendanceRecords(conn);
            int defaulters = dashboardDAO.countDefaulters(conn, AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
            return new AdminDashboardMetrics(students, subjects, attendance, defaulters);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load admin metrics: {0}", e.getMessage());
            return AdminDashboardMetrics.empty();
        }
    }

    /**
     * Fetches current faculty dashboard metrics.
     */
    public FacultyDashboardMetrics getFacultyMetrics() {
        try (Connection conn = dbConnection.getConnection()) {
            int students = dashboardDAO.countStudents(conn);
            int attendance = dashboardDAO.countAttendanceRecords(conn);
            int defaulters = dashboardDAO.countDefaulters(conn, AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
            return new FacultyDashboardMetrics(students, attendance, defaulters);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load faculty metrics: {0}", e.getMessage());
            return FacultyDashboardMetrics.empty();
        }
    }

    /**
     * Fetches personal attendance summary for an authenticated student using their verified studentId.
     *
     * @param studentId authoritative student primary key
     * @return {@link StudentAttendanceSummary}, or {@code null} if student record is missing
     */
    public StudentAttendanceSummary getStudentSummary(Integer studentId) {
        if (studentId == null || studentId <= 0) {
            LOGGER.warning("Attempted to fetch student summary with invalid or null studentId");
            return null;
        }

        try (Connection conn = dbConnection.getConnection()) {
            return dashboardDAO.getStudentAttendanceSummary(conn, studentId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load student summary: {0}", e.getMessage());
            return null;
        }
    }
}
