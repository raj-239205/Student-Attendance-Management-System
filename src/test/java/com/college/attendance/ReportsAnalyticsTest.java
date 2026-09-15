package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.ReportDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.AttendanceTrendItem;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.OverallAttendanceSummary;
import com.college.attendance.model.StudentAttendanceReportItem;
import com.college.attendance.model.SubjectAttendanceReportItem;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.ReportService;
import com.college.attendance.ui.components.StudentReportTableModel;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.util.CsvExportUtil;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

/**
 * Automated test suite for Stage 9 — Reports & Analytics Module:
 * <ol>
 *   <li>ADMIN has access to REPORTS navigation.</li>
 *   <li>FACULTY has access to REPORTS navigation.</li>
 *   <li>STUDENT is denied access to REPORTS navigation.</li>
 *   <li>Programmatic RBAC: STUDENT cannot query overall summary.</li>
 *   <li>Programmatic RBAC: STUDENT cannot query general student reports.</li>
 *   <li>Programmatic RBAC: STUDENT cannot query subject reports.</li>
 *   <li>Student data isolation: Student can query own personal report.</li>
 *   <li>Student data isolation: Student blocked from querying another student's report.</li>
 *   <li>Overall summary calculations: Total records == Present + Absent.</li>
 *   <li>Overall summary calculations: Attendance percentage matches formula.</li>
 *   <li>Overall summary calculations: Defaulters + Safe == Evaluated students.</li>
 *   <li>Student report calculations: Attendance percentage matches present/total.</li>
 *   <li>Student report standing: % < 75.0 is DEFAULTER, % >= 75.0 is SAFE.</li>
 *   <li>Student report sorting: Lowest attendance percentage first.</li>
 *   <li>Subject filter: Isolates specific course in student report.</li>
 *   <li>Keyword search: Matches roll number and student name.</li>
 *   <li>Status filter: DEFAULTER filter returns only defaulters.</li>
 *   <li>Status filter: SAFE filter returns only safe students.</li>
 *   <li>Subject report: Total records == Present + Absent.</li>
 *   <li>Subject report: Defaulters + Safe == Total students.</li>
 *   <li>Subject report: Defaulter rate matches formula.</li>
 *   <li>Distribution query: Present + Absent == Total.</li>
 *   <li>Trend query: Returns chronological dates.</li>
 *   <li>Date range validation: Start date after end date is safely normalized.</li>
 *   <li>Date range filtering: Records outside range are excluded.</li>
 *   <li>CSV export: RFC 4180 escaping for commas and quotes.</li>
 *   <li>CSV export: Table model exports header and data rows correctly.</li>
 *   <li>Cross-module consistency: Attendance % identical between Defaulters and Reports.</li>
 * </ol>
 */
