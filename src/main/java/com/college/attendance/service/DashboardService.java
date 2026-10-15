package com.college.attendance.service;

import com.college.attendance.config.AppConstants;
import com.college.attendance.dao.DashboardDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.LectureAttendanceSummary;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.Subject;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

    public DatabaseConnection getDbConnection() {
        return dbConnection;
    }

    /**
     * Metric container for the Administrator dashboard.
     */
    public record AdminDashboardMetrics(int totalStudents, int totalSubjects, int totalAttendanceRecords, int defaultersCount, int todayPresentCount) {
        public AdminDashboardMetrics(int totalStudents, int totalSubjects, int totalAttendanceRecords, int defaultersCount) {
            this(totalStudents, totalSubjects, totalAttendanceRecords, defaultersCount, 0);
        }

        public static AdminDashboardMetrics empty() {
            return new AdminDashboardMetrics(0, 0, 0, 0, 0);
        }
    }

    /**
     * Metric container for the Faculty dashboard.
     */
    public record FacultyDashboardMetrics(int totalStudents, int totalAttendanceRecords, int defaultersCount, int todayPresentCount) {
        public FacultyDashboardMetrics(int totalStudents, int totalAttendanceRecords, int defaultersCount) {
            this(totalStudents, totalAttendanceRecords, defaultersCount, 0);
        }

        public static FacultyDashboardMetrics empty() {
            return new FacultyDashboardMetrics(0, 0, 0, 0);
        }
    }

    /**
     * Metric container for the Faculty dashboard scoped strictly to their assigned subjects.
     */
    public record FacultyAssignedMetrics(
            int assignedSubjectsCount,
            int todayPresentCount,
            int todayLecturesCount,
            int defaultersCount,
            List<Subject> assignedSubjects
    ) {
        public static FacultyAssignedMetrics empty() {
            return new FacultyAssignedMetrics(0, 0, 0, 0, Collections.emptyList());
        }
    }

    /**
     * Fetches current administrator dashboard metrics for today.
     */
    public AdminDashboardMetrics getAdminMetrics() {
        return getAdminMetrics(LocalDate.now());
    }

    /**
     * Fetches administrator dashboard metrics for a specified date.
     */
    public AdminDashboardMetrics getAdminMetrics(LocalDate date) {
        try (Connection conn = dbConnection.getConnection()) {
            int students = dashboardDAO.countStudents(conn);
            int subjects = dashboardDAO.countSubjects(conn);
            int attendance = dashboardDAO.countAttendanceRecords(conn);
            int defaulters = dashboardDAO.countDefaulters(conn, AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
            int todayPresent = dashboardDAO.countTodayPresentStudents(conn, (date != null) ? date : LocalDate.now());
            return new AdminDashboardMetrics(students, subjects, attendance, defaulters, todayPresent);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load admin metrics: {0}", e.getMessage());
            return AdminDashboardMetrics.empty();
        }
    }

    /**
     * Fetches current faculty dashboard metrics for today.
     */
    public FacultyDashboardMetrics getFacultyMetrics() {
        return getFacultyMetrics(LocalDate.now());
    }

    /**
     * Fetches faculty dashboard metrics for a specified date.
     */
    public FacultyDashboardMetrics getFacultyMetrics(LocalDate date) {
        try (Connection conn = dbConnection.getConnection()) {
            int students = dashboardDAO.countStudents(conn);
            int attendance = dashboardDAO.countAttendanceRecords(conn);
            int defaulters = dashboardDAO.countDefaulters(conn, AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
            int todayPresent = dashboardDAO.countTodayPresentStudents(conn, (date != null) ? date : LocalDate.now());
            return new FacultyDashboardMetrics(students, attendance, defaulters, todayPresent);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load faculty metrics: {0}", e.getMessage());
            return FacultyDashboardMetrics.empty();
        }
    }

    /**
     * Fetches lecture-wise attendance summaries with optional date range and subject filtering.
     */
    public List<LectureAttendanceSummary> getLectureAttendance(LocalDate startDate, LocalDate endDate, Integer subjectId, int limit) {
        try (Connection conn = dbConnection.getConnection()) {
            return dashboardDAO.getLectureAttendanceSummaries(conn, startDate, endDate, subjectId, limit);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load lecture attendance: {0}", e.getMessage());
            return Collections.emptyList();
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

    /**
     * Fetches faculty dashboard metrics scoped strictly to their assigned subjects for today.
     */
    public FacultyAssignedMetrics getFacultyAssignedMetrics(int facultyUserId) {
        return getFacultyAssignedMetrics(facultyUserId, LocalDate.now());
    }

    /**
     * Fetches faculty dashboard metrics scoped strictly to their assigned subjects for a specified date.
     */
    public FacultyAssignedMetrics getFacultyAssignedMetrics(int facultyUserId, LocalDate date) {
        try (Connection conn = dbConnection.getConnection()) {
            List<Subject> assigned = dashboardDAO.getFacultyAssignedSubjects(conn, facultyUserId);
            List<Integer> subIds = assigned.stream().map(Subject::getId).toList();
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            int todayPresent = dashboardDAO.countFacultyAssignedTodayPresent(conn, subIds, targetDate);
            int todayLectures = dashboardDAO.countFacultyAssignedTodayLectures(conn, subIds, targetDate);
            int defaulters = dashboardDAO.countFacultyAssignedDefaulters(conn, subIds, AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
            return new FacultyAssignedMetrics(assigned.size(), todayPresent, todayLectures, defaulters, assigned);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load faculty assigned metrics: {0}", e.getMessage());
            return FacultyAssignedMetrics.empty();
        }
    }

    /**
     * Fetches overall attendance distribution (Present vs Absent) scoped strictly to the faculty's assigned subjects.
     */
    public AttendanceDistribution getFacultyAttendanceDistribution(int facultyUserId) {
        try (Connection conn = dbConnection.getConnection()) {
            List<Subject> assigned = dashboardDAO.getFacultyAssignedSubjects(conn, facultyUserId);
            List<Integer> subIds = assigned.stream().map(Subject::getId).toList();
            return dashboardDAO.getFacultyAttendanceDistribution(conn, subIds);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load faculty attendance distribution: {0}", e.getMessage());
            return AttendanceDistribution.empty();
        }
    }

    /**
     * Retrieves the list of subjects assigned to the faculty member.
     */
    public List<Subject> getFacultyAssignedSubjects(int facultyUserId) {
        try (Connection conn = dbConnection.getConnection()) {
            return dashboardDAO.getFacultyAssignedSubjects(conn, facultyUserId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load faculty assigned subjects: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches lecture-wise attendance summaries scoped strictly to the faculty's assigned subjects.
     */
    public List<LectureAttendanceSummary> getFacultyLectureAttendance(int facultyUserId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    Integer specificSubjectId,
                                                                    int limit) {
        try (Connection conn = dbConnection.getConnection()) {
            List<Subject> assigned = dashboardDAO.getFacultyAssignedSubjects(conn, facultyUserId);
            List<Integer> subIds = assigned.stream().map(Subject::getId).toList();
            return dashboardDAO.getFacultyLectureAttendanceSummaries(conn, subIds, startDate, endDate, specificSubjectId, limit);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load faculty lecture attendance: {0}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches today's attendance sessions for the faculty's assigned subjects.
     */
    public List<LectureAttendanceSummary> getFacultyTodayAttendance(int facultyUserId, LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return getFacultyLectureAttendance(facultyUserId, targetDate, targetDate, null, 0);
    }
}
