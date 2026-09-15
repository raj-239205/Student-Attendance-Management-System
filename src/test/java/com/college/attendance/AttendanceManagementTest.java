package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.AttendanceDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceRecord;
import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.AttendanceService;
import com.college.attendance.ui.navigation.NavigationItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated test suite for Stage 7 — Attendance Management Module:
 * <ol>
 *   <li>ADMIN can access ATTENDANCE navigation.</li>
 *   <li>FACULTY can access ATTENDANCE navigation.</li>
 *   <li>STUDENT is denied access to ATTENDANCE navigation.</li>
 *   <li>STUDENT can access MY_ATTENDANCE navigation.</li>
 *   <li>Retrieval of students for subject returns enrolled students.</li>
 *   <li>STUDENT cannot retrieve students for subject.</li>
 *   <li>Valid transactional batch attendance creation succeeds.</li>
 *   <li>Duplicate attendance marking gracefully updates existing record.</li>
 *   <li>Validation: null date rejected.</li>
 *   <li>Validation: future date rejected.</li>
 *   <li>Validation: zero/invalid subject ID rejected.</li>
 *   <li>Validation: empty entries list rejected.</li>
 *   <li>Validation: null attendance status rejected.</li>
 *   <li>Programmatic RBAC: STUDENT cannot mark attendance.</li>
 *   <li>Programmatic RBAC: STUDENT cannot update attendance.</li>
 *   <li>Programmatic RBAC: STUDENT cannot delete attendance.</li>
 *   <li>Programmatic RBAC: FACULTY cannot delete attendance.</li>
 *   <li>Attendance update succeeds for authorized user.</li>
 *   <li>Attendance history retrieval without filter returns logs.</li>
 *   <li>Attendance history filtered by subject works.</li>
 *   <li>Attendance history filtered by keyword works.</li>
 *   <li>Student data isolation: Student can query own attendance summary.</li>
 *   <li>Student data isolation: Student blocked from querying another student.</li>
 *   <li>Attendance percentage: 100% calculation.</li>
 *   <li>Attendance percentage: 0% calculation.</li>
 *   <li>Attendance percentage: partial calculation.</li>
 *   <li>Attendance percentage: zero total classes handled safely.</li>
 *   <li>Standalone attendance deletion succeeds for ADMIN.</li>
 * </ol>
 */
