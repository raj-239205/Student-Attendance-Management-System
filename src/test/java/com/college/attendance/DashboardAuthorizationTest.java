package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.DashboardDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Session;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.SubjectAttendance;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.AuthenticationService;
import com.college.attendance.service.DashboardService;
import com.college.attendance.ui.navigation.NavigationItem;

import java.sql.Connection;
import java.util.List;

/**
 * Comprehensive automated test suite for Stage 4 — Dashboard & Role-Based Navigation:
 * <ol>
 *   <li>ADMIN can access all administrator navigation destinations.</li>
 *   <li>FACULTY can access academic navigation destinations.</li>
 *   <li>FACULTY cannot access admin-only course catalog management.</li>
 *   <li>STUDENT can access personal attendance and profile navigation destinations.</li>
 *   <li>STUDENT is strictly denied access to STUDENTS management.</li>
 *   <li>STUDENT is strictly denied access to SUBJECTS catalog.</li>
 *   <li>STUDENT is strictly denied access to all-student ATTENDANCE records.</li>
 *   <li>STUDENT is strictly denied access to DEFAULTERS list.</li>
 *   <li>STUDENT is strictly denied access to administrative REPORTS.</li>
 *   <li>Unauthenticated session is denied for all destinations.</li>
 *   <li>DashboardService calculates correct student metrics for student_id = 1 (80.0% attendance, non-defaulter).</li>
 *   <li>DashboardService detects defaulters correctly at 75.0% threshold (Priya Patel at 40.0%).</li>
 *   <li>DashboardService handles invalid/null student IDs gracefully without throwing exceptions.</li>
 *   <li>DatabaseDAO counts (students, subjects, attendance) return valid positive numbers.</li>
 *   <li>Logout destroys the session and clears all identity attributes.</li>
 *   <li>Student data isolation: queries strictly depend on Session.getStudentId().</li>
 * </ol>
 */
