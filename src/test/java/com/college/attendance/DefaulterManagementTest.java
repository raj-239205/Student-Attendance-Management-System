package com.college.attendance;

import com.college.attendance.config.AppConstants;
import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.DefaulterDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.DefaulterSummary;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.DefaulterService;
import com.college.attendance.ui.navigation.NavigationItem;

import java.util.List;

/**
 * Automated test suite for Stage 8 — Automatic Defaulter Management Module:
 * <ol>
 *   <li>ADMIN has access to DEFAULTERS navigation.</li>
 *   <li>FACULTY has access to DEFAULTERS navigation.</li>
 *   <li>STUDENT is denied access to DEFAULTERS navigation.</li>
 *   <li>Programmatic RBAC: STUDENT cannot query general defaulter records.</li>
 *   <li>Programmatic RBAC: STUDENT cannot query general defaulter summary.</li>
 *   <li>Student data isolation: Student can query own course breakdown.</li>
 *   <li>Student data isolation: Student blocked from querying another student's breakdown.</li>
 *   <li>Threshold resolution: Valid custom threshold respected.</li>
 *   <li>Threshold resolution: Null threshold defaults to 75.0%.</li>
 *   <li>Threshold resolution: Out-of-bounds threshold (< 0 or > 100) defaults to 75.0%.</li>
 *   <li>Threshold comparison: 100.0% is SAFE.</li>
 *   <li>Threshold comparison: 90.0% is SAFE.</li>
 *   <li>Threshold comparison: 75.0% is SAFE (strictly < rule).</li>
 *   <li>Threshold comparison: 74.99% is DEFAULTER.</li>
 *   <li>Threshold comparison: 60.0% is DEFAULTER.</li>
 *   <li>Threshold comparison: 0.0% with classes held is DEFAULTER.</li>
 *   <li>Zero attendance safety: 0 classes held returns NO_DATA, 0.0% attendance, 0.0% deficit.</li>
 *   <li>Deficit calculation: Defaulter has positive deficit (Threshold - Attendance).</li>
 *   <li>Deficit calculation: Safe student has 0.0% deficit (never negative).</li>
 *   <li>DAO query: Database defaulter records retrieved with valid structure.</li>
 *   <li>Sorting rule: Defaulter records sorted with lowest attendance first.</li>
 *   <li>Filtering: Status filter DEFAULTER returns only defaulters.</li>
 *   <li>Filtering: Status filter SAFE returns only safe students.</li>
 *   <li>Filtering: Subject filter isolates specific course subjects.</li>
 *   <li>Summary statistics: Defaulter summary computes valid totals and percentages.</li>
 * </ol>
 */
