package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.StudentDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Student;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.StudentService;
import com.college.attendance.ui.navigation.NavigationItem;

import java.sql.Connection;
import java.util.List;

/**
 * Automated test suite for Stage 5 — Student Management Module:
 * <ol>
 *   <li>ADMIN can access Students navigation.</li>
 *   <li>FACULTY can access Students navigation.</li>
 *   <li>STUDENT cannot access Students navigation.</li>
 *   <li>Valid student creation succeeds for ADMIN.</li>
 *   <li>Invalid student creation (blank required fields, invalid semester) is rejected.</li>
 *   <li>Duplicate roll number is rejected.</li>
 *   <li>Duplicate email is rejected.</li>
 *   <li>Student retrieval by ID returns accurate profile data.</li>
 *   <li>Student keyword search works (by roll number, name, branch, email).</li>
 *   <li>Student update persists modified details for ADMIN.</li>
 *   <li>Invalid update (conflicting roll number with another student) is rejected.</li>
 *   <li>Student deletion of a standalone student succeeds for ADMIN.</li>
 *   <li>Foreign-key deletion restriction is enforced safely without crashing when dependent records exist.</li>
 *   <li>Empty search keyword returns all enrolled students.</li>
 *   <li>Invalid / non-existent student ID handled safely.</li>
 *   <li>Programmatic authorization: Non-ADMIN (FACULTY / STUDENT) calling create/update/delete is rejected.</li>
 * </ol>
 */
