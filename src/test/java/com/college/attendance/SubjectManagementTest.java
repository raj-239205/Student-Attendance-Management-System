package com.college.attendance;

import com.college.attendance.config.DatabaseConfig;
import com.college.attendance.dao.SubjectDAO;
import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Subject;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.navigation.NavigationItem;

import java.util.List;

/**
 * Automated test suite for Stage 6 — Subject Management Module:
 * <ol>
 *   <li>ADMIN can access Subjects navigation.</li>
 *   <li>FACULTY can access Subjects navigation (read-only).</li>
 *   <li>STUDENT cannot access Subjects navigation.</li>
 *   <li>Subject retrieval for ADMIN returns curriculum list.</li>
 *   <li>Subject retrieval for FACULTY returns curriculum list.</li>
 *   <li>Subject retrieval for STUDENT is denied.</li>
 *   <li>Subject search by code works.</li>
 *   <li>Subject search by name works.</li>
 *   <li>Subject search by branch works.</li>
 *   <li>Empty search keyword returns full curriculum.</li>
 *   <li>Subject retrieval by ID returns accurate data.</li>
 *   <li>Subject retrieval by invalid ID returns null.</li>
 *   <li>Field validation: blank code rejected.</li>
 *   <li>Field validation: blank name rejected.</li>
 *   <li>Field validation: blank branch rejected.</li>
 *   <li>Field validation: semester &lt;= 0 rejected.</li>
 *   <li>Field validation: excessive code length rejected.</li>
 *   <li>Field validation: excessive name length rejected.</li>
 *   <li>Programmatic RBAC: FACULTY cannot create subjects.</li>
 *   <li>Programmatic RBAC: STUDENT cannot create subjects.</li>
 *   <li>Duplicate subject code creation is rejected.</li>
 *   <li>Valid subject creation succeeds for ADMIN.</li>
 *   <li>Programmatic RBAC: FACULTY cannot update subjects.</li>
 *   <li>Programmatic RBAC: STUDENT cannot update subjects.</li>
 *   <li>Valid subject update succeeds for ADMIN.</li>
 *   <li>Subject update with conflicting code is rejected.</li>
 *   <li>Foreign-key deletion restriction is enforced safely on subjects with attendance records.</li>
 *   <li>Programmatic RBAC: FACULTY cannot delete subjects.</li>
 *   <li>Programmatic RBAC: STUDENT cannot delete subjects.</li>
 *   <li>Standalone subject deletion succeeds for ADMIN.</li>
 *   <li>Subject verified deleted from database.</li>
 * </ol>
 */
