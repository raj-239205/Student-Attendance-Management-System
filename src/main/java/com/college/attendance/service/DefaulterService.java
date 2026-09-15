package com.college.attendance.service;

import com.college.attendance.config.AppConstants;
import com.college.attendance.dao.DefaulterDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.DefaulterSummary;
import com.college.attendance.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service component coordinating automatic defaulter identification, threshold configuration,
 * deficit calculation, summary statistics, and strict programmatic RBAC.
 */
public class DefaulterService {

    private static final Logger LOGGER = Logger.getLogger(DefaulterService.class.getName());

    private final DatabaseConnection dbConnection;
    private final DefaulterDAO defaulterDAO;

    public DefaulterService(DatabaseConnection dbConnection) {
        this(dbConnection, new DefaulterDAO());
    }

    public DefaulterService(DatabaseConnection dbConnection, DefaulterDAO defaulterDAO) {
        this.dbConnection = dbConnection;
        this.defaulterDAO = defaulterDAO;
    }

    /**
     * Resolves an effective threshold percentage, ensuring it falls within [0.0, 100.0].
     * Defaults to {@link AppConstants#DEFAULT_DEFAULTER_THRESHOLD} (75.0%) if invalid or null.
     */
    public double resolveThreshold(Double threshold) {
        if (threshold == null || threshold.isNaN() || threshold.isInfinite() || threshold < 0.0 || threshold > 100.0) {
            return AppConstants.DEFAULT_DEFAULTER_THRESHOLD;
        }
        return Math.round(threshold * 100.0) / 100.0;
    }

    /**
     * Calculates the attendance deficit against the required threshold.
     * Deficit is 0.0 if student is at or above threshold.
     */
    public double calculateDeficit(double threshold, double attendancePercentage) {
        if (attendancePercentage >= threshold) {
            return 0.0;
        }
        double deficit = threshold - attendancePercentage;
        return Math.round(deficit * 100.0) / 100.0;
    }

    /**
     * Calculates attendance percentage from present and total class counts safely without division by zero.
     */
    public double calculatePercentage(int presentClasses, int totalClasses) {
        if (totalClasses <= 0) {
            return 0.0;
        }
        double pct = ((double) presentClasses / totalClasses) * 100.0;
        return Math.round(pct * 100.0) / 100.0;
    }

    /**
     * Evaluates the standing status of a student for a subject.
     */
    public DefaulterRecord.Status evaluateStatus(int totalClasses, double attendancePercentage, double threshold) {
        if (totalClasses <= 0) {
            return DefaulterRecord.Status.NO_DATA;
        }
        return (attendancePercentage < threshold) ? DefaulterRecord.Status.DEFAULTER : DefaulterRecord.Status.SAFE;
    }

    /**
     * Retrieves student defaulter records across subjects.
     * Programmatic RBAC: Restricted strictly to ADMIN and FACULTY callers.
     */
    public List<DefaulterRecord> getDefaulterRecords(Integer subjectId, Double threshold,
                                                     String statusFilter, String searchKeyword,
                                                     User.Role callerRole) {
        // Strict RBAC enforcement
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access defaulters management module.");
            return Collections.emptyList();
        }

        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return defaulterDAO.getDefaulterRecords(conn, subjectId, effectiveThreshold, statusFilter, searchKeyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving defaulter records: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves aggregated summary statistics for defaulter analysis.
     * Restricted to ADMIN and FACULTY.
     */
    public DefaulterSummary getDefaulterSummary(Integer subjectId, Double threshold, User.Role callerRole) {
        if (callerRole != User.Role.ADMIN && callerRole != User.Role.FACULTY) {
            LOGGER.warning("Access Denied: Role " + callerRole + " attempted to access defaulters summary.");
            return DefaulterSummary.empty(resolveThreshold(threshold));
        }

        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return defaulterDAO.getDefaulterSummary(conn, subjectId, effectiveThreshold);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving defaulter summary: {0}", e.getMessage());
            return DefaulterSummary.empty(effectiveThreshold);
        }
    }

    /**
     * Retrieves course-wise defaulter records for a specific student.
     * Enforces student isolation: STUDENT caller can only query their own linked studentId.
     */
    public List<DefaulterRecord> getStudentDefaulterBreakdown(int targetStudentId, Double threshold,
                                                              User.Role callerRole, Integer callerStudentId) {
        if (callerRole == User.Role.STUDENT) {
            if (callerStudentId == null || callerStudentId != targetStudentId) {
                LOGGER.warning(String.format("Blocked student isolation breach: Caller student %s attempted to query defaulter status of student %d",
                        callerStudentId, targetStudentId));
                return Collections.emptyList();
            }
        }

        if (targetStudentId <= 0) {
            return Collections.emptyList();
        }

        double effectiveThreshold = resolveThreshold(threshold);

        try (Connection conn = dbConnection.getConnection()) {
            return defaulterDAO.getStudentDefaulterBreakdown(conn, targetStudentId, effectiveThreshold);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error retrieving student defaulter breakdown: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