public class StudentManagementTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Stage 5 — Student Management Module Test Suite");
        System.out.println("==================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        AuthorizationService authz = new AuthorizationService();
        StudentDAO studentDAO = new StudentDAO();
        StudentService studentService = new StudentService(dbConnection, studentDAO);

        // --- SECTION A: ROLE-BASED ACCESS CONTROL (RBAC) ---
        System.out.println("\n[A] Verifying Navigation & Module Permissions:");

        assertTrue("ADMIN has access to STUDENTS", authz.canAccess(User.Role.ADMIN, NavigationItem.STUDENTS));
        assertTrue("FACULTY has access to STUDENTS", authz.canAccess(User.Role.FACULTY, NavigationItem.STUDENTS));
        assertFalse("STUDENT is denied access to STUDENTS", authz.canAccess(User.Role.STUDENT, NavigationItem.STUDENTS));

        // --- SECTION B: STUDENT RETRIEVAL & SEARCH ---
        System.out.println("\n[B] Verifying Student Retrieval & Keyword Search:");

        List<Student> allStudents = studentService.getAllStudents(User.Role.ADMIN);
        assertTrue("getAllStudents returns at least 3 existing students", allStudents.size() >= 3);

        List<Student> facultyList = studentService.getAllStudents(User.Role.FACULTY);
        assertTrue("FACULTY can retrieve all students", facultyList.size() == allStudents.size());

        List<Student> studentRoleList = studentService.getAllStudents(User.Role.STUDENT);
        assertTrue("STUDENT role receives empty list for getAllStudents", studentRoleList.isEmpty());

        // Search by Roll Number
        List<Student> searchByRoll = studentService.searchStudents("CS2024001", User.Role.ADMIN);
        assertTrue("Search by roll 'CS2024001' returns Aarav Sharma",
                !searchByRoll.isEmpty() && "Aarav Sharma".equals(searchByRoll.get(0).getName()));

        // Search by Name
        List<Student> searchByName = studentService.searchStudents("Priya", User.Role.FACULTY);
        assertTrue("Search by name 'Priya' returns Priya Patel",
                !searchByName.isEmpty() && "CS2024002".equals(searchByName.get(0).getRollNo()));

        // Search by Branch
        List<Student> searchByBranch = studentService.searchStudents("CSE", User.Role.ADMIN);
        assertTrue("Search by branch 'CSE' returns matching students", !searchByBranch.isEmpty());

        // Empty Search Keyword
        List<Student> searchEmpty = studentService.searchStudents("", User.Role.ADMIN);
        assertTrue("Empty search keyword returns full student list", searchEmpty.size() == allStudents.size());

        // Retrieval by ID
        Student s1 = studentService.getStudentById(1, User.Role.ADMIN);
        assertTrue("getStudentById(1) returns Aarav Sharma", s1 != null && "Aarav Sharma".equals(s1.getName()));

        Student sInvalid = studentService.getStudentById(-999, User.Role.ADMIN);
        assertTrue("getStudentById with invalid ID returns null", sInvalid == null);

        // --- SECTION C: VALIDATION RULES ---
        System.out.println("\n[C] Verifying Field Validation Rules:");

        Student blankName = new Student("TEST001", "", "CSE", 2, "A");
        assertTrue("Blank name is rejected", studentService.validateStudent(blankName) != null);

        Student blankRoll = new Student("", "Test Student", "CSE", 2, "A");
        assertTrue("Blank roll number is rejected", studentService.validateStudent(blankRoll) != null);

        Student blankBranch = new Student("TEST001", "Test Student", "", 2, "A");
        assertTrue("Blank branch is rejected", studentService.validateStudent(blankBranch) != null);

        Student zeroSemester = new Student("TEST001", "Test Student", "CSE", 0, "A");
        assertTrue("Semester <= 0 is rejected", studentService.validateStudent(zeroSemester) != null);

        Student blankSection = new Student("TEST001", "Test Student", "CSE", 2, "");
        assertTrue("Blank section is rejected", studentService.validateStudent(blankSection) != null);

        Student invalidEmail = new Student("TEST001", "Test Student", "CSE", 2, "A");
        invalidEmail.setEmail("not-an-email");
        assertTrue("Malformed email is rejected", studentService.validateStudent(invalidEmail) != null);

        // --- SECTION D: STUDENT CREATION, UNIQUENESS, AND PROGRAMMATIC RBAC ---
        System.out.println("\n[D] Verifying Student Creation & Uniqueness Checks:");

        // Programmatic RBAC: FACULTY cannot create students
        Student testStudent = new Student("TEMP_TEST_01", "Unit Test Student", "CSE", 3, "B");
        testStudent.setEmail("unittest01@college.edu");
        testStudent.setPhone("9999999999");

        StudentService.ServiceResult<Student> facultyCreate = studentService.createStudent(testStudent, User.Role.FACULTY);
        assertFalse("FACULTY cannot create students (RBAC check)", facultyCreate.isSuccess());

        // Programmatic RBAC: STUDENT cannot create students
        StudentService.ServiceResult<Student> studentRoleCreate = studentService.createStudent(testStudent, User.Role.STUDENT);
        assertFalse("STUDENT role cannot create students (RBAC check)", studentRoleCreate.isSuccess());

        // Duplicate Roll Number check with existing student (CS2024001)
        Student duplicateRoll = new Student("CS2024001", "Another Student", "CSE", 2, "A");
        StudentService.ServiceResult<Student> dupRollRes = studentService.createStudent(duplicateRoll, User.Role.ADMIN);
        assertFalse("Duplicate roll number creation is rejected", dupRollRes.isSuccess());

        // Duplicate Email check with existing student (aarav.sharma@college.edu)
        Student duplicateEmail = new Student("TEST_ROLL_99", "Another Student", "CSE", 2, "A");
        duplicateEmail.setEmail("aarav.sharma@college.edu");
        StudentService.ServiceResult<Student> dupEmailRes = studentService.createStudent(duplicateEmail, User.Role.ADMIN);
        assertFalse("Duplicate email creation is rejected", dupEmailRes.isSuccess());

        // Valid creation by ADMIN
        StudentService.ServiceResult<Student> createRes = studentService.createStudent(testStudent, User.Role.ADMIN);
        assertTrue("ADMIN can create valid student", createRes.isSuccess() && createRes.getData().getId() > 0);
        int createdId = (createRes.getData() != null) ? createRes.getData().getId() : -1;

        // --- SECTION E: UPDATE OPERATIONS ---
        System.out.println("\n[E] Verifying Student Update Operations:");

        if (createdId > 0) {
            testStudent.setId(createdId);
            testStudent.setName("Unit Test Student (Updated)");
            testStudent.setSection("C");

            // FACULTY cannot update
            StudentService.ServiceResult<Student> facultyUpdate = studentService.updateStudent(testStudent, User.Role.FACULTY);
            assertFalse("FACULTY cannot update students (RBAC check)", facultyUpdate.isSuccess());

            // ADMIN valid update
            StudentService.ServiceResult<Student> adminUpdate = studentService.updateStudent(testStudent, User.Role.ADMIN);
            assertTrue("ADMIN can update student", adminUpdate.isSuccess());

            Student fetchedUpdated = studentService.getStudentById(createdId, User.Role.ADMIN);
            assertTrue("Updated name was persisted", fetchedUpdated != null && "Unit Test Student (Updated)".equals(fetchedUpdated.getName()));
            assertTrue("Updated section was persisted", fetchedUpdated != null && "C".equals(fetchedUpdated.getSection()));

            // Update attempting to reuse another student's roll number (CS2024002)
            testStudent.setRollNo("CS2024002");
            StudentService.ServiceResult<Student> conflictRollUpdate = studentService.updateStudent(testStudent, User.Role.ADMIN);
            assertFalse("Update with conflicting roll number is rejected", conflictRollUpdate.isSuccess());
        }

        // --- SECTION F: DELETION OPERATIONS & REFERENTIAL INTEGRITY ---
        System.out.println("\n[F] Verifying Deletion Safety & Referential Integrity:");

        // 1. Foreign Key Restriction: Attempting to delete student_id = 1 (Aarav Sharma)
        // Aarav has attendance records and user login account; deletion must be safely blocked.
        StudentService.ServiceResult<Void> restrictedDelete = studentService.deleteStudent(1, User.Role.ADMIN);
        assertFalse("Deleting student with attendance/user references is safely blocked", restrictedDelete.isSuccess());
        assertTrue("Error message explains dependency constraint",
                restrictedDelete.getMessage().contains("Attendance records exist") || restrictedDelete.getMessage().contains("user account"));

        // 2. FACULTY cannot delete
        if (createdId > 0) {
            StudentService.ServiceResult<Void> facultyDelete = studentService.deleteStudent(createdId, User.Role.FACULTY);
            assertFalse("FACULTY cannot delete students (RBAC check)", facultyDelete.isSuccess());
        }

        // 3. Standalone student deletion by ADMIN
        if (createdId > 0) {
            StudentService.ServiceResult<Void> adminDelete = studentService.deleteStudent(createdId, User.Role.ADMIN);
            assertTrue("ADMIN can delete standalone student", adminDelete.isSuccess());

            Student deletedCheck = studentService.getStudentById(createdId, User.Role.ADMIN);
            assertTrue("Student no longer exists after deletion", deletedCheck == null);
        }

        // --- SUMMARY ---
        System.out.println("\n==================================================");
        System.out.println(String.format("Stage 5 Results: %d PASSED, %d FAILED", passedCount, failedCount));
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
