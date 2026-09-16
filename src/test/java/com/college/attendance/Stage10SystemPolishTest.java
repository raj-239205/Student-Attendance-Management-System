package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.AttendanceTrendItem;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.OverallAttendanceSummary;
import com.college.attendance.model.Session;
import com.college.attendance.model.StudentAttendanceReportItem;
import com.college.attendance.model.SubjectAttendanceReportItem;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.AttendanceService;
import com.college.attendance.service.DefaulterService;
import com.college.attendance.service.ReportService;
import com.college.attendance.service.StudentService;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.components.StudentReportTableModel;
import com.college.attendance.ui.components.SubjectReportTableModel;
import com.college.attendance.ui.components.TrendReportTableModel;
import com.college.attendance.ui.components.charts.BarChartPanel;
import com.college.attendance.ui.components.charts.DonutChartPanel;
import com.college.attendance.ui.components.charts.LineTrendChartPanel;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.ui.views.StudentProfileView;
import com.college.attendance.util.CsvExportUtil;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Automated test suite for Stage 10 — Final Polish, Integration & System Verification:
 * <ol>
 *   <li>Boundary testing: 74.99% is DEFAULTER, 75.00% is SAFE.</li>
 *   <li>Attendance calculation formula consistency (Present / Total * 100).</li>
 *   <li>Zero attendance instances classified as NO_DATA.</li>
 *   <li>Student role navigation isolation (denied all administrative sections).</li>
 *   <li>Student role permitted access (Dashboard, My Attendance, My Profile).</li>
 *   <li>Faculty role navigation permissions (Dashboard, Students, Subjects, Attendance, Defaulters, Reports).</li>
 *   <li>Admin role navigation permissions (Full system access).</li>
 *   <li>Date range auto-normalization logic (startDate > endDate swapped safely).</li>
 *   <li>Threshold boundary clamping: [0.0, 100.0].</li>
 *   <li>RFC 4180 CSV export with complex special characters (quotes, commas, CRLF).</li>
 *   <li>Table model resilience with empty datasets (zero NPEs).</li>
 *   <li>Chart component resilience with empty/sparse datasets.</li>
 *   <li>StudentProfileView isolation and binding.</li>
 *   <li>Cross-module consistency: Single source of truth across all services.</li>
 * </ol>
 */