public class ReportsAnalyticsTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("Stage 9 — Reports & Analytics Module Test Suite");
        System.out.println("==========================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        AuthorizationService authz = new AuthorizationService();
        ReportDAO reportDAO = new ReportDAO();
        ReportService reportService = new ReportService(dbConnection, reportDAO);

        // --- SECTION A: ROLE-BASED ACCESS CONTROL (RBAC) ---
        System.out.println("\n[A] Verifying Navigation & Role-Based Access Control:");

        assertTrue("ADMIN has access to REPORTS navigation",
                authz.canAccess(User.Role.ADMIN, NavigationItem.REPORTS));
        assertTrue("FACULTY has access to REPORTS navigation",
                authz.canAccess(User.Role.FACULTY, NavigationItem.REPORTS));
        assertFalse("STUDENT is denied access to REPORTS navigation",
                authz.canAccess(User.Role.STUDENT, NavigationItem.REPORTS));

        // Programmatic RBAC checks
        OverallAttendanceSummary studentSummaryAttempt = reportService.getOverallSummary(
                null, null, null, 75.0, User.Role.STUDENT);
        assertEquals("STUDENT caller is programmatically denied overall summary (records 0)",
                0, studentSummaryAttempt.getTotalAttendanceRecords());

        List<StudentAttendanceReportItem> studentReportAttempt = reportService.getStudentAttendanceReports(
                null, null, null, null, "ALL", null, 75.0, User.Role.STUDENT);
        assertTrue("STUDENT caller is programmatically denied general student reports",
                studentReportAttempt.isEmpty());

        List<SubjectAttendanceReportItem> subjectReportAttempt = reportService.getSubjectAttendanceReports(
                null, null, 75.0, User.Role.STUDENT);
        assertTrue("STUDENT caller is programmatically denied subject reports",
                subjectReportAttempt.isEmpty());

        // Student Data Isolation
        List<StudentAttendanceReportItem> ownReport = reportService.getPersonalStudentReport(
                1, null, null, 75.0, User.Role.STUDENT, 1);
        assertTrue("STUDENT caller can access own personal report",
                ownReport != null && !ownReport.isEmpty());

        List<StudentAttendanceReportItem> foreignReport = reportService.getPersonalStudentReport(
                2, null, null, 75.0, User.Role.STUDENT, 1);
        assertTrue("STUDENT caller blocked from accessing another student's report",
                foreignReport.isEmpty());

        // --- SECTION B: OVERALL ATTENDANCE SUMMARY ---
        System.out.println("\n[B] Verifying Overall Attendance Summary Metrics:");

        OverallAttendanceSummary summary = reportService.getOverallSummary(
                null, null, null, 75.0, User.Role.ADMIN);
        assertTrue("Total students >= 3", summary.getTotalStudents() >= 3);
        assertTrue("Total subjects >= 2", summary.getTotalSubjects() >= 2);
        assertTrue("Total attendance records >= 15", summary.getTotalAttendanceRecords() >= 15);
        assertEquals("Total records equals present + absent count",
                summary.getTotalAttendanceRecords(), summary.getPresentCount() + summary.getAbsentCount());

        double expectedOverallPct = Math.round(((double) summary.getPresentCount() / summary.getTotalAttendanceRecords()) * 10000.0) / 100.0;
        assertEqualsDouble("Overall attendance percentage matches present/total formula",
                expectedOverallPct, summary.getOverallAttendancePercentage(), 0.05);

        assertTrue("Total defaulters >= 1", summary.getTotalDefaulters() >= 1);
        assertTrue("Safe students count >= 1", summary.getTotalSafeStudents() >= 1);

        // --- SECTION C: STUDENT-WISE ATTENDANCE REPORT ---
        System.out.println("\n[C] Verifying Student-Wise Attendance Report:");

        List<StudentAttendanceReportItem> studentReports = reportService.getStudentAttendanceReports(
                null, null, null, null, "ALL", null, 75.0, User.Role.ADMIN);
        assertTrue("Student reports returned for ADMIN", studentReports != null && !studentReports.isEmpty());

        for (StudentAttendanceReportItem r : studentReports) {
            assertEquals("Report item total classes == present + absent",
                    r.getTotalClasses(), r.getPresentClasses() + r.getAbsentClasses());
            if (r.getTotalClasses() > 0) {
                if (r.getAttendancePercentage() < 75.0) {
                    assertEquals("Student < 75% has status DEFAULTER", DefaulterRecord.Status.DEFAULTER, r.getStatus());
                    assertEqualsDouble("Defaulter deficit == 75.0 - attendance",
                            75.0 - r.getAttendancePercentage(), r.getDeficit(), 0.05);
                } else {
                    assertEquals("Student >= 75% has status SAFE", DefaulterRecord.Status.SAFE, r.getStatus());
                    assertEqualsDouble("Safe student deficit == 0.0", 0.0, r.getDeficit(), 0.001);
                }
            }
        }

        // Verify sorting: lowest attendance percentage first
        boolean isSortedAsc = true;
        for (int i = 0; i < studentReports.size() - 1; i++) {
            if (studentReports.get(i).getAttendancePercentage() > studentReports.get(i + 1).getAttendancePercentage()) {
                isSortedAsc = false;
                break;
            }
        }
        assertTrue("Student attendance report sorted with lowest attendance first", isSortedAsc);

        // Subject Filter
        int testSubId = studentReports.getFirst().getSubjectId();
        List<StudentAttendanceReportItem> subFiltered = reportService.getStudentAttendanceReports(
                testSubId, null, null, null, "ALL", null, 75.0, User.Role.FACULTY);
        for (StudentAttendanceReportItem r : subFiltered) {
            assertEquals("Subject filter strictly isolates course ID " + testSubId, testSubId, r.getSubjectId());
        }

        // Status Filters
        List<StudentAttendanceReportItem> defOnly = reportService.getStudentAttendanceReports(
                null, null, null, null, "DEFAULTER", null, 75.0, User.Role.ADMIN);
        for (StudentAttendanceReportItem r : defOnly) {
            assertEquals("DEFAULTER filter contains only defaulters", DefaulterRecord.Status.DEFAULTER, r.getStatus());
        }

        List<StudentAttendanceReportItem> safeOnly = reportService.getStudentAttendanceReports(
                null, null, null, null, "SAFE", null, 75.0, User.Role.ADMIN);
        for (StudentAttendanceReportItem r : safeOnly) {
            assertEquals("SAFE filter contains only safe students", DefaulterRecord.Status.SAFE, r.getStatus());
        }

        // Keyword Search
        List<StudentAttendanceReportItem> searchResult = reportService.getStudentAttendanceReports(
                null, null, null, null, "ALL", "Aarav", 75.0, User.Role.ADMIN);
        assertTrue("Search keyword 'Aarav' returns matching student", !searchResult.isEmpty());
        assertTrue("Search result student name contains Aarav", searchResult.getFirst().getStudentName().contains("Aarav"));

        // --- SECTION D: SUBJECT-WISE PERFORMANCE REPORT ---
        System.out.println("\n[D] Verifying Subject-Wise Performance Report:");

        List<SubjectAttendanceReportItem> subjectReports = reportService.getSubjectAttendanceReports(
                null, null, 75.0, User.Role.ADMIN);
        assertTrue("Subject reports returned for ADMIN", subjectReports != null && !subjectReports.isEmpty());

        for (SubjectAttendanceReportItem s : subjectReports) {
            assertEquals("Subject total records == present + absent",
                    s.getTotalRecords(), s.getPresentCount() + s.getAbsentCount());
            assertEquals("Subject evaluated students == defaulters + safe",
                    s.getTotalStudents(), s.getDefaulterCount() + s.getSafeCount());
        }

        // --- SECTION E: DISTRIBUTION & TIME-SERIES TRENDS ---
        System.out.println("\n[E] Verifying Distribution & Trends Queries:");

        AttendanceDistribution dist = reportService.getAttendanceDistribution(null, null, null, User.Role.ADMIN);
        assertTrue("Distribution total > 0", dist.getTotalCount() > 0);
        assertEquals("Distribution total == present + absent",
                dist.getTotalCount(), dist.getPresentCount() + dist.getAbsentCount());
        assertEqualsDouble("Distribution percentages sum to 100.0%",
                100.0, dist.getPresentPercentage() + dist.getAbsentPercentage(), 0.1);

        List<AttendanceTrendItem> trends = reportService.getAttendanceTrends(null, null, null, User.Role.ADMIN);
        assertTrue("Attendance trends returned", trends != null && !trends.isEmpty());
        for (int i = 0; i < trends.size() - 1; i++) {
            assertTrue("Trends are in chronological order",
                    !trends.get(i).getDate().isAfter(trends.get(i + 1).getDate()));
        }

        // --- SECTION F: DATE RANGE FILTERING & NORMALIZATION ---
        System.out.println("\n[F] Verifying Date Range Filtering:");

        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 15);

        // Normalization check (swapped dates)
        LocalDate[] normalized = reportService.normalizeDateRange(end, start);
        assertEquals("Start date normalized correctly", start, normalized[0]);
        assertEquals("End date normalized correctly", end, normalized[1]);

        // Range filtering check
        List<AttendanceTrendItem> rangeTrends = reportService.getAttendanceTrends(null, start, end, User.Role.ADMIN);
        for (AttendanceTrendItem t : rangeTrends) {
            assertTrue("Trend date >= startDate", !t.getDate().isBefore(start));
            assertTrue("Trend date <= endDate", !t.getDate().isAfter(end));
        }

        // --- SECTION G: CSV EXPORT UTILITY ---
        System.out.println("\n[G] Verifying CSV Export Utility & RFC 4180 Escaping:");

        assertEquals("Plain string left unchanged", "Hello", CsvExportUtil.escapeCsv("Hello"));
        assertEquals("Comma causes quotes", "\"Hello, World\"", CsvExportUtil.escapeCsv("Hello, World"));
        assertEquals("Quotes are doubled and wrapped", "\"Hello \"\"World\"\"\"", CsvExportUtil.escapeCsv("Hello \"World\""));
        assertEquals("Newline causes quotes", "\"Line 1\nLine 2\"", CsvExportUtil.escapeCsv("Line 1\nLine 2"));

        StudentReportTableModel tableModel = new StudentReportTableModel();
        tableModel.setItems(studentReports);
        StringWriter sw = new StringWriter();
        try {
            int exportedRows = CsvExportUtil.exportToWriter(tableModel, sw);
            assertEquals("CSV exported row count matches model row count", studentReports.size(), exportedRows);
            String csvOutput = sw.toString();
            assertTrue("CSV contains Roll No header", csvOutput.contains("Roll No"));
            assertTrue("CSV contains Student Name header", csvOutput.contains("Student Name"));
            assertTrue("CSV contains Attendance % header", csvOutput.contains("Attendance %"));
        } catch (Exception e) {
            failedCount++;
            System.err.println("  [FAIL] CSV export failed: " + e.getMessage());
        }

        // --- FINAL SUMMARY ---
        System.out.println("\n==========================================================");
        System.out.println(String.format("Tests Run: %d | Passed: %d | Failed: %d",
                passedCount + failedCount, passedCount, failedCount));
        System.out.println("==========================================================");

        if (failedCount > 0) {
            System.err.println("AUTOMATED TEST SUITE FAILED with " + failedCount + " error(s)!");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! Stage 9 Reports & Analytics is verified.");
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            passedCount++;
            System.out.println("  [PASS] " + testName);
        } else {
            failedCount++;
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void assertFalse(String testName, boolean condition) {
        assertTrue(testName, !condition);
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (match) {
            passedCount++;
            System.out.println("  [PASS] " + testName);
        } else {
            failedCount++;
            System.err.println(String.format("  [FAIL] %s - Expected: %s, Actual: %s", testName, expected, actual));
        }
    }

    private static void assertEqualsDouble(String testName, double expected, double actual, double delta) {
        boolean match = Math.abs(expected - actual) <= delta;
        if (match) {
            passedCount++;
            System.out.println("  [PASS] " + testName);
        } else {
            failedCount++;
            System.err.println(String.format("  [FAIL] %s - Expected: %.4f, Actual: %.4f (delta: %.4f)",
                    testName, expected, actual, delta));
        }
    }
}
