package com.college.attendance.dao;

import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.SubjectAttendance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