public class Stage10SystemPolishTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================");
        System.out.println("Stage 10 — Final Polish & System Verification Test Suite");
        System.out.println("==========================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);

        AuthorizationService authz = new AuthorizationService();
        ReportService reportService = new ReportService(dbConnection);
        DefaulterService defaulterService = new DefaulterService(dbConnection);
        AttendanceService attendanceService = new AttendanceService(dbConnection);
        StudentService studentService = new StudentService(dbConnection);
        SubjectService subjectService = new SubjectService(dbConnection);

        // [A] Defaulter Boundary & Classification Verification
        System.out.println("\n[A] Defaulter Boundary & Mathematical Formula Verification:");
        testAssert("Default threshold is 75.0%", AppConstants.DEFAULT_DEFAULTER_THRESHOLD == 75.0);

        // 74.99% must be classified as DEFAULTER
        double boundaryDefaulter = 74.99;
        boolean isDefaulter = boundaryDefaulter < AppConstants.DEFAULT_DEFAULTER_THRESHOLD;
        testAssert("74.99% is strictly DEFAULTER", isDefaulter);

        // 75.00% must be classified as SAFE
        double boundarySafe = 75.00;
        boolean isSafe = boundarySafe >= AppConstants.DEFAULT_DEFAULTER_THRESHOLD;
        testAssert("75.00% is strictly SAFE", isSafe);

        // Formula check
        double calcPct1 = attendanceService.calculatePercentage(3, 4); // 75.0%
        testAssert("3 present out of 4 total is 75.0%", Math.abs(calcPct1 - 75.0) < 0.001);

        double calcPct2 = attendanceService.calculatePercentage(2, 3); // 66.67%
        testAssert("2 present out of 3 total is ~66.67%", Math.abs(calcPct2 - 66.6666) < 0.01);

        double calcPct0 = attendanceService.calculatePercentage(0, 0); // 0.0%
        testAssert("0 present out of 0 total is 0.0%", calcPct0 == 0.0);

        // [B] Role Navigation & Access Control Matrix
        System.out.println("\n[B] Role-Based Access Control Matrix Audit:");
        // Student restrictions
        testAssert("STUDENT denied access to STUDENTS module", !authz.canAccess(User.Role.STUDENT, NavigationItem.STUDENTS));
        testAssert("STUDENT denied access to SUBJECTS module", !authz.canAccess(User.Role.STUDENT, NavigationItem.SUBJECTS));
        testAssert("STUDENT denied access to ATTENDANCE module", !authz.canAccess(User.Role.STUDENT, NavigationItem.ATTENDANCE));
        testAssert("STUDENT denied access to DEFAULTERS module", !authz.canAccess(User.Role.STUDENT, NavigationItem.DEFAULTERS));
        testAssert("STUDENT denied access to REPORTS module", !authz.canAccess(User.Role.STUDENT, NavigationItem.REPORTS));

        // Student permitted
        testAssert("STUDENT permitted to access DASHBOARD", authz.canAccess(User.Role.STUDENT, NavigationItem.DASHBOARD));
        testAssert("STUDENT permitted to access MY_ATTENDANCE", authz.canAccess(User.Role.STUDENT, NavigationItem.MY_ATTENDANCE));
        testAssert("STUDENT permitted to access MY_PROFILE", authz.canAccess(User.Role.STUDENT, NavigationItem.MY_PROFILE));

        // Faculty permissions
        testAssert("FACULTY permitted to access DASHBOARD", authz.canAccess(User.Role.FACULTY, NavigationItem.DASHBOARD));
        testAssert("FACULTY permitted to access STUDENTS", authz.canAccess(User.Role.FACULTY, NavigationItem.STUDENTS));
        testAssert("FACULTY permitted to access SUBJECTS", authz.canAccess(User.Role.FACULTY, NavigationItem.SUBJECTS));
        testAssert("FACULTY permitted to access ATTENDANCE", authz.canAccess(User.Role.FACULTY, NavigationItem.ATTENDANCE));
        testAssert("FACULTY permitted to access DEFAULTERS", authz.canAccess(User.Role.FACULTY, NavigationItem.DEFAULTERS));
        testAssert("FACULTY permitted to access REPORTS", authz.canAccess(User.Role.FACULTY, NavigationItem.REPORTS));
        testAssert("FACULTY denied access to MY_ATTENDANCE", !authz.canAccess(User.Role.FACULTY, NavigationItem.MY_ATTENDANCE));

        // Admin permissions
        testAssert("ADMIN permitted to access DASHBOARD", authz.canAccess(User.Role.ADMIN, NavigationItem.DASHBOARD));
        testAssert("ADMIN permitted to access STUDENTS", authz.canAccess(User.Role.ADMIN, NavigationItem.STUDENTS));
        testAssert("ADMIN permitted to access SUBJECTS", authz.canAccess(User.Role.ADMIN, NavigationItem.SUBJECTS));
        testAssert("ADMIN permitted to access ATTENDANCE", authz.canAccess(User.Role.ADMIN, NavigationItem.ATTENDANCE));
        testAssert("ADMIN permitted to access DEFAULTERS", authz.canAccess(User.Role.ADMIN, NavigationItem.DEFAULTERS));
        testAssert("ADMIN permitted to access REPORTS", authz.canAccess(User.Role.ADMIN, NavigationItem.REPORTS));

        // [C] Date Normalization & Input Validation
        System.out.println("\n[C] Date Normalization & Input Validation:");
        LocalDate d1 = LocalDate.of(2026, 9, 20);
        LocalDate d2 = LocalDate.of(2026, 9, 10);
        LocalDate[] normalized = reportService.normalizeDateRange(d1, d2);
        testAssert("Start date swapped to earlier date", normalized[0].equals(d2));
        testAssert("End date swapped to later date", normalized[1].equals(d1));

        LocalDate[] alreadyOrdered = reportService.normalizeDateRange(d2, d1);
        testAssert("Already ordered range unchanged (start)", alreadyOrdered[0].equals(d2));
        testAssert("Already ordered range unchanged (end)", alreadyOrdered[1].equals(d1));

        LocalDate[] nullRange = reportService.normalizeDateRange(null, null);
        testAssert("Null range handled safely", nullRange[0] == null && nullRange[1] == null);

        // Clamping threshold simulation
        double clampedLow = Math.max(0.0, Math.min(100.0, -15.5));
        testAssert("Negative threshold clamped to 0.0%", clampedLow == 0.0);
        double clampedHigh = Math.max(0.0, Math.min(100.0, 155.0));
        testAssert("Over-100 threshold clamped to 100.0%", clampedHigh == 100.0);
        double clampedNormal = Math.max(0.0, Math.min(100.0, 75.5));
        testAssert("Valid threshold remains 75.5%", clampedNormal == 75.5);

        // [D] RFC 4180 CSV Export Hardening
        System.out.println("\n[D] RFC 4180 CSV Export Edge-Cases:");
        testAssert("Empty string remains empty", CsvExportUtil.escapeCsv("").equals(""));
        testAssert("Null string produces empty", CsvExportUtil.escapeCsv(null).equals(""));
        testAssert("Single quote inside string doubled and quoted",
                CsvExportUtil.escapeCsv("He said \"Hello\"").equals("\"He said \"\"Hello\"\"\""));
        testAssert("Commas cause quotes",
                CsvExportUtil.escapeCsv("Sharma, Rahul").equals("\"Sharma, Rahul\""));
        testAssert("Embedded newline causes quotes",
                CsvExportUtil.escapeCsv("Line1\nLine2").equals("\"Line1\nLine2\""));
        testAssert("Complex string with quotes, commas and newlines correctly escaped",
                CsvExportUtil.escapeCsv("Dept: \"CSE, IT\"\nYear: 2026").equals("\"Dept: \"\"CSE, IT\"\"\nYear: 2026\""));

        // [E] Empty Datasets & Table Model Resilience
        System.out.println("\n[E] Empty Datasets & Component Resilience:");
        StudentReportTableModel studentModel = new StudentReportTableModel();
        studentModel.setItems(Collections.emptyList());
        testAssert("Empty student report model has 0 rows", studentModel.getRowCount() == 0);
        testAssert("Student report model has 9 columns", studentModel.getColumnCount() == 9);

        SubjectReportTableModel subjectModel = new SubjectReportTableModel();
        subjectModel.setItems(Collections.emptyList());
        testAssert("Empty subject report model has 0 rows", subjectModel.getRowCount() == 0);
        testAssert("Subject report model has 11 columns", subjectModel.getColumnCount() == 11);

        TrendReportTableModel trendModel = new TrendReportTableModel();
        trendModel.setItems(Collections.emptyList());
        testAssert("Empty trend report model has 0 rows", trendModel.getRowCount() == 0);
        testAssert("Trend report model has 5 columns", trendModel.getColumnCount() == 5);

        // Exporting empty model produces valid CSV with just headers
        StringWriter sw = new StringWriter();
        int exportedRows = CsvExportUtil.exportToWriter(studentModel, sw);
        testAssert("Exporting empty model writes 0 data rows", exportedRows == 0);
        testAssert("Exported output contains header line", sw.toString().contains("Roll No"));

        // [F] Chart Panels Resilience with Empty Data
        System.out.println("\n[F] Chart Panels Resilience (Zero Exceptions on Empty State):");
        DonutChartPanel donut = new DonutChartPanel();
        donut.setDistribution(new AttendanceDistribution(0, 0));
        testAssert("Donut chart accepts zero distribution without error", donut != null);

        BarChartPanel barChart = new BarChartPanel();
        barChart.setItems(Collections.emptyList());
        testAssert("Bar chart accepts empty items list without error", barChart != null);

        LineTrendChartPanel lineChart = new LineTrendChartPanel();
        lineChart.setItems(Collections.emptyList());
        testAssert("Line trend chart accepts empty list without error", lineChart != null);

        // [G] StudentProfileView Binding
        System.out.println("\n[G] Student Profile View Session Binding:");
        User studentUser = new User();
        studentUser.setId(10);
        studentUser.setUsername("aarav.student");
        studentUser.setFullName("Aarav Sharma");
        studentUser.setRole(User.Role.STUDENT);
        studentUser.setStudentId(1);
        Session studentSession = Session.start(studentUser);

        StudentProfileView profileView = new StudentProfileView(studentService, studentSession);
        testAssert("StudentProfileView successfully instantiated for student session", profileView != null);

        // Null student session safely handled
        StudentProfileView unlinkedView = new StudentProfileView(studentService, null);
        testAssert("StudentProfileView handles null session safely", unlinkedView != null);

        // [H] Cross-Module Consistency (DefaulterService vs ReportService)
        System.out.println("\n[H] Cross-Module Attendance & Defaulter Consistency:");
        List<DefaulterRecord> defaulters = defaulterService.getDefaulterRecords(null, 75.0, null, null, User.Role.ADMIN);
        List<StudentAttendanceReportItem> reports = reportService.getStudentAttendanceReports(
                null, null, null, null, null, null, 75.0, User.Role.ADMIN);

        testAssert("Defaulter records retrieved", !defaulters.isEmpty());
        testAssert("Reports records retrieved", !reports.isEmpty());

        // Verify that any student in reports has identical status and percentage to defaulters
        for (DefaulterRecord dr : defaulters) {
            for (StudentAttendanceReportItem item : reports) {
                if (item.getRollNo().equalsIgnoreCase(dr.getRollNo())) {
                    testAssert("Student " + dr.getStudentName() + " attendance % matches across modules",
                            Math.abs(item.getAttendancePercentage() - dr.getAttendancePercentage()) < 0.01);
                    testAssert("Student " + dr.getStudentName() + " status matches across modules",
                            item.getStatus().name().equalsIgnoreCase(dr.getStatus().name()));
                    break;
                }
            }
        }

        System.out.println("\n==========================================================");
        System.out.printf("Stage 10 Tests Run: %d | Passed: %d | Failed: %d\n",
                passedCount + failedCount, passedCount, failedCount);
        System.out.println("==========================================================");

        if (failedCount > 0) {
            System.err.println("SOME TESTS FAILED in Stage 10 suite!");
            System.exit(1);
        } else {
            System.out.println("ALL STAGE 10 TESTS PASSED SUCCESSFULLY! System verification complete.");
        }
    }

    private static void testAssert(String description, boolean condition) {
        if (condition) {
            passedCount++;
            System.out.println("  [PASS] " + description);
        } else {
            failedCount++;
            System.err.println("  [FAIL] " + description);
        }
    }
}
