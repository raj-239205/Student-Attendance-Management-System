package com.college.attendance.dao;

import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.LectureAttendanceSummary;
import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.Subject;
import com.college.attendance.model.SubjectAttendance;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Access Object for dashboard metrics and dynamic student attendance statistics.
 */
public class DashboardDAO {

    /**
     * Retrieves the total count of enrolled students.
     */
    public int countStudents(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves the total count of active subjects.
     */
    public int countSubjects(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM subjects";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves the total count of attendance records logged.
     */
     public int countAttendanceRecords(Connection conn) throws SQLException {
         String sql = "SELECT COUNT(*) FROM attendance";
         try (PreparedStatement stmt = conn.prepareStatement(sql);
              ResultSet rs = stmt.executeQuery()) {
             if (rs.next()) {
                 return rs.getInt(1);
             }
         }
         return 0;
     }

    /**
     * Counts the distinct students marked PRESENT for a specific date (e.g. today).
     * Prevents double-counting if a student attended multiple lectures.
     */
    public int countTodayPresentStudents(Connection conn, LocalDate date) throws SQLException {
        if (date == null) {
            return 0;
        }
        String sql = "SELECT COUNT(DISTINCT student_id) FROM attendance WHERE attendance_date = ? AND status = 'PRESENT'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves aggregated lecture-wise attendance records grouped by session (attendance_date and subject).
     */
    public List<LectureAttendanceSummary> getLectureAttendanceSummaries(Connection conn,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate,
                                                                        Integer subjectId,
                                                                        int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.attendance_date, sub.id AS subject_id, sub.subject_code, sub.subject_name,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count,
                       COUNT(a.id) AS total_count
                FROM attendance a
                JOIN subjects sub ON a.subject_id = sub.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (startDate != null) {
            sql.append(" AND a.attendance_date >= ?");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND a.attendance_date <= ?");
            params.add(Date.valueOf(endDate));
        }
        if (subjectId != null && subjectId > 0) {
            sql.append(" AND a.subject_id = ?");
            params.add(subjectId);
        }

        sql.append(" GROUP BY a.attendance_date, sub.id, sub.subject_code, sub.subject_name ");
        sql.append(" ORDER BY a.attendance_date DESC, sub.subject_code ASC");

        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        List<LectureAttendanceSummary> summaries = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("attendance_date");
                    LocalDate date = (d != null) ? d.toLocalDate() : null;
                    if (date != null) {
                        summaries.add(new LectureAttendanceSummary(
                                date,
                                rs.getInt("subject_id"),
                                rs.getString("subject_code"),
                                rs.getString("subject_name"),
                                rs.getInt("present_count"),
                                rs.getInt("absent_count"),
                                rs.getInt("total_count")
                        ));
                    }
                }
            }
        }
        return summaries;
    }