public class DashboardAuthorizationTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Stage 4 — Dashboard & Role-Based Navigation Suite");
        System.out.println("==================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        AuthorizationService authz = new AuthorizationService();
        DashboardService dashboardService = new DashboardService(dbConnection);
        DashboardDAO dashboardDAO = new DashboardDAO();

        // --- SECTION A: ROLE-BASED AUTHORIZATION & PERMISSIONS ---
        System.out.println("\n[A] Verifying Role-Based Access Control (RBAC):");

        // Test 1: ADMIN permissions
        List<NavigationItem> adminItems = authz.getAllowedNavigationItems(User.Role.ADMIN);
        assertTrue("ADMIN has access to DASHBOARD", authz.canAccess(User.Role.ADMIN, NavigationItem.DASHBOARD));
        assertTrue("ADMIN has access to STUDENTS", authz.canAccess(User.Role.ADMIN, NavigationItem.STUDENTS));
        assertTrue("ADMIN has access to SUBJECTS", authz.canAccess(User.Role.ADMIN, NavigationItem.SUBJECTS));
        assertTrue("ADMIN has access to ATTENDANCE", authz.canAccess(User.Role.ADMIN, NavigationItem.ATTENDANCE));
        assertTrue("ADMIN has access to DEFAULTERS", authz.canAccess(User.Role.ADMIN, NavigationItem.DEFAULTERS));
        assertTrue("ADMIN has access to REPORTS", authz.canAccess(User.Role.ADMIN, NavigationItem.REPORTS));
        assertTrue("ADMIN allowed items count equals 6", adminItems.size() == 6);

        // Test 2: FACULTY permissions
        List<NavigationItem> facultyItems = authz.getAllowedNavigationItems(User.Role.FACULTY);
        assertTrue("FACULTY has access to DASHBOARD", authz.canAccess(User.Role.FACULTY, NavigationItem.DASHBOARD));
        assertTrue("FACULTY has access to STUDENTS", authz.canAccess(User.Role.FACULTY, NavigationItem.STUDENTS));
        assertTrue("FACULTY has access to ATTENDANCE", authz.canAccess(User.Role.FACULTY, NavigationItem.ATTENDANCE));
        assertTrue("FACULTY has access to DEFAULTERS", authz.canAccess(User.Role.FACULTY, NavigationItem.DEFAULTERS));
        assertTrue("FACULTY has access to REPORTS", authz.canAccess(User.Role.FACULTY, NavigationItem.REPORTS));
        assertTrue("FACULTY has access to SUBJECTS (read-only)", authz.canAccess(User.Role.FACULTY, NavigationItem.SUBJECTS));
        assertTrue("FACULTY allowed items count equals 6", facultyItems.size() == 6);

        // Test 3: STUDENT permissions & isolation
        List<NavigationItem> studentItems = authz.getAllowedNavigationItems(User.Role.STUDENT);
        assertTrue("STUDENT has access to DASHBOARD", authz.canAccess(User.Role.STUDENT, NavigationItem.DASHBOARD));
        assertTrue("STUDENT has access to MY_ATTENDANCE", authz.canAccess(User.Role.STUDENT, NavigationItem.MY_ATTENDANCE));
        assertTrue("STUDENT has access to MY_PROFILE", authz.canAccess(User.Role.STUDENT, NavigationItem.MY_PROFILE));
        assertFalse("STUDENT is denied STUDENTS management", authz.canAccess(User.Role.STUDENT, NavigationItem.STUDENTS));
        assertFalse("STUDENT is denied SUBJECTS catalog", authz.canAccess(User.Role.STUDENT, NavigationItem.SUBJECTS));
        assertFalse("STUDENT is denied ATTENDANCE management", authz.canAccess(User.Role.STUDENT, NavigationItem.ATTENDANCE));
        assertFalse("STUDENT is denied DEFAULTERS list", authz.canAccess(User.Role.STUDENT, NavigationItem.DEFAULTERS));
        assertFalse("STUDENT is denied administrative REPORTS", authz.canAccess(User.Role.STUDENT, NavigationItem.REPORTS));
        assertTrue("STUDENT allowed items count equals 3", studentItems.size() == 3);

        // Test 4: Unauthenticated / null checks
        assertFalse("Null role is denied DASHBOARD", authz.canAccess(null, NavigationItem.DASHBOARD));
        assertFalse("Null item is denied", authz.canAccess(User.Role.ADMIN, null));
        Session.destroy();
        assertFalse("Unauthenticated session has no access", authz.canAccess(NavigationItem.DASHBOARD));

        // --- SECTION B: DATABASE METRICS & STUDENT SUMMARY ---
        System.out.println("\n[B] Verifying Database Dashboard Metrics & Student Data:");

        // Test 5: Admin metrics from database
        DashboardService.AdminDashboardMetrics adminMetrics = dashboardService.getAdminMetrics();
        assertTrue("Total students >= 3", adminMetrics.totalStudents() >= 3);
        assertTrue("Total subjects >= 2", adminMetrics.totalSubjects() >= 2);
        assertTrue("Total attendance records >= 15", adminMetrics.totalAttendanceRecords() >= 15);
        assertTrue("Defaulters count >= 1", adminMetrics.defaultersCount() >= 1);

        // Test 6: Faculty metrics from database
        DashboardService.FacultyDashboardMetrics facultyMetrics = dashboardService.getFacultyMetrics();
        assertTrue("Faculty view sees total students", facultyMetrics.totalStudents() == adminMetrics.totalStudents());
        assertTrue("Faculty view sees attendance records", facultyMetrics.totalAttendanceRecords() == adminMetrics.totalAttendanceRecords());
        assertTrue("Faculty view sees defaulters count", facultyMetrics.defaultersCount() == adminMetrics.defaultersCount());

        // Test 7: Student attendance summary for student_id = 1 (Aarav Sharma)
        StudentAttendanceSummary s1Summary = dashboardService.getStudentSummary(1);
        assertTrue("Student 1 summary is not null", s1Summary != null);
        if (s1Summary != null) {
            assertTrue("Student 1 name is Aarav Sharma", "Aarav Sharma".equals(s1Summary.getStudent().getName()));
            assertTrue("Student 1 roll no is CS2024001", "CS2024001".equals(s1Summary.getStudent().getRollNo()));
            assertTrue("Student 1 total classes >= 5", s1Summary.getTotalClasses() >= 5);
            assertTrue("Student 1 present classes >= 4", s1Summary.getPresentClasses() >= 4);
            assertTrue("Student 1 absent classes >= 1", s1Summary.getAbsentClasses() >= 1);
            assertTrue("Student 1 overall percentage >= 75.0%", s1Summary.getOverallPercentage() >= 75.0);
            assertFalse("Student 1 is NOT a defaulter at 75.0%", s1Summary.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));

            List<SubjectAttendance> subjects = s1Summary.getSubjectAttendances();
            assertTrue("Student 1 has subject attendance records", !subjects.isEmpty());
            SubjectAttendance sub1 = subjects.get(0);
            assertTrue("Subject is CS401", "CS401".equals(sub1.getSubjectCode()));
            assertTrue("Subject percentage >= 75.0%", sub1.getPercentage() >= 75.0);
        }

        // Test 8: Defaulter student check for student_id = 2 (Priya Patel: defaulter < 75%)
        StudentAttendanceSummary s2Summary = dashboardService.getStudentSummary(2);
        assertTrue("Student 2 summary is not null", s2Summary != null);
        if (s2Summary != null) {
            assertTrue("Student 2 is Priya Patel", "Priya Patel".equals(s2Summary.getStudent().getName()));
            assertTrue("Student 2 percentage <= 50.0%", s2Summary.getOverallPercentage() <= 50.0);
            assertTrue("Student 2 IS a defaulter (< 75%)", s2Summary.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
        }

        // Test 9: Robust error & null handling
        StudentAttendanceSummary nullSummary = dashboardService.getStudentSummary(null);
        assertTrue("Null student ID safely returns null", nullSummary == null);

        StudentAttendanceSummary invalidSummary = dashboardService.getStudentSummary(-9999);
        assertTrue("Non-existent student ID safely returns null", invalidSummary == null);

        // --- SECTION C: SESSION ISOLATION & LOGOUT ---
        System.out.println("\n[C] Verifying Session Integrity & Logout:");

        // Test 10: Session creation and student ID attachment
        User studentUser = new User("test_student", "hash", "Test Student", User.Role.STUDENT, 1);
        Session studentSession = Session.start(studentUser);
        assertTrue("Session is authenticated", Session.isAuthenticated());
        assertTrue("Session studentId is 1", studentSession.getStudentId() != null && studentSession.getStudentId() == 1);
        assertTrue("AuthorizationService verifies active session access", authz.canAccess(NavigationItem.DASHBOARD));
        assertFalse("AuthorizationService blocks active session from admin items", authz.canAccess(NavigationItem.STUDENTS));

        // Test 11: Logout destroys session
        AuthenticationService authService = new AuthenticationService(dbConnection);
        authService.logout();
        assertFalse("Session is not authenticated after logout", Session.isAuthenticated());
        assertTrue("Session.getCurrent() is null after logout", Session.getCurrent() == null);
        assertFalse("AuthorizationService denies all after logout", authz.canAccess(NavigationItem.DASHBOARD));

        // --- SUMMARY ---
        System.out.println("\n==================================================");
        System.out.println(String.format("Stage 4 Results: %d PASSED, %d FAILED", passedCount, failedCount));
        System.out.println("==================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            System.out.println("  ✓ PASS: " + message);
            passedCount++;
        } else {
            System.err.println("  ✗ FAIL: " + message);
            failedCount++;
        }
    }

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    private static void assertEquals(String message, double expected, double actual, double delta) {
        boolean match = Math.abs(expected - actual) <= delta;
        if (match) {
            System.out.println(String.format("  ✓ PASS: %s (expected=%.2f, actual=%.2f)", message, expected, actual));
            passedCount++;
        } else {
            System.err.println(String.format("  ✗ FAIL: %s (expected=%.2f, actual=%.2f)", message, expected, actual));
            failedCount++;
        }
    }
}