public class AttendanceManagementTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Stage 7 — Attendance Management Module Test Suite");
        System.out.println("==================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        AuthorizationService authz = new AuthorizationService();
        AttendanceService attendanceService = new AttendanceService(dbConnection);

        // --- SECTION A: NAVIGATION & ROLE-BASED ACCESS CONTROL ---
        System.out.println("\n[A] Verifying Navigation & Role Permissions:");

        assertTrue("ADMIN has access to ATTENDANCE", authz.canAccess(User.Role.ADMIN, NavigationItem.ATTENDANCE));
        assertTrue("FACULTY has access to ATTENDANCE", authz.canAccess(User.Role.FACULTY, NavigationItem.ATTENDANCE));
        assertFalse("STUDENT is denied access to ATTENDANCE", authz.canAccess(User.Role.STUDENT, NavigationItem.ATTENDANCE));
        assertTrue("STUDENT has access to MY_ATTENDANCE", authz.canAccess(User.Role.STUDENT, NavigationItem.MY_ATTENDANCE));

        // --- SECTION B: STUDENT ROSTER RETRIEVAL ---
        System.out.println("\n[B] Verifying Student Roster Resolution for Subject:");

        List<Student> facultyRoster = attendanceService.getStudentsForSubject(1, User.Role.FACULTY);
        assertTrue("FACULTY can retrieve roster for subject 1 (>= 3 students)", facultyRoster.size() >= 3);

        List<Student> adminRoster = attendanceService.getStudentsForSubject(1, User.Role.ADMIN);
        assertTrue("ADMIN can retrieve roster for subject 1", adminRoster.size() == facultyRoster.size());

        List<Student> studentRoster = attendanceService.getStudentsForSubject(1, User.Role.STUDENT);
        assertTrue("STUDENT receives empty roster (RBAC check)", studentRoster.isEmpty());

        // --- SECTION C: FIELD VALIDATION RULES ---
        System.out.println("\n[C] Verifying Attendance Validation Rules:");

        LocalDate testDate = LocalDate.of(2026, 9, 10);
        List<Attendance> validEntries = List.of(
                new Attendance(1, 1, testDate, Attendance.Status.PRESENT),
                new Attendance(2, 1, testDate, Attendance.Status.ABSENT),
                new Attendance(3, 1, testDate, Attendance.Status.PRESENT)
        );

        // Null Date
        AttendanceService.ServiceResult<Integer> nullDateRes = attendanceService.markAttendanceBatch(1, null, validEntries, User.Role.FACULTY);
        assertFalse("Null date is rejected", nullDateRes.isSuccess());

        // Future Date
        LocalDate futureDate = LocalDate.now().plusDays(10);
        AttendanceService.ServiceResult<Integer> futureDateRes = attendanceService.markAttendanceBatch(1, futureDate, validEntries, User.Role.FACULTY);
        assertFalse("Future date is rejected", futureDateRes.isSuccess());

        // Invalid Subject ID
        AttendanceService.ServiceResult<Integer> invalidSubRes = attendanceService.markAttendanceBatch(-1, testDate, validEntries, User.Role.FACULTY);
        assertFalse("Invalid subject ID <= 0 is rejected", invalidSubRes.isSuccess());

        // Empty Entries List
        AttendanceService.ServiceResult<Integer> emptyEntriesRes = attendanceService.markAttendanceBatch(1, testDate, List.of(), User.Role.FACULTY);
        assertFalse("Empty entries list is rejected", emptyEntriesRes.isSuccess());

        // Null status in entry
        List<Attendance> nullStatusEntries = List.of(new Attendance(1, 1, testDate, null));
        AttendanceService.ServiceResult<Integer> nullStatusRes = attendanceService.markAttendanceBatch(1, testDate, nullStatusEntries, User.Role.FACULTY);
        assertFalse("Entry with null status is rejected", nullStatusRes.isSuccess());

        // --- SECTION D: BATCH ATTENDANCE CREATION & DUPLICATE PREVENTION ---
        System.out.println("\n[D] Verifying Batch Attendance Logging & Duplicate Handling:");

        // Programmatic RBAC: STUDENT cannot mark attendance
        AttendanceService.ServiceResult<Integer> studentMarkRes = attendanceService.markAttendanceBatch(1, testDate, validEntries, User.Role.STUDENT);
        assertFalse("STUDENT cannot mark attendance (RBAC check)", studentMarkRes.isSuccess());

        // Valid batch marking by FACULTY
        AttendanceService.ServiceResult<Integer> facultyBatchRes = attendanceService.markAttendanceBatch(1, testDate, validEntries, User.Role.FACULTY);
        assertTrue("FACULTY can save attendance batch for enrolled students", facultyBatchRes.isSuccess() && facultyBatchRes.getData() == 3);

        // Verify records in DB
        List<AttendanceRecord> savedRecords = attendanceService.getAttendanceBySubjectAndDate(1, testDate, User.Role.FACULTY);
        assertTrue("3 attendance records exist for subject 1 on testDate", savedRecords.size() == 3);

        // Duplicate / Re-marking: update student 2 from ABSENT to PRESENT on same date
        List<Attendance> updatedEntries = List.of(
                new Attendance(1, 1, testDate, Attendance.Status.PRESENT),
                new Attendance(2, 1, testDate, Attendance.Status.PRESENT),
                new Attendance(3, 1, testDate, Attendance.Status.PRESENT)
        );
        AttendanceService.ServiceResult<Integer> reMarkRes = attendanceService.markAttendanceBatch(1, testDate, updatedEntries, User.Role.FACULTY);
        assertTrue("Re-marking attendance on same date succeeds via duplicate update", reMarkRes.isSuccess());

        List<AttendanceRecord> reMarkedRecords = attendanceService.getAttendanceBySubjectAndDate(1, testDate, User.Role.FACULTY);
        boolean student2Updated = reMarkedRecords.stream()
                .anyMatch(r -> r.getStudentId() == 2 && r.getStatus() == Attendance.Status.PRESENT);
        assertTrue("Student 2 attendance updated to PRESENT on duplicate date", student2Updated);

        // --- SECTION E: RECORD UPDATES & PROGRAMMATIC RBAC ---
        System.out.println("\n[E] Verifying Individual Record Updates & RBAC:");

        int targetRecordId = reMarkedRecords.get(0).getId();

        // STUDENT cannot update status
        AttendanceService.ServiceResult<Void> studentUpdateRes = attendanceService.updateAttendanceStatus(targetRecordId, Attendance.Status.ABSENT, User.Role.STUDENT);
        assertFalse("STUDENT cannot update attendance status (RBAC check)", studentUpdateRes.isSuccess());

        // FACULTY can update status
        AttendanceService.ServiceResult<Void> facultyUpdateRes = attendanceService.updateAttendanceStatus(targetRecordId, Attendance.Status.ABSENT, User.Role.FACULTY);
        assertTrue("FACULTY can update attendance status", facultyUpdateRes.isSuccess());

        // --- SECTION F: ATTENDANCE HISTORY & FILTERING ---
        System.out.println("\n[F] Verifying Attendance History & Search Filters:");

        List<AttendanceRecord> allHistory = attendanceService.getAttendanceHistory(null, null, null, 100, User.Role.FACULTY);
        assertTrue("History retrieval without filters returns >= 15 records", allHistory.size() >= 15);

        List<AttendanceRecord> subHistory = attendanceService.getAttendanceHistory(1, null, null, 100, User.Role.FACULTY);
        assertTrue("History filtered by subject 1 returns records", !subHistory.isEmpty());

        List<AttendanceRecord> keywordHistory = attendanceService.getAttendanceHistory(null, null, "Aarav", 100, User.Role.FACULTY);
        assertTrue("History filtered by keyword 'Aarav' returns Aarav Sharma records",
                !keywordHistory.isEmpty() && keywordHistory.stream().allMatch(r -> r.getStudentName().contains("Aarav")));

        // --- SECTION G: STUDENT DATA ISOLATION & PERCENTAGE ---
        System.out.println("\n[G] Verifying Student Data Isolation & Percentage Math:");

        // Student 1 querying own attendance
        StudentAttendanceSummary s1Summary = attendanceService.getStudentAttendanceSummary(1, User.Role.STUDENT, 1);
        assertTrue("Student 1 can view own attendance summary", s1Summary != null && s1Summary.getTotalClasses() > 0);

        // Student 1 attempting to query Student 2 attendance (breach attempt)
        StudentAttendanceSummary breachAttempt = attendanceService.getStudentAttendanceSummary(2, User.Role.STUDENT, 1);
        assertTrue("Student 1 querying Student 2 is strictly blocked (null returned)", breachAttempt == null);

        // Percentage calculations
        assertTrue("5/5 present = 100.0%", attendanceService.calculatePercentage(5, 5) == 100.0);
        assertTrue("0/5 present = 0.0%", attendanceService.calculatePercentage(0, 5) == 0.0);
        assertTrue("4/5 present = 80.0%", attendanceService.calculatePercentage(4, 5) == 80.0);
        assertTrue("0/0 classes = 0.0%", attendanceService.calculatePercentage(0, 0) == 0.0);

        // --- SECTION H: DELETION SAFETY & CLEANUP ---
        System.out.println("\n[H] Verifying Deletion Safety & RBAC:");

        // STUDENT cannot delete
        AttendanceService.ServiceResult<Void> studentDelRes = attendanceService.deleteAttendance(targetRecordId, User.Role.STUDENT);
        assertFalse("STUDENT cannot delete attendance record (RBAC check)", studentDelRes.isSuccess());

        // FACULTY cannot delete (admin only)
        AttendanceService.ServiceResult<Void> facultyDelRes = attendanceService.deleteAttendance(targetRecordId, User.Role.FACULTY);
        assertFalse("FACULTY cannot delete attendance record (RBAC check)", facultyDelRes.isSuccess());

        // Clean up the 3 test records created for testDate
        for (AttendanceRecord r : reMarkedRecords) {
            AttendanceService.ServiceResult<Void> adminDelRes = attendanceService.deleteAttendance(r.getId(), User.Role.ADMIN);
            assertTrue("ADMIN can delete attendance record #" + r.getId(), adminDelRes.isSuccess());
        }

        // Verify cleanup
        List<AttendanceRecord> cleanCheck = attendanceService.getAttendanceBySubjectAndDate(1, testDate, User.Role.FACULTY);
        assertTrue("Test attendance records cleaned up successfully", cleanCheck.isEmpty());

        // --- SUMMARY ---
        System.out.println("\n==================================================");
        System.out.println(String.format("Stage 7 Results: %d PASSED, %d FAILED", passedCount, failedCount));
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
}
