package com.college.attendance.dao;

import com.college.attendance.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for course curriculum and subject catalog.
 */
public class SubjectDAO {

    /**
     * Retrieves all subjects ordered by semester, branch, and subject code.
     */
    public List<Subject> getAllSubjects(Connection conn) throws SQLException {
        String sql = """
                SELECT id, subject_code, subject_name, branch, semester, created_at
                FROM subjects
                ORDER BY semester ASC, branch ASC, subject_code ASC
                """;
        List<Subject> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToSubject(rs));
            }
        }
        return list;
    }

    /**
     * Searches subjects matching keyword against subject code, subject name, or branch.
     */
    public List<Subject> searchSubjects(Connection conn, String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSubjects(conn);
        }

        String pattern = "%" + keyword.trim() + "%";
        String sql = """
                SELECT id, subject_code, subject_name, branch, semester, created_at
                FROM subjects
                WHERE subject_code LIKE ?
                   OR subject_name LIKE ?
                   OR branch LIKE ?
                ORDER BY semester ASC, branch ASC, subject_code ASC
                """;

        List<Subject> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToSubject(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves a single subject by primary key ID.
     */
    public Subject getSubjectById(Connection conn, int subjectId) throws SQLException {
        String sql = """
                SELECT id, subject_code, subject_name, branch, semester, created_at
                FROM subjects
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSubject(rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks if a subject code already exists, optionally excluding an existing subject ID (for updates).
     */
    public boolean subjectCodeExists(Connection conn, String subjectCode, Integer excludeId) throws SQLException {
        String sql = (excludeId != null)
                ? "SELECT COUNT(*) FROM subjects WHERE subject_code = ? AND id != ?"
                : "SELECT COUNT(*) FROM subjects WHERE subject_code = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subjectCode.trim());
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
     * Creates a new subject record and returns the generated primary key ID.
     */
    public int createSubject(Connection conn, Subject subject) throws SQLException {
        String sql = """
                INSERT INTO subjects (subject_code, subject_name, branch, semester)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, subject.getSubjectCode().trim());
            stmt.setString(2, subject.getSubjectName().trim());
            stmt.setString(3, subject.getBranch().trim());
            stmt.setInt(4, subject.getSemester());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int generatedId = keys.getInt(1);
                        subject.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Updates an existing subject record.
     */
    public boolean updateSubject(Connection conn, Subject subject) throws SQLException {
        String sql = """
                UPDATE subjects
                SET subject_code = ?,
                    subject_name = ?,
                    branch = ?,
                    semester = ?
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getSubjectCode().trim());
            stmt.setString(2, subject.getSubjectName().trim());
            stmt.setString(3, subject.getBranch().trim());
            stmt.setInt(4, subject.getSemester());
            stmt.setInt(5, subject.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a subject by primary key ID.
     */
    public boolean deleteSubject(Connection conn, int subjectId) throws SQLException {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks if any attendance records reference the given subject ID.
     */
    public boolean hasAttendanceRecords(Connection conn, int subjectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE subject_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Subject mapRowToSubject(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id"));
        s.setSubjectCode(rs.getString("subject_code"));
        s.setSubjectName(rs.getString("subject_name"));
        s.setBranch(rs.getString("branch"));
        s.setSemester(rs.getInt("semester"));

        java.sql.Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            s.setCreatedAt(created.toLocalDateTime());
        }

        return s;
    }
}