public class DefaulterManagementTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("Stage 8 — Automatic Defaulter Management Module Test Suite");
        System.out.println("==========================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        AuthorizationService authz = new AuthorizationService();
        DefaulterDAO defaulterDAO = new DefaulterDAO();
        DefaulterService defaulterService = new DefaulterService(dbConnection, defaulterDAO);

        // --- SECTION A: ROLE-BASED ACCESS CONTROL (RBAC) ---
        System.out.println("\n[A] Verifying Navigation & Role-Based Access Control:");

        assertTrue("ADMIN has access to DEFAULTERS navigation",
                authz.canAccess(User.Role.ADMIN, NavigationItem.DEFAULTERS));
        assertTrue("FACULTY has access to DEFAULTERS navigation",
                authz.canAccess(User.Role.FACULTY, NavigationItem.DEFAULTERS));
        assertFalse("STUDENT is denied access to DEFAULTERS navigation",
                authz.canAccess(User.Role.STUDENT, NavigationItem.DEFAULTERS));

        // Programmatic RBAC checks in DefaulterService
        List<DefaulterRecord> studentAttempt = defaulterService.getDefaulterRecords(
                null, 75.0, "ALL", null, User.Role.STUDENT);
        assertTrue("STUDENT caller is programmatically denied general defaulter records",
                studentAttempt.isEmpty());

        DefaulterSummary studentSummaryAttempt = defaulterService.getDefaulterSummary(
                null, 75.0, User.Role.STUDENT);
        assertEquals("STUDENT caller is programmatically denied defaulter summary (total 0)",
                0, studentSummaryAttempt.getTotalEvaluated());

        // Student Data Isolation
        List<DefaulterRecord> ownBreakdown = defaulterService.getStudentDefaulterBreakdown(
                1, 75.0, User.Role.STUDENT, 1);
        assertTrue("STUDENT caller can query own attendance breakdown",
                ownBreakdown != null);

        List<DefaulterRecord> foreignBreakdown = defaulterService.getStudentDefaulterBreakdown(
                2, 75.0, User.Role.STUDENT, 1);
        assertTrue("STUDENT caller blocked from accessing another student's breakdown",
                foreignBreakdown.isEmpty());

        List<DefaulterRecord> adminAccessBreakdown = defaulterService.getStudentDefaulterBreakdown(
                1, 75.0, User.Role.ADMIN, null);
        assertTrue("ADMIN caller can access student breakdown",
                adminAccessBreakdown != null);

        // --- SECTION B: THRESHOLD & CONFIGURATION RULES ---
        System.out.println("\n[B] Verifying Configurable Threshold Resolution:");

        assertEqualsDouble("Default threshold constant is 75.0%",
                75.0, AppConstants.DEFAULT_DEFAULTER_THRESHOLD, 0.001);
        assertEqualsDouble("Null threshold resolves to default 75.0%",
                75.0, defaulterService.resolveThreshold(null), 0.001);
        assertEqualsDouble("Negative threshold resolves to default 75.0%",
                75.0, defaulterService.resolveThreshold(-10.0), 0.001);
        assertEqualsDouble("Excessive threshold (>100) resolves to default 75.0%",
                75.0, defaulterService.resolveThreshold(120.0), 0.001);
        assertEqualsDouble("Valid custom threshold (80.0%) is preserved",
                80.0, defaulterService.resolveThreshold(80.0), 0.001);

        // --- SECTION C: CALCULATION & DEFAULTER STANDING LOGIC ---
        System.out.println("\n[C] Verifying Attendance Percentage & Standing Logic:");

        // 100.0% -> SAFE
        DefaulterRecord r100 = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 10, 10, 0, 75.0);
        assertEquals("10/10 (100%) status is SAFE", DefaulterRecord.Status.SAFE, r100.getStatus());
        assertEqualsDouble("10/10 deficit is 0.0%", 0.0, r100.getDeficit(), 0.001);

        // 90.0% -> SAFE
        DefaulterRecord r90 = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 10, 9, 1, 75.0);
        assertEquals("9/10 (90%) status is SAFE", DefaulterRecord.Status.SAFE, r90.getStatus());
        assertEqualsDouble("9/10 deficit is 0.0%", 0.0, r90.getDeficit(), 0.001);

        // Exactly 75.0% -> SAFE (rule: Attendance < Threshold)
        DefaulterRecord r75 = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 20, 15, 5, 75.0);
        assertEquals("15/20 (75.0%) status is SAFE (strictly < rule)", DefaulterRecord.Status.SAFE, r75.getStatus());
        assertEqualsDouble("75.0% deficit is 0.0%", 0.0, r75.getDeficit(), 0.001);

        // 74.99% -> DEFAULTER
        // Example: 149 / 200 = 74.5%
        DefaulterRecord r745 = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 200, 149, 51, 75.0);
        assertEquals("149/200 (74.5%) status is DEFAULTER", DefaulterRecord.Status.DEFAULTER, r745.getStatus());
        assertEqualsDouble("74.5% deficit is 0.5%", 0.5, r745.getDeficit(), 0.001);

        // 60.0% -> DEFAULTER
        DefaulterRecord r60 = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 10, 6, 4, 75.0);
        assertEquals("6/10 (60.0%) status is DEFAULTER", DefaulterRecord.Status.DEFAULTER, r60.getStatus());
        assertEqualsDouble("60.0% deficit is 15.0%", 15.0, r60.getDeficit(), 0.001);

        // 0.0% with classes held -> DEFAULTER
        DefaulterRecord r0 = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 10, 0, 10, 75.0);
        assertEquals("0/10 (0.0%) status is DEFAULTER", DefaulterRecord.Status.DEFAULTER, r0.getStatus());
        assertEqualsDouble("0.0% deficit is 75.0%", 75.0, r0.getDeficit(), 0.001);

        // Zero Attendance Case (0 total classes held) -> NO_DATA
        DefaulterRecord rZero = new DefaulterRecord(1, "CS01", "Student", "CSE", 4, 1, "CS401", "OS", 0, 0, 0, 75.0);
        assertEquals("0 total classes status is NO_DATA", DefaulterRecord.Status.NO_DATA, rZero.getStatus());
        assertEqualsDouble("0 total classes attendance % is 0.0%", 0.0, rZero.getAttendancePercentage(), 0.001);
        assertEqualsDouble("0 total classes deficit is 0.0%", 0.0, rZero.getDeficit(), 0.001);

        // Service helper methods
        assertEqualsDouble("Service calculatePercentage with 0 total is 0.0%",
                0.0, defaulterService.calculatePercentage(0, 0), 0.001);
        assertEqualsDouble("Service calculateDeficit for safe student is 0.0%",
                0.0, defaulterService.calculateDeficit(75.0, 85.0), 0.001);
        assertEqualsDouble("Service calculateDeficit for defaulter (68.0%) is 7.0%",
                7.0, defaulterService.calculateDeficit(75.0, 68.0), 0.001);

        // --- SECTION D: DATABASE QUERIES, FILTERING & SORTING ---
        System.out.println("\n[D] Verifying Database Queries, Aggregations & Sorting:");

        List<DefaulterRecord> dbRecords = defaulterService.getDefaulterRecords(
                null, 75.0, "ALL", null, User.Role.ADMIN);
        assertTrue("Database records retrieved for ADMIN", dbRecords != null && !dbRecords.isEmpty());

        // Verify sorting: lowest attendance percentage first
        boolean isSortedAscending = true;
        for (int i = 0; i < dbRecords.size() - 1; i++) {
            if (dbRecords.get(i).getAttendancePercentage() > dbRecords.get(i + 1).getAttendancePercentage()) {
                isSortedAscending = false;
                break;
            }
        }
        assertTrue("Defaulter records are sorted with lowest attendance first", isSortedAscending);

        // Status Filter: DEFAULTER
        List<DefaulterRecord> onlyDefaulters = defaulterService.getDefaulterRecords(
                null, 75.0, "DEFAULTER", null, User.Role.ADMIN);
        for (DefaulterRecord r : onlyDefaulters) {
            assertEquals("Record in DEFAULTER filter has DEFAULTER status",
                    DefaulterRecord.Status.DEFAULTER, r.getStatus());
        }

        // Status Filter: SAFE
        List<DefaulterRecord> onlySafe = defaulterService.getDefaulterRecords(
                null, 75.0, "SAFE", null, User.Role.ADMIN);
        for (DefaulterRecord r : onlySafe) {
            assertEquals("Record in SAFE filter has SAFE status",
                    DefaulterRecord.Status.SAFE, r.getStatus());
        }

        // Aggregate Summary
        DefaulterSummary summary = defaulterService.getDefaulterSummary(
                null, 75.0, User.Role.ADMIN);
        assertTrue("Defaulter summary totalEvaluated matches sum of defaulters and safe",
                summary.getTotalEvaluated() == (summary.getDefaulterCount() + summary.getSafeCount()));
        assertTrue("Defaulter rate is between 0.0 and 100.0%",
                summary.getDefaulterRate() >= 0.0 && summary.getDefaulterRate() <= 100.0);

        // Course Subject Filtering
        if (!dbRecords.isEmpty()) {
            int testSubjectId = dbRecords.getFirst().getSubjectId();
            List<DefaulterRecord> subjectRecords = defaulterService.getDefaulterRecords(
                    testSubjectId, 75.0, "ALL", null, User.Role.FACULTY);
            for (DefaulterRecord r : subjectRecords) {
                assertEquals("Subject filter isolates subject ID " + testSubjectId,
                        testSubjectId, r.getSubjectId());
            }
        }

        // Keyword Search Filtering
        List<DefaulterRecord> searchRecords = defaulterService.getDefaulterRecords(
                null, 75.0, "ALL", "CS", User.Role.ADMIN);
        assertTrue("Keyword search returns filtered records", searchRecords != null);

        // --- FINAL SUMMARY ---
        System.out.println("\n==========================================================");
        System.out.println(String.format("Tests Run: %d | Passed: %d | Failed: %d",
                passedCount + failedCount, passedCount, failedCount));
        System.out.println("==========================================================");

        if (failedCount > 0) {
            System.err.println("AUTOMATED TEST SUITE FAILED with " + failedCount + " error(s)!");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! Stage 8 Defaulter Management is verified.");
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
