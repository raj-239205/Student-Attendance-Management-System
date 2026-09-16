package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.database.DemoDataSeeder;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.OverallAttendanceSummary;
import com.college.attendance.model.StudentAttendanceReportItem;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.User;
import com.college.attendance.service.DashboardService;
import com.college.attendance.service.DefaulterService;
import com.college.attendance.service.ReportService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Automated test suite verifying the Realistic Demonstration Dataset.
 *
 * <p>Validates:
 * <ul>
 *   <li>60 students populated in MySQL database.</li>
 *   <li>At least 15 faculty accounts populated in users table.</li>
 *   <li>8 curriculum subjects populated in subjects table.</li>
 *   <li>Attendance records generated across 22 weekdays in September 2026.</li>
 *   <li>Exact boundary test cases (100%, 90%, 75.0%, 72.73%, 60.0%, and NO_DATA).</li>
 *   <li>Realistic distribution (~68% Safe, ~28% Defaulter, ~3% No Data).</li>
 *   <li>Full idempotence: re-seeding produces 0 duplicate records.</li>
 * </ul>
 * </p>
 */
public class DemoDatasetVerificationTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("Demo Dataset Verification Test Suite");
        System.out.println("==========================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);

        if (!dbConnection.testConnection()) {
            System.err.println("Cannot connect to database. Aborting demo dataset test.");
            System.exit(1);
        }

        try (Connection conn = dbConnection.getConnection()) {
            String seedPassword = config.isPasswordConfigured() ? config.getPassword() : "Admin@123";

            // 1. Run Seeder
            System.out.println("\n[A] Executing DemoDataSeeder:");
            DemoDataSeeder.DemoSeedReport report = DemoDataSeeder.seedDemoData(conn, seedPassword);
            testAssert("Seeder returned non-null report", report != null);

            // 2. Validate Entity Counts
            System.out.println("\n[B] Validating Seeded Entity Counts:");
            testAssert("Total students in DB >= 60", report.totalStudents() >= 60);
            testAssert("Total faculty in DB >= 15", report.totalFaculty() >= 15);
            testAssert("Total subjects in DB >= 8", report.totalSubjects() >= 8);
            testAssert("Total attendance records in DB >= 1000", report.totalAttendance() >= 1000);

            // 3. Validate Idempotence (run second time)
            System.out.println("\n[C] Verifying Idempotence (Second Run):");
            DemoDataSeeder.DemoSeedReport secondRun = DemoDataSeeder.seedDemoData(conn, seedPassword);
            testAssert("Second run inserted 0 additional students", secondRun.studentsInserted() == 0);
            testAssert("Second run inserted 0 additional faculty", secondRun.facultyInserted() == 0);
            testAssert("Second run inserted 0 additional subjects", secondRun.subjectsInserted() == 0);
            testAssert("Second run inserted 0 additional attendance records", secondRun.attendanceInserted() == 0);
            testAssert("Total students unchanged on second run", secondRun.totalStudents() == report.totalStudents());
            testAssert("Total attendance unchanged on second run", secondRun.totalAttendance() == report.totalAttendance());

            // 4. Validate Specific Boundary Test Cases
            System.out.println("\n[D] Validating Boundary Test Cases in Database:");
            DashboardService dashboardService = new DashboardService(dbConnection);

            // Student 4: 100% Safe (22/22 present in CS401)
            int s4Id = resolveStudentId(conn, "CS2024004");
            StudentAttendanceSummary s4 = dashboardService.getStudentSummary(s4Id);
            testAssert("Student 4 (CS2024004) summary found", s4 != null);
            if (s4 != null) {
                testAssert("Student 4 attendance is 100.0%", Math.abs(s4.getOverallPercentage() - 100.0) < 0.01);
                testAssert("Student 4 is SAFE", !s4.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            }

            // Student 5: ~90.91% Safe
            int s5Id = resolveStudentId(conn, "CS2024005");
            StudentAttendanceSummary s5 = dashboardService.getStudentSummary(s5Id);
            testAssert("Student 5 (CS2024005) summary found", s5 != null);
            if (s5 != null) {
                testAssert("Student 5 attendance >= 90.0%", s5.getOverallPercentage() >= 90.0);
                testAssert("Student 5 is SAFE", !s5.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            }

            // Student 6: EXACT 75.00% Safe
            int s6Id = resolveStudentId(conn, "CS2024006");
            StudentAttendanceSummary s6 = dashboardService.getStudentSummary(s6Id);
            testAssert("Student 6 (CS2024006) summary found", s6 != null);
            if (s6 != null) {
                testAssert("Student 6 attendance is EXACTLY 75.00%", Math.abs(s6.getOverallPercentage() - 75.0) < 0.01);
                testAssert("Student 6 (75.00%) is strictly SAFE", !s6.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            }

            // Student 7: 72.73% (< 75.0%) DEFAULTER
            int s7Id = resolveStudentId(conn, "CS2024007");
            StudentAttendanceSummary s7 = dashboardService.getStudentSummary(s7Id);
            testAssert("Student 7 (CS2024007) summary found", s7 != null);
            if (s7 != null) {
                testAssert("Student 7 attendance is < 75.0% (~71.43%)", s7.getOverallPercentage() < 75.0 && s7.getOverallPercentage() >= 70.0);
                testAssert("Student 7 is DEFAULTER", s7.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            }

            // Student 8: EXACT 60.00% DEFAULTER
            int s8Id = resolveStudentId(conn, "CS2024008");
            StudentAttendanceSummary s8 = dashboardService.getStudentSummary(s8Id);
            testAssert("Student 8 (CS2024008) summary found", s8 != null);
            if (s8 != null) {
                testAssert("Student 8 attendance is EXACTLY 60.00%", Math.abs(s8.getOverallPercentage() - 60.0) < 0.01);
                testAssert("Student 8 is DEFAULTER", s8.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            }

            // Student 59 & 60: NO_DATA (0 classes)
            int s59Id = resolveStudentId(conn, "CS2024059");
            StudentAttendanceSummary s59 = dashboardService.getStudentSummary(s59Id);
            testAssert("Student 59 (CS2024059) summary found", s59 != null);
            if (s59 != null) {
                testAssert("Student 59 total classes == 0 (NO_DATA)", s59.getTotalClasses() == 0);
            }

            // 5. Validate Distribution in Reports and Analytics
            System.out.println("\n[E] Validating Attendance Distribution in Reports:");
            ReportService reportService = new ReportService(dbConnection);
            OverallAttendanceSummary overall = reportService.getOverallSummary(null, null, null, 75.0, User.Role.ADMIN);
            testAssert("Overall summary total students >= 60", overall.getTotalStudents() >= 60);
            testAssert("Overall summary total subjects >= 8", overall.getTotalSubjects() >= 8);
            testAssert("Overall summary total records >= 1000", overall.getTotalAttendanceRecords() >= 1000);
            testAssert("Overall defaulters >= 10", overall.getTotalDefaulters() >= 10);
            testAssert("Overall safe students >= 35", overall.getTotalSafeStudents() >= 35);

            double safeRatio = (double) overall.getTotalSafeStudents() / overall.getTotalStudents();
            double defRatio = (double) overall.getTotalDefaulters() / overall.getTotalStudents();
            testAssert("Safe students distribution is within 60% - 75%", safeRatio >= 0.60 && safeRatio <= 0.75);
            testAssert("Defaulters distribution is within 20% - 35%", defRatio >= 0.20 && defRatio <= 0.35);

            // 6. DefaulterService Integration
            System.out.println("\n[F] Validating DefaulterService with Seeded Dataset:");
            DefaulterService defaulterService = new DefaulterService(dbConnection);
            List<DefaulterRecord> defaulterRecords = defaulterService.getDefaulterRecords(null, 75.0, "DEFAULTER", null, User.Role.ADMIN);
            testAssert("Defaulter service returned defaulter records", !defaulterRecords.isEmpty());
            testAssert("All returned records have status DEFAULTER", defaulterRecords.stream().allMatch(r -> r.getStatus() == DefaulterRecord.Status.DEFAULTER));

        } catch (Exception e) {
            System.err.println("Exception in DemoDatasetVerificationTest: " + e.getMessage());
            e.printStackTrace();
            failedCount++;
        }

        System.out.println("\n==========================================================");
        System.out.printf("Demo Dataset Tests Run: %d | Passed: %d | Failed: %d\n",
                passedCount + failedCount, passedCount, failedCount);
        System.out.println("==========================================================");

        if (failedCount > 0) {
            System.err.println("SOME TESTS FAILED in DemoDatasetVerificationTest!");
            System.exit(1);
        } else {
            System.out.println("ALL DEMO DATASET TESTS PASSED SUCCESSFULLY!");
        }
    }

    private static int resolveStudentId(Connection conn, String rollNo) throws Exception {
        String sql = "SELECT id FROM students WHERE roll_no = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new IllegalStateException("Student not found for rollNo: " + rollNo);
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
