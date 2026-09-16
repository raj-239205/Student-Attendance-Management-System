package com.college.attendance;

/**
 * Master automated test runner orchestrating all test suites across Stages 1–10:
 * <ul>
 *   <li>Stage 2 & 3.1: Authentication & Account Linkage (AuthenticationTest)</li>
 *   <li>Stage 5: Dashboard & Role-Based Authorization (DashboardAuthorizationTest)</li>
 *   <li>Stage 5.2: Student Management & CRUD (StudentManagementTest)</li>
 *   <li>Stage 6: Subject Management & Faculty Assignment (SubjectManagementTest)</li>
 *   <li>Stage 7: Attendance Marking & Transactions (AttendanceManagementTest)</li>
 *   <li>Stage 8: Automatic Defaulter Management (DefaulterManagementTest)</li>
 *   <li>Stage 9: Reports & Analytics (ReportsAnalyticsTest)</li>
 *   <li>Stage 10: Final Polish, Integration & System Verification (Stage10SystemPolishTest)</li>
 * </ul>
 */
public class AllTestsSuite {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        System.out.println("================================================================================");
        System.out.println("  COLLEGE ATTENDANCE MANAGEMENT SYSTEM — FULL REGRESSION & VERIFICATION SUITE   ");
        System.out.println("================================================================================");

        try {
            // 1. Authentication & Security Linkage Test Suite
            System.out.println("\n>>> [1/8] Running Authentication & Session Security Suite...");
            AuthenticationTest.main(args);

            // 2. Dashboard Authorization Test Suite
            System.out.println("\n>>> [2/8] Running Dashboard Authorization Suite...");
            DashboardAuthorizationTest.main(args);

            // 3. Student Management Test Suite
            System.out.println("\n>>> [3/8] Running Student Management Suite...");
            StudentManagementTest.main(args);

            // 4. Subject Management Test Suite
            System.out.println("\n>>> [4/8] Running Subject Management Suite...");
            SubjectManagementTest.main(args);

            // 5. Attendance Management Test Suite
            System.out.println("\n>>> [5/8] Running Attendance Management Suite...");
            AttendanceManagementTest.main(args);

            // 6. Defaulter Management Test Suite
            System.out.println("\n>>> [6/8] Running Defaulter Management Suite...");
            DefaulterManagementTest.main(args);

            // 7. Reports & Analytics Test Suite
            System.out.println("\n>>> [7/8] Running Reports & Analytics Suite...");
            ReportsAnalyticsTest.main(args);

            // 8. Stage 10 Final Polish & System Verification
            System.out.println("\n>>> [8/9] Running Stage 10 System Polish & Integration Suite...");
            Stage10SystemPolishTest.main(args);

            // 9. Realistic Demonstration Dataset Verification
            System.out.println("\n>>> [9/9] Running Demo Dataset Verification Suite...");
            DemoDatasetVerificationTest.main(args);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("\n================================================================================");
            System.out.println("  ALL SUITES (STAGES 1–10 + DEMO DATASET) COMPLETED SUCCESSFULLY IN " + elapsed + " ms  ");
            System.out.println("  100% PASS RATE — ZERO FAILURES — ZERO ERRORS                                  ");
            System.out.println("================================================================================");

            System.exit(0);

        } catch (Throwable t) {
            System.err.println("\n[FATAL ERROR] Automated test execution failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        } finally {
            try {
                // Gracefully shut down MySQL driver background daemon cleanup thread
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {}
        }
    }
}