public class SubjectManagementTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Stage 6 — Subject Management Module Test Suite");
        System.out.println("==================================================");

        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection dbConnection = new DatabaseConnection(config);
        AuthorizationService authz = new AuthorizationService();
        SubjectDAO subjectDAO = new SubjectDAO();
        SubjectService subjectService = new SubjectService(dbConnection, subjectDAO);

        // --- SECTION A: ROLE-BASED ACCESS CONTROL (RBAC) ---
        System.out.println("\n[A] Verifying Navigation & Module Permissions:");

        assertTrue("ADMIN has access to SUBJECTS", authz.canAccess(User.Role.ADMIN, NavigationItem.SUBJECTS));
        assertTrue("FACULTY has access to SUBJECTS (read-only)", authz.canAccess(User.Role.FACULTY, NavigationItem.SUBJECTS));
        assertFalse("STUDENT is denied access to SUBJECTS", authz.canAccess(User.Role.STUDENT, NavigationItem.SUBJECTS));

        // --- SECTION B: SUBJECT RETRIEVAL & SEARCH ---
        System.out.println("\n[B] Verifying Subject Retrieval & Keyword Search:");

        List<Subject> allSubjects = subjectService.getAllSubjects(User.Role.ADMIN);
        assertTrue("getAllSubjects returns at least 2 existing subjects", allSubjects.size() >= 2);

        List<Subject> facultyList = subjectService.getAllSubjects(User.Role.FACULTY);
        assertTrue("FACULTY can retrieve all subjects", facultyList.size() == allSubjects.size());

        List<Subject> studentRoleList = subjectService.getAllSubjects(User.Role.STUDENT);
        assertTrue("STUDENT role receives empty list for getAllSubjects", studentRoleList.isEmpty());

        // Search by Code
        List<Subject> searchByCode = subjectService.searchSubjects("CS401", User.Role.ADMIN);
        assertTrue("Search by code 'CS401' returns Advanced Java",
                !searchByCode.isEmpty() && "Advanced Java".equals(searchByCode.get(0).getSubjectName()));

        // Search by Name
        List<Subject> searchByName = subjectService.searchSubjects("Database", User.Role.FACULTY);
        assertTrue("Search by name 'Database' returns CS402",
                !searchByName.isEmpty() && "CS402".equals(searchByName.get(0).getSubjectCode()));

        // Search by Branch
        List<Subject> searchByBranch = subjectService.searchSubjects("CSE", User.Role.ADMIN);
        assertTrue("Search by branch 'CSE' returns matching subjects", !searchByBranch.isEmpty());

        // Empty Search Keyword
        List<Subject> searchEmpty = subjectService.searchSubjects("", User.Role.ADMIN);
        assertTrue("Empty search keyword returns full subject list", searchEmpty.size() == allSubjects.size());

        // Retrieval by ID
        Subject s1 = subjectService.getSubjectById(1, User.Role.ADMIN);
        assertTrue("getSubjectById(1) returns CS401", s1 != null && "CS401".equals(s1.getSubjectCode()));

        Subject sInvalid = subjectService.getSubjectById(-999, User.Role.ADMIN);
        assertTrue("getSubjectById with invalid ID returns null", sInvalid == null);

        // --- SECTION C: VALIDATION RULES ---
        System.out.println("\n[C] Verifying Field Validation Rules:");

        Subject blankCode = new Subject("", "Software Testing", "CSE", 4);
        assertTrue("Blank subject code is rejected", subjectService.validateSubject(blankCode) != null);

        Subject blankName = new Subject("CS499", "", "CSE", 4);
        assertTrue("Blank subject name is rejected", subjectService.validateSubject(blankName) != null);

        Subject blankBranch = new Subject("CS499", "Software Testing", "", 4);
        assertTrue("Blank branch is rejected", subjectService.validateSubject(blankBranch) != null);

        Subject zeroSemester = new Subject("CS499", "Software Testing", "CSE", 0);
        assertTrue("Semester <= 0 is rejected", subjectService.validateSubject(zeroSemester) != null);

        Subject longCode = new Subject("A".repeat(25), "Valid Name", "CSE", 4);
        assertTrue("Subject code exceeding 20 chars is rejected", subjectService.validateSubject(longCode) != null);

        Subject longName = new Subject("CS499", "A".repeat(105), "CSE", 4);
        assertTrue("Subject name exceeding 100 chars is rejected", subjectService.validateSubject(longName) != null);

        // --- SECTION D: SUBJECT CREATION, UNIQUENESS, AND PROGRAMMATIC RBAC ---
        System.out.println("\n[D] Verifying Subject Creation & Uniqueness Checks:");

        Subject testSubject = new Subject("TEST_CS_99", "Principles of Compiler Design", "CSE", 6);

        // Programmatic RBAC: FACULTY cannot create subjects
        SubjectService.ServiceResult<Subject> facultyCreate = subjectService.createSubject(testSubject, User.Role.FACULTY);
        assertFalse("FACULTY cannot create subjects (RBAC check)", facultyCreate.isSuccess());

        // Programmatic RBAC: STUDENT cannot create subjects
        SubjectService.ServiceResult<Subject> studentRoleCreate = subjectService.createSubject(testSubject, User.Role.STUDENT);
        assertFalse("STUDENT role cannot create subjects (RBAC check)", studentRoleCreate.isSuccess());

        // Duplicate Code check with existing subject (CS401)
        Subject duplicateCode = new Subject("CS401", "Duplicate Subject", "CSE", 4);
        SubjectService.ServiceResult<Subject> dupCodeRes = subjectService.createSubject(duplicateCode, User.Role.ADMIN);
        assertFalse("Duplicate subject code creation is rejected", dupCodeRes.isSuccess());

        // Valid creation by ADMIN
        SubjectService.ServiceResult<Subject> createRes = subjectService.createSubject(testSubject, User.Role.ADMIN);
        assertTrue("ADMIN can create valid subject", createRes.isSuccess() && createRes.getData().getId() > 0);
        int createdId = (createRes.getData() != null) ? createRes.getData().getId() : -1;

        // --- SECTION E: UPDATE OPERATIONS ---
        System.out.println("\n[E] Verifying Subject Update Operations:");

        if (createdId > 0) {
            testSubject.setId(createdId);
            testSubject.setSubjectName("Compiler Design & Optimization");
            testSubject.setSemester(7);

            // FACULTY cannot update
            SubjectService.ServiceResult<Subject> facultyUpdate = subjectService.updateSubject(testSubject, User.Role.FACULTY);
            assertFalse("FACULTY cannot update subjects (RBAC check)", facultyUpdate.isSuccess());

            // STUDENT cannot update
            SubjectService.ServiceResult<Subject> studentRoleUpdate = subjectService.updateSubject(testSubject, User.Role.STUDENT);
            assertFalse("STUDENT cannot update subjects (RBAC check)", studentRoleUpdate.isSuccess());

            // ADMIN valid update
            SubjectService.ServiceResult<Subject> adminUpdate = subjectService.updateSubject(testSubject, User.Role.ADMIN);
            assertTrue("ADMIN can update subject", adminUpdate.isSuccess());

            Subject fetchedUpdated = subjectService.getSubjectById(createdId, User.Role.ADMIN);
            assertTrue("Updated name was persisted", fetchedUpdated != null && "Compiler Design & Optimization".equals(fetchedUpdated.getSubjectName()));
            assertTrue("Updated semester was persisted", fetchedUpdated != null && fetchedUpdated.getSemester() == 7);

            // Update attempting to reuse another subject's code (CS402)
            testSubject.setSubjectCode("CS402");
            SubjectService.ServiceResult<Subject> conflictCodeUpdate = subjectService.updateSubject(testSubject, User.Role.ADMIN);
            assertFalse("Update with conflicting subject code is rejected", conflictCodeUpdate.isSuccess());
        }

        // --- SECTION F: DELETION OPERATIONS & REFERENTIAL INTEGRITY ---
        System.out.println("\n[F] Verifying Deletion Safety & Referential Integrity:");

        // 1. Foreign Key Restriction: Attempting to delete subject_id = 1 (CS401)
        // CS401 has attendance records; deletion must be safely blocked.
        SubjectService.ServiceResult<Void> restrictedDelete = subjectService.deleteSubject(1, User.Role.ADMIN);
        assertFalse("Deleting subject with attendance references is safely blocked", restrictedDelete.isSuccess());
        assertTrue("Error message explains attendance records constraint",
                restrictedDelete.getMessage().contains("Attendance records") || restrictedDelete.getMessage().contains("Cannot delete"));

        // 2. FACULTY cannot delete
        if (createdId > 0) {
            SubjectService.ServiceResult<Void> facultyDelete = subjectService.deleteSubject(createdId, User.Role.FACULTY);
            assertFalse("FACULTY cannot delete subjects (RBAC check)", facultyDelete.isSuccess());
        }

        // 3. STUDENT cannot delete
        if (createdId > 0) {
            SubjectService.ServiceResult<Void> studentDelete = subjectService.deleteSubject(createdId, User.Role.STUDENT);
            assertFalse("STUDENT cannot delete subjects (RBAC check)", studentDelete.isSuccess());
        }

        // 4. Standalone subject deletion by ADMIN
        if (createdId > 0) {
            SubjectService.ServiceResult<Void> adminDelete = subjectService.deleteSubject(createdId, User.Role.ADMIN);
            assertTrue("ADMIN can delete standalone subject", adminDelete.isSuccess());

            Subject deletedCheck = subjectService.getSubjectById(createdId, User.Role.ADMIN);
            assertTrue("Subject no longer exists after deletion", deletedCheck == null);
        }

        // --- SUMMARY ---
        System.out.println("\n==================================================");
        System.out.println(String.format("Stage 6 Results: %d PASSED, %d FAILED", passedCount, failedCount));
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