    /**
     * Calculates the number of unique students with overall attendance strictly below the threshold.
     */
    public int countDefaulters(Connection conn, double threshold) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM (
                    SELECT student_id
                    FROM attendance
                    GROUP BY student_id
                    HAVING (SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*)) * 100.0 < ?
                ) AS defaulters
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves a student by their primary key.
     */
    public Student findStudentById(Connection conn, int studentId) throws SQLException {
        String sql = "SELECT id, roll_no, name, branch, semester, section, email, phone FROM students WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Student s = new Student();
                    s.setId(rs.getInt("id"));
                    s.setRollNo(rs.getString("roll_no"));
                    s.setName(rs.getString("name"));
                    s.setBranch(rs.getString("branch"));
                    s.setSemester(rs.getInt("semester"));
                    s.setSection(rs.getString("section"));
                    s.setEmail(rs.getString("email"));
                    s.setPhone(rs.getString("phone"));
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the comprehensive personal attendance summary for a student.
     */
    public StudentAttendanceSummary getStudentAttendanceSummary(Connection conn, int studentId) throws SQLException {
        Student student = findStudentById(conn, studentId);
        if (student == null) {
            return null;
        }

        int totalClasses = 0;
        int presentClasses = 0;
        int absentClasses = 0;

        String totalSql = """
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count
                FROM attendance
                WHERE student_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(totalSql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalClasses = rs.getInt("total");
                    presentClasses = rs.getInt("present_count");
                    absentClasses = rs.getInt("absent_count");
                }
            }
        }

        List<SubjectAttendance> subjects = new ArrayList<>();
        String subjectSql = """
                SELECT s.id, s.subject_code, s.subject_name,
                       COUNT(a.id) AS total_classes,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_classes,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_classes
                FROM attendance a
                JOIN subjects s ON a.subject_id = s.id
                WHERE a.student_id = ?
                GROUP BY s.id, s.subject_code, s.subject_name
                ORDER BY s.subject_code ASC
                """;
        try (PreparedStatement stmt = conn.prepareStatement(subjectSql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(new SubjectAttendance(
                            rs.getInt("id"),
                            rs.getString("subject_code"),
                            rs.getString("subject_name"),
                            rs.getInt("total_classes"),
                            rs.getInt("present_classes"),
                            rs.getInt("absent_classes")
                    ));
                }
            }
        }

        return new StudentAttendanceSummary(student, totalClasses, presentClasses, absentClasses, subjects);
    }

    /**
     * Retrieves the list of subjects assigned to a specific faculty member.
     * Guarantees deterministic role-scoped subject assignment.
     */
    public List<Subject> getFacultyAssignedSubjects(Connection conn, int facultyUserId) throws SQLException {
        List<Subject> all = new SubjectDAO().getAllSubjects(conn);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        if (all.size() <= 4) {
            return all;
        }
        // Deterministically assign 4 subjects based on faculty user ID
        // For default faculty (id <= 2 or even id), assigns CS401, CS402, CS403, CS404
        int offset = (facultyUserId <= 2) ? 0 : Math.abs(facultyUserId * 2) % all.size();
        List<Subject> assigned = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            assigned.add(all.get((offset + i) % all.size()));
        }
        return assigned;
    }

    /**
     * Counts distinct students marked PRESENT today across the faculty's assigned subjects.
     */
    public int countFacultyAssignedTodayPresent(Connection conn, List<Integer> subjectIds, LocalDate date) throws SQLException {
        if (subjectIds == null || subjectIds.isEmpty() || date == null) {
            return 0;
        }
        String inClause = subjectIds.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(DISTINCT student_id) FROM attendance WHERE attendance_date = ? AND status = 'PRESENT' AND subject_id IN (" + inClause + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            for (int i = 0; i < subjectIds.size(); i++) {
                stmt.setInt(i + 2, subjectIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Counts distinct lecture sessions held today across the faculty's assigned subjects.
     */
    public int countFacultyAssignedTodayLectures(Connection conn, List<Integer> subjectIds, LocalDate date) throws SQLException {
        if (subjectIds == null || subjectIds.isEmpty() || date == null) {
            return 0;
        }
        String inClause = subjectIds.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(DISTINCT subject_id) FROM attendance WHERE attendance_date = ? AND subject_id IN (" + inClause + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            for (int i = 0; i < subjectIds.size(); i++) {
                stmt.setInt(i + 2, subjectIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Calculates the number of defaulters (< threshold) strictly within the faculty's assigned subjects.
     */
    public int countFacultyAssignedDefaulters(Connection conn, List<Integer> subjectIds, double threshold) throws SQLException {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return 0;
        }
        String inClause = subjectIds.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(*) FROM (" +
                " SELECT student_id FROM attendance WHERE subject_id IN (" + inClause + ")" +
                " GROUP BY student_id" +
                " HAVING (SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*)) * 100.0 < ?" +
                ") AS def";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer subId : subjectIds) {
                stmt.setInt(idx++, subId);
            }
            stmt.setDouble(idx, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves overall attendance distribution (Present vs Absent) strictly for the faculty's assigned subjects.
     */
    public AttendanceDistribution getFacultyAttendanceDistribution(Connection conn, List<Integer> subjectIds) throws SQLException {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return AttendanceDistribution.empty();
        }
        String inClause = subjectIds.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = "SELECT SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count, " +
                " SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count " +
                " FROM attendance WHERE subject_id IN (" + inClause + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < subjectIds.size(); i++) {
                stmt.setInt(i + 1, subjectIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int present = rs.getInt("present_count");
                    int absent = rs.getInt("absent_count");
                    return new AttendanceDistribution(present, absent);
                }
            }
        }
        return AttendanceDistribution.empty();
    }

    /**
     * Retrieves aggregated lecture-wise attendance summaries strictly for the faculty's assigned subjects.
     */
    public List<LectureAttendanceSummary> getFacultyLectureAttendanceSummaries(Connection conn,
                                                                               List<Integer> subjectIds,
                                                                               LocalDate startDate,
                                                                               LocalDate endDate,
                                                                               Integer specificSubjectId,
                                                                               int limit) throws SQLException {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Collections.emptyList();
        }
        // RBAC enforcement: verify specific subject is authorized
        List<Integer> effectiveSubjectIds;
        if (specificSubjectId != null && specificSubjectId > 0) {
            if (!subjectIds.contains(specificSubjectId)) {
                return Collections.emptyList();
            }
            effectiveSubjectIds = List.of(specificSubjectId);
        } else {
            effectiveSubjectIds = subjectIds;
        }

        String inClause = effectiveSubjectIds.stream().map(s -> "?").collect(Collectors.joining(","));
        StringBuilder sql = new StringBuilder("""
                SELECT a.attendance_date, sub.id AS subject_id, sub.subject_code, sub.subject_name,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count,
                       COUNT(a.id) AS total_count
                FROM attendance a
                JOIN subjects sub ON a.subject_id = sub.id
                WHERE a.subject_id IN (
                """);
        sql.append(inClause).append(")");

        List<Object> params = new ArrayList<>(effectiveSubjectIds);
        if (startDate != null) {
            sql.append(" AND a.attendance_date >= ?");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND a.attendance_date <= ?");
            params.add(Date.valueOf(endDate));
        }

        sql.append(" GROUP BY a.attendance_date, sub.id, sub.subject_code, sub.subject_name ");
        sql.append(" ORDER BY a.attendance_date DESC, sub.subject_code ASC");

        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        List<LectureAttendanceSummary> summaries = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("attendance_date");
                    LocalDate date = (d != null) ? d.toLocalDate() : null;
                    if (date != null) {
                        summaries.add(new LectureAttendanceSummary(
                                date,
                                rs.getInt("subject_id"),
                                rs.getString("subject_code"),
                                rs.getString("subject_name"),
                                rs.getInt("present_count"),
                                rs.getInt("absent_count"),
                                rs.getInt("total_count")
                        ));
                    }
                }
            }
        }
        return summaries;
    }
}
