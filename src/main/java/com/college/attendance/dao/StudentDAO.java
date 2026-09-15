package com.college.attendance.dao;

import com.college.attendance.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for student entities, managing database persistence
 * with PreparedStatement and try-with-resources.
 */
public class StudentDAO {

    /**
     * Retrieves all enrolled students ordered by semester, branch, roll number.
     */
    public List<Student> getAllStudents(Connection conn) throws SQLException {
        String sql = """
                SELECT id, roll_no, name, branch, semester, section, email, phone, created_at, updated_at
                FROM students
                ORDER BY semester ASC, branch ASC, roll_no ASC
                """;
        List<Student> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToStudent(rs));
            }
        }
        return list;
    }

    /**
     * Searches students by matching keyword against roll number, name, email, or branch.
     */
    public List<Student> searchStudents(Connection conn, String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents(conn);
        }

        String pattern = "%" + keyword.trim() + "%";
        String sql = """
                SELECT id, roll_no, name, branch, semester, section, email, phone, created_at, updated_at
                FROM students
                WHERE roll_no LIKE ?
                   OR name LIKE ?
                   OR email LIKE ?
                   OR branch LIKE ?
                ORDER BY semester ASC, branch ASC, roll_no ASC
                """;

        List<Student> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToStudent(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves a student by primary key ID.
     */
    public Student getStudentById(Connection conn, int studentId) throws SQLException {
        String sql = """
                SELECT id, roll_no, name, branch, semester, section, email, phone, created_at, updated_at
                FROM students
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStudent(rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks if a roll number already exists, optionally excluding a specific student ID (for updates).
     */
    public boolean rollNumberExists(Connection conn, String rollNo, Integer excludeId) throws SQLException {
        String sql = (excludeId != null)
                ? "SELECT COUNT(*) FROM students WHERE roll_no = ? AND id != ?"
                : "SELECT COUNT(*) FROM students WHERE roll_no = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Checks if an email already exists, optionally excluding a specific student ID (for updates).
     */
    public boolean emailExists(Connection conn, String email, Integer excludeId) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String sql = (excludeId != null)
                ? "SELECT COUNT(*) FROM students WHERE email = ? AND id != ?"
                : "SELECT COUNT(*) FROM students WHERE email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Creates a new student record and returns the generated primary key ID.
     */
    public int createStudent(Connection conn, Student student) throws SQLException {
        String sql = """
                INSERT INTO students (roll_no, name, branch, semester, section, email, phone)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, student.getRollNo());
            stmt.setString(2, student.getName());
            stmt.setString(3, student.getBranch());
            stmt.setInt(4, student.getSemester());
            stmt.setString(5, student.getSection());
            stmt.setString(6, (student.getEmail() != null && !student.getEmail().isBlank()) ? student.getEmail().trim() : null);
            stmt.setString(7, (student.getPhone() != null && !student.getPhone().isBlank()) ? student.getPhone().trim() : null);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int generatedId = keys.getInt(1);
                        student.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Updates an existing student record.
     */
    public boolean updateStudent(Connection conn, Student student) throws SQLException {
        String sql = """
                UPDATE students
                SET roll_no = ?,
                    name = ?,
                    branch = ?,
                    semester = ?,
                    section = ?,
                    email = ?,
                    phone = ?
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, student.getRollNo());
            stmt.setString(2, student.getName());
            stmt.setString(3, student.getBranch());
            stmt.setInt(4, student.getSemester());
            stmt.setString(5, student.getSection());
            stmt.setString(6, (student.getEmail() != null && !student.getEmail().isBlank()) ? student.getEmail().trim() : null);
            stmt.setString(7, (student.getPhone() != null && !student.getPhone().isBlank()) ? student.getPhone().trim() : null);
            stmt.setInt(8, student.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a student by primary key ID.
     */
    public boolean deleteStudent(Connection conn, int studentId) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks if any attendance records reference the student.
     */
    public boolean hasAttendanceRecords(Connection conn, int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE student_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a user login account references the student.
     */
    public boolean hasUserAccount(Connection conn, int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE student_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Student mapRowToStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setRollNo(rs.getString("roll_no"));
        s.setName(rs.getString("name"));
        s.setBranch(rs.getString("branch"));
        s.setSemester(rs.getInt("semester"));
        s.setSection(rs.getString("section"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));

        java.sql.Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            s.setCreatedAt(created.toLocalDateTime());
        }

        java.sql.Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            s.setUpdatedAt(updated.toLocalDateTime());
        }

        return s;
    }
}
