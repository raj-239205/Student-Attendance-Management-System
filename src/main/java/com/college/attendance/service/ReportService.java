package com.college.attendance.service;

import com.college.attendance.config.AppConstants;
import com.college.attendance.dao.ReportDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.AttendanceTrendItem;
import com.college.attendance.model.OverallAttendanceSummary;
import com.college.attendance.model.StudentAttendanceReportItem;
import com.college.attendance.model.SubjectAttendanceReportItem;
import com.college.attendance.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service coordinating comprehensive academic attendance reports, analytics,
 * date-range filtering, deficit formulas, and strict role-based access control.
 */
public class ReportService {

    private static final Logger LOGGER = Logger.getLogger(ReportService.class.getName());

    private final DatabaseConnection dbConnection;
    private final ReportDAO reportDAO;

    public ReportService(DatabaseConnection dbConnection) {
        this(dbConnection, new ReportDAO());
    }

    public ReportService(DatabaseConnection dbConnection, ReportDAO reportDAO) {
        this.dbConnection = dbConnection;
        this.reportDAO = reportDAO;
    }

    /**
     * Resolves the threshold percentage, defaulting to {@link AppConstants#DEFAULT_DEFAULTER_THRESHOLD} (75.0%).
     */
    public double resolveThreshold(Double threshold) {
        if (threshold == null || threshold.isNaN() || threshold.isInfinite() || threshold < 0.0 || threshold > 100.0) {
            return AppConstants.DEFAULT_DEFAULTER_THRESHOLD;
        }
        return Math.round(threshold * 100.0) / 100.0;
    }

    /**
     * Validates date range logic. Swaps start and end dates if start is after end.
     */
    public LocalDate[] normalizeDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            LOGGER.info("Start date is after end date; normalizing date range automatically.");
            return new LocalDate[]{endDate, startDate};
        }
        return new LocalDate[]{startDate, endDate};
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

    /**
     * Retrieves overall attendance summary metrics.
     * Restricted to ADMIN and FACULTY.
     */
    public OverallAttendanceSummary getOverallSummary(Integer subjectId, LocalDate startDate, LocalDate endDate,
                                                      Double threshold, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access overall attendance summary.");
            return OverallAttendanceSummary.empty(resolveThreshold(threshold));
        }

        LocalDate[] range = normalizeDateRange(startDate, endDate);
        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return reportDAO.getOverallSummary(conn, subjectId, range[0], range[1], effectiveThreshold);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving overall summary: {0}", e.getMessage());
            return OverallAttendanceSummary.empty(effectiveThreshold);
        }
    }

    /**
     * Retrieves student-wise attendance report records.
     * Restricted to ADMIN and FACULTY.
     */
    public List<StudentAttendanceReportItem> getStudentAttendanceReports(Integer subjectId, Integer studentId,
                                                                        LocalDate startDate, LocalDate endDate,
                                                                        String statusFilter, String searchKeyword,
                                                                        Double threshold, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access student attendance reports.");
            return Collections.emptyList();
        }

        LocalDate[] range = normalizeDateRange(startDate, endDate);
        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return reportDAO.getStudentAttendanceReports(conn, subjectId, studentId, range[0], range[1],
                    statusFilter, searchKeyword, effectiveThreshold);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving student reports: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves subject-wise performance reports.
     * Restricted to ADMIN and FACULTY.
     */
    public List<SubjectAttendanceReportItem> getSubjectAttendanceReports(LocalDate startDate, LocalDate endDate,
                                                                        Double threshold, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access subject reports.");
            return Collections.emptyList();
        }

        LocalDate[] range = normalizeDateRange(startDate, endDate);
        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return reportDAO.getSubjectAttendanceReports(conn, range[0], range[1], effectiveThreshold);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving subject reports: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves attendance distribution (Present vs Absent) for charts.
     * Restricted to ADMIN and FACULTY.
     */
    public AttendanceDistribution getAttendanceDistribution(Integer subjectId, LocalDate startDate, LocalDate endDate,
                                                            User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access attendance distribution.");
            return AttendanceDistribution.empty();
        }

        LocalDate[] range = normalizeDateRange(startDate, endDate);

        try (Connection conn = dbConnection.getConnection()) {
            return reportDAO.getAttendanceDistribution(conn, subjectId, range[0], range[1]);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving attendance distribution: {0}", e.getMessage());
            return AttendanceDistribution.empty();
        }
    }

    /**
     * Retrieves daily attendance trends for time-series charts.
     * Restricted to ADMIN and FACULTY.
     */
    public List<AttendanceTrendItem> getAttendanceTrends(Integer subjectId, LocalDate startDate, LocalDate endDate,
                                                         User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access attendance trends.");
            return Collections.emptyList();
        }

        LocalDate[] range = normalizeDateRange(startDate, endDate);

        try (Connection conn = dbConnection.getConnection()) {
            return reportDAO.getAttendanceTrends(conn, subjectId, range[0], range[1]);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving attendance trends: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves personal report records for a student. Enforces student data isolation.
     */
    public List<StudentAttendanceReportItem> getPersonalStudentReport(int targetStudentId, LocalDate startDate,
                                                                     LocalDate endDate, Double threshold,
                                                                     User.Role callerRole, Integer callerStudentId) {
        if (callerRole == User.Role.STUDENT) {
            if (callerStudentId == null || callerStudentId != targetStudentId) {
                LOGGER.warning(String.format("Blocked student isolation breach: Caller student %s attempted to query student %d",
                        callerStudentId, targetStudentId));
                return Collections.emptyList();
            }
        }

        if (targetStudentId <= 0) {
            return Collections.emptyList();
        }

        LocalDate[] range = normalizeDateRange(startDate, endDate);
        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return reportDAO.getStudentAttendanceReports(conn, null, targetStudentId, range[0], range[1],
                    "ALL", null, effectiveThreshold);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving personal student report: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
