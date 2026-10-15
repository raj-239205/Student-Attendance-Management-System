package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.DashboardDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.LectureAttendanceSummary;
import com.college.attendance.service.DashboardService;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Automated test suite verifying the Dashboard Attendance Metric Correction:
 * 1. Replacement of raw attendance row count with distinct Today's Present count
 * 2. No duplicate counting of students attending multiple lectures on the same date
 * 3. Lecture-wise attendance aggregation, session breakdown, present/absent counts, and percentage
 * 4. Date and subject filtering for lecture attendance
 * 5. Clean handling of empty state when no attendance is recorded
 */
public class DashboardMetricsCorrectionTest {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   DASHBOARD ATTENDANCE METRIC CORRECTION — AUTOMATED TEST SUITE                ");
        System.out.println("================================================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        DashboardDAO dashboardDAO = new DashboardDAO();
        DashboardService dashboardService = new DashboardService(dbConnection, dashboardDAO);

        LocalDate targetDate = LocalDate.of(2026, 9, 16);

        try (Connection conn = dbConnection.getConnection()) {

            // Test 1: Today's Present distinct student count
            testsTotal++;
            int todayPresent = dashboardDAO.countTodayPresentStudents(conn, targetDate);
            System.out.println("[Test 1] Today's Present students on " + targetDate + ": " + todayPresent);
            assertTrue("Today's Present must be > 0 on " + targetDate, todayPresent > 0);
            assertTrue("Today's Present on " + targetDate + " must be between 35 and 60 distinct students", todayPresent >= 35 && todayPresent <= 60);
            testsPassed++;

            // Test 2: Ensure distinct student count does NOT double count
            testsTotal++;
            List<LectureAttendanceSummary> lecturesToday = dashboardDAO.getLectureAttendanceSummaries(conn, targetDate, targetDate, null, 0);
            int sumPresentAcrossLectures = lecturesToday.stream().mapToInt(LectureAttendanceSummary::getPresentCount).sum();
            System.out.println("[Test 2] Sum of present across lectures: " + sumPresentAcrossLectures + ", Distinct present students: " + todayPresent);
            assertTrue("Sum of lecture attendance (" + sumPresentAcrossLectures + ") can exceed distinct student count (" + todayPresent + ") due to multiple classes",
                    sumPresentAcrossLectures >= todayPresent);
            assertTrue("Distinct student count prevents double-counting", todayPresent <= 60);
            testsPassed++;

            // Test 3: Admin dashboard metrics contains todayPresentCount
            testsTotal++;
            DashboardService.AdminDashboardMetrics adminMetrics = dashboardService.getAdminMetrics(targetDate);
            System.out.println("[Test 3] Admin Metrics: Students=" + adminMetrics.totalStudents() +
                    ", Subjects=" + adminMetrics.totalSubjects() +
                    ", Today's Present=" + adminMetrics.todayPresentCount() +
                    ", Defaulters=" + adminMetrics.defaultersCount() +
                    ", Historical Logs=" + adminMetrics.totalAttendanceRecords());
            assertEquals("Total Students KPI must be 60", 60, adminMetrics.totalStudents());
            assertEquals("Total Subjects KPI must be 8", 8, adminMetrics.totalSubjects());
            assertTrue("Today's Present KPI must be between 35 and 60", adminMetrics.todayPresentCount() >= 35 && adminMetrics.todayPresentCount() <= 60);
            assertEquals("Defaulters KPI must be 17", 17, adminMetrics.defaultersCount());
            assertTrue("Historical attendance records preserved (>= 4000)", adminMetrics.totalAttendanceRecords() >= 4000);
            testsPassed++;

            // Test 4: Lecture-wise attendance summary integrity
            testsTotal++;
            System.out.println("[Test 4] Verifying Lecture Attendance Summaries on " + targetDate + ":");
            assertTrue("Must have lecture sessions recorded on " + targetDate, !lecturesToday.isEmpty());
            for (LectureAttendanceSummary lecture : lecturesToday) {
                System.out.printf("   - %s (%s): Present=%d, Absent=%d, Total=%d, Pct=%.1f%%%n",
                        lecture.getSubjectCode(), lecture.getSubjectName(),
                        lecture.getPresentCount(), lecture.getAbsentCount(),
                        lecture.getTotalCount(), lecture.getAttendancePercentage());
                assertEquals("Present + Absent must equal Total for " + lecture.getSubjectCode(),
                        lecture.getTotalCount(), lecture.getPresentCount() + lecture.getAbsentCount());
                assertTrue("Total count must be > 0", lecture.getTotalCount() > 0);
                double expectedPct = ((double) lecture.getPresentCount() / lecture.getTotalCount()) * 100.0;
                assertEquals("Percentage calculation must match formula", expectedPct, lecture.getAttendancePercentage(), 0.01);
            }
            testsPassed++;

            // Test 5: Date filter verification
            testsTotal++;
            LocalDate pastDate = LocalDate.of(2026, 9, 1);
            List<LectureAttendanceSummary> pastLectures = dashboardDAO.getLectureAttendanceSummaries(conn, pastDate, pastDate, null, 0);
            assertTrue("Must return lectures for 2026-09-01", !pastLectures.isEmpty());
            for (LectureAttendanceSummary l : pastLectures) {
                assertEquals("All returned lectures must be for the filtered date", pastDate, l.getDate());
            }
            testsPassed++;

            // Test 6: Subject filter verification
            testsTotal++;
            int sampleSubjectId = lecturesToday.get(0).getSubjectId();
            String sampleSubjectCode = lecturesToday.get(0).getSubjectCode();
            List<LectureAttendanceSummary> subjectFiltered = dashboardDAO.getLectureAttendanceSummaries(conn, null, null, sampleSubjectId, 50);
            assertTrue("Must return records for subject " + sampleSubjectCode, !subjectFiltered.isEmpty());
            for (LectureAttendanceSummary l : subjectFiltered) {
                assertEquals("Filtered subject ID must match", sampleSubjectId, l.getSubjectId());
                assertEquals("Filtered subject code must match", sampleSubjectCode, l.getSubjectCode());
            }
            testsPassed++;

            // Test 7: Empty state verification for a non-existent date
            testsTotal++;
            LocalDate nonExistentDate = LocalDate.of(2099, 1, 1);
            int zeroPresent = dashboardDAO.countTodayPresentStudents(conn, nonExistentDate);
            assertEquals("Non-existent date must have 0 students present", 0, zeroPresent);
            List<LectureAttendanceSummary> emptyLectures = dashboardDAO.getLectureAttendanceSummaries(conn, nonExistentDate, nonExistentDate, null, 0);
            assertTrue("Non-existent date must return empty lecture list", emptyLectures.isEmpty());
            testsPassed++;

            // Test 8: Backward compatibility for existing AdminDashboardMetrics
            testsTotal++;
            DashboardService.AdminDashboardMetrics compatMetrics = new DashboardService.AdminDashboardMetrics(60, 8, 4919, 17);
            assertEquals("4-arg constructor works for totalStudents", 60, compatMetrics.totalStudents());
            assertEquals("4-arg constructor works for totalSubjects", 8, compatMetrics.totalSubjects());
            assertEquals("4-arg constructor works for totalAttendanceRecords", 4919, compatMetrics.totalAttendanceRecords());
            assertEquals("4-arg constructor works for defaultersCount", 17, compatMetrics.defaultersCount());
            assertEquals("4-arg constructor defaults todayPresentCount to 0", 0, compatMetrics.todayPresentCount());
            testsPassed++;

            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("  DASHBOARD METRIC CORRECTION SUITE: " + testsPassed + "/" + testsTotal + " TESTS PASSED (100%)");
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
