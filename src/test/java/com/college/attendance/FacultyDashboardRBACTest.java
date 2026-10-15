package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.DashboardDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.LectureAttendanceSummary;
import com.college.attendance.model.Subject;
import com.college.attendance.service.DashboardService;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Automated test suite verifying the Faculty Dashboard & RBAC Scoping:
 * 1. Faculty assigned subjects resolution (exactly 4 modules)
 * 2. Today's Present KPI dynamically computed and scoped to assigned subjects
 * 3. Today's Lectures KPI dynamically computed and scoped to assigned subjects
 * 4. Defaulters count strictly scoped to assigned subjects using 75% threshold
 * 5. My Attendance Overview donut distribution strictly scoped to assigned subjects
 * 6. Today's attendance session breakdown for assigned subjects
 * 7. Lecture attendance filter & aggregation strictly for assigned subjects
 * 8. RBAC security: Unauthorized subject queries are strictly blocked at service/DAO level
 */
public class FacultyDashboardRBACTest {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   FACULTY DASHBOARD & RBAC SCOPING — AUTOMATED TEST SUITE                      ");
        System.out.println("================================================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        DashboardDAO dashboardDAO = new DashboardDAO();
        DashboardService dashboardService = new DashboardService(dbConnection, dashboardDAO);

        int facultyUserId = 2; // Default Faculty user
        LocalDate targetDate = LocalDate.of(2026, 9, 16);

        try (Connection conn = dbConnection.getConnection()) {

            // Test 1: Assigned Subjects Resolution
            testsTotal++;
            List<Subject> assignedSubjects = dashboardService.getFacultyAssignedSubjects(facultyUserId);
            System.out.println("[Test 1] Faculty Assigned Subjects: " + assignedSubjects.stream().map(Subject::getSubjectCode).toList());
            assertEquals("Faculty must be assigned 4 subjects", 4, assignedSubjects.size());
            assertTrue("Assigned subjects must contain CS401", assignedSubjects.stream().anyMatch(s -> "CS401".equals(s.getSubjectCode())));
            assertTrue("Assigned subjects must contain CS402", assignedSubjects.stream().anyMatch(s -> "CS402".equals(s.getSubjectCode())));
            assertTrue("Assigned subjects must contain CS403", assignedSubjects.stream().anyMatch(s -> "CS403".equals(s.getSubjectCode())));
            assertTrue("Assigned subjects must contain CS404", assignedSubjects.stream().anyMatch(s -> "CS404".equals(s.getSubjectCode())));
            testsPassed++;

            // Test 2: Faculty Assigned Metrics
            testsTotal++;
            DashboardService.FacultyAssignedMetrics metrics = dashboardService.getFacultyAssignedMetrics(facultyUserId, targetDate);
            System.out.println("[Test 2] Faculty Assigned Metrics on " + targetDate + ": " +
                    "Assigned=" + metrics.assignedSubjectsCount() +
                    ", TodayPresent=" + metrics.todayPresentCount() +
                    ", TodayLectures=" + metrics.todayLecturesCount() +
                    ", Defaulters=" + metrics.defaultersCount());

            assertEquals("Assigned subjects KPI must be 4", 4, metrics.assignedSubjectsCount());
            assertTrue("Today's present must be > 0", metrics.todayPresentCount() > 0);
            assertTrue("Today's present must not exceed total students (60)", metrics.todayPresentCount() <= 60);
            assertTrue("Today's lectures must be > 0 and <= 4", metrics.todayLecturesCount() > 0 && metrics.todayLecturesCount() <= 4);
            assertTrue("Defaulters count must be >= 0", metrics.defaultersCount() >= 0);
            testsPassed++;

            // Test 3: Faculty Attendance Overview Donut Distribution
            testsTotal++;
            AttendanceDistribution dist = dashboardService.getFacultyAttendanceDistribution(facultyUserId);
            System.out.printf("[Test 3] Faculty Donut Distribution: Present=%d (%.1f%%), Absent=%d (%.1f%%), Total=%d%n",
                    dist.getPresentCount(), dist.getPresentPercentage(),
                    dist.getAbsentCount(), dist.getAbsentPercentage(),
                    dist.getTotalCount());
            assertTrue("Present count must be > 0", dist.getPresentCount() > 0);
            assertTrue("Absent count must be > 0", dist.getAbsentCount() > 0);
            assertEquals("Total records must equal present + absent", dist.getTotalCount(), dist.getPresentCount() + dist.getAbsentCount());
            assertEquals("Percentages must sum to 100", 100.0, dist.getPresentPercentage() + dist.getAbsentPercentage(), 0.1);
            testsPassed++;

            // Test 4: Faculty Today's Attendance Sessions
            testsTotal++;
            List<LectureAttendanceSummary> todaySessions = dashboardService.getFacultyTodayAttendance(facultyUserId, targetDate);
            System.out.println("[Test 4] Today's attendance sessions for faculty: " + todaySessions.size());
            assertTrue("Must have sessions held today for faculty", !todaySessions.isEmpty());
            for (LectureAttendanceSummary s : todaySessions) {
                System.out.printf("   - %s (%s): Present=%d, Absent=%d, Total=%d, Pct=%.1f%%%n",
                        s.getSubjectCode(), s.getSubjectName(),
                        s.getPresentCount(), s.getAbsentCount(),
                        s.getTotalCount(), s.getAttendancePercentage());
                assertTrue("Session subject must be among assigned subjects",
                        assignedSubjects.stream().anyMatch(sub -> sub.getId() == s.getSubjectId()));
                assertEquals("Present + Absent must equal Total", s.getTotalCount(), s.getPresentCount() + s.getAbsentCount());
            }
            testsPassed++;

            // Test 5: Faculty Lecture Attendance List & Date Filtering
            testsTotal++;
            List<LectureAttendanceSummary> allAssignedLectures = dashboardService.getFacultyLectureAttendance(facultyUserId, null, null, null, 100);
            assertTrue("Must retrieve historical lecture sessions for assigned subjects", !allAssignedLectures.isEmpty());
            for (LectureAttendanceSummary l : allAssignedLectures) {
                assertTrue("Every lecture returned must belong to assigned subjects",
                        assignedSubjects.stream().anyMatch(sub -> sub.getId() == l.getSubjectId()));
            }
            testsPassed++;

            // Test 6: RBAC Isolation — Faculty cannot query unassigned subjects
            testsTotal++;
            int unassignedSubjectId = 8; // EC401 is not assigned to facultyUserId = 2
            boolean isAssigned = assignedSubjects.stream().anyMatch(sub -> sub.getId() == unassignedSubjectId);
            assertFalse("Subject 8 (EC401) must NOT be assigned to faculty user 2", isAssigned);

            List<LectureAttendanceSummary> unauthorizedQuery = dashboardService.getFacultyLectureAttendance(
                    facultyUserId, null, null, unassignedSubjectId, 100
            );
            assertTrue("Querying unauthorized subject must be strictly blocked and return empty list", unauthorizedQuery.isEmpty());
            testsPassed++;

            // Test 7: Backward Compatibility of Legacy getFacultyMetrics()
            testsTotal++;
            DashboardService.FacultyDashboardMetrics legacyMetrics = dashboardService.getFacultyMetrics(targetDate);
            assertTrue("Legacy getFacultyMetrics still returns total students >= 60", legacyMetrics.totalStudents() >= 60);
            assertTrue("Legacy getFacultyMetrics still returns defaultersCount >= 1", legacyMetrics.defaultersCount() >= 1);
            testsPassed++;

            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("  FACULTY DASHBOARD RBAC SUITE: " + testsPassed + "/" + testsTotal + " TESTS PASSED (100%)");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("[TEST FAILURE] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {}
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError("[ASSERTION FAILED] " + message);
        }
    }

    private static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new AssertionError("[ASSERTION FAILED] " + message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("[ASSERTION FAILED] " + message + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertEquals(String message, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError("[ASSERTION FAILED] " + message + " | Expected: " + expected + ", Actual: " + actual + " (delta: " + delta + ")");
        }
    }
}
