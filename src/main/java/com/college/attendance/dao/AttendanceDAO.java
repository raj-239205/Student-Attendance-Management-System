package com.college.attendance.dao;

import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceRecord;
import com.college.attendance.model.Student;
import com.college.attendance.model.Subject;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Attendance entity and associated academic logs.
 * Handles transactional batch marking, status updates, single deletions,
 * and comprehensive history queries.
 */
public class AttendanceDAO {

    /**
     * Saves or updates a batch of attendance records in a single database transaction.
     * Uses ON DUPLICATE KEY UPDATE to allow overwriting or re-marking on the same date.
     *
     * @param conn the active database connection
     * @param records the list of attendance items to save
     * @return number of affected rows
     * @throws SQLException on database error
     */
    public int saveAttendanceBatch(Connection conn, List<Attendance> records) throws SQLException {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT INTO attendance (student_id, subject_id, attendance_date, status)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    status = VALUES(status),
                    marked_at = CURRENT_TIMESTAMP
                """;

        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Attendance att : records) {
                    stmt.setInt(1, att.getStudentId());
                    stmt.setInt(2, att.getSubjectId());
                    stmt.setDate(3, Date.valueOf(att.getAttendanceDate()));
                    stmt.setString(4, att.getStatus().name());
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                conn.commit();

                int totalSaved = 0;
                for (int res : results) {
                    if (res >= 0 || res == Statement.SUCCESS_NO_INFO) {
                        totalSaved++;
                    }
                }
                return totalSaved;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Retrieves an attendance record by its primary key.
     */
    public Attendance getAttendanceById(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, student_id, subject_id, attendance_date, status, marked_at FROM attendance WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapAttendance(rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks whether an attendance record already exists for the given student, subject, and date.
     */
    public boolean existsAttendance(Connection conn, int studentId, int subjectId, LocalDate date) throws SQLException {
        String sql = "SELECT id FROM attendance WHERE student_id = ? AND subject_id = ? AND attendance_date = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, subjectId);
            stmt.setDate(3, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Updates the status of a specific attendance record.
     */
    public boolean updateStatus(Connection conn, int id, Attendance.Status status) throws SQLException {
        String sql = "UPDATE attendance SET status = ?, marked_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a single attendance record by ID.
     */
    public boolean deleteAttendance(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves existing attendance records for a specific subject and date.
     */
    public List<AttendanceRecord> getAttendanceBySubjectAndDate(Connection conn, int subjectId, LocalDate date) throws SQLException {
        String sql = """
                SELECT a.id, a.student_id, s.roll_no, s.name AS student_name,
                       a.subject_id, sub.subject_code, sub.subject_name,
                       a.attendance_date, a.status, a.marked_at
                FROM attendance a
                JOIN students s ON a.student_id = s.id
                JOIN subjects sub ON a.subject_id = sub.id
                WHERE a.subject_id = ? AND a.attendance_date = ?
                ORDER BY s.roll_no ASC
                """;
        List<AttendanceRecord> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendanceRecord(rs));
                }
            }
        }
        return list;
    }

    /**
     * Queries attendance history with optional filters (subjectId, date, keyword).
     */
    public List<AttendanceRecord> getAttendanceHistory(Connection conn, Integer subjectId, LocalDate date, String keyword, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.student_id, s.roll_no, s.name AS student_name,
                       a.subject_id, sub.subject_code, sub.subject_name,
                       a.attendance_date, a.status, a.marked_at
                FROM attendance a
                JOIN students s ON a.student_id = s.id
                JOIN subjects sub ON a.subject_id = sub.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (subjectId != null && subjectId > 0) {
            sql.append(" AND a.subject_id = ?");
            params.add(subjectId);
        }

        if (date != null) {
            sql.append(" AND a.attendance_date = ?");
            params.add(Date.valueOf(date));
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (s.roll_no LIKE ? OR s.name LIKE ? OR sub.subject_code LIKE ? OR sub.subject_name LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY a.attendance_date DESC, a.marked_at DESC, s.roll_no ASC LIMIT ?");
        params.add(limit > 0 ? limit : 200);

        List<AttendanceRecord> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendanceRecord(rs));
                }
            }
        }
        return list;
    }

    /**
     * Resolves enrolled students associated with a subject based on matching branch and semester.
     * If no students are enrolled in that specific branch/semester, returns all active students as fallback.
     */
    public List<Student> getStudentsForSubject(Connection conn, int subjectId) throws SQLException {
        // First retrieve the subject to know its branch and semester
        String subSql = "SELECT branch, semester FROM subjects WHERE id = ?";
        String branch = null;
        int semester = 0;

        try (PreparedStatement stmt = conn.prepareStatement(subSql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    branch = rs.getString("branch");
                    semester = rs.getInt("semester");
                }
            }
        }

        List<Student> students = new ArrayList<>();
        if (branch != null && semester > 0) {
            String stuSql = "SELECT id, roll_no, name, branch, semester, section, email, phone FROM students WHERE branch = ? AND semester = ? ORDER BY roll_no ASC";
            try (PreparedStatement stmt = conn.prepareStatement(stuSql)) {
                stmt.setString(1, branch);
                stmt.setInt(2, semester);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        students.add(mapStudent(rs));
                    }
                }
            }
        }

        // Fallback: If no student matches exact branch/sem, return all students
        if (students.isEmpty()) {
            String allSql = "SELECT id, roll_no, name, branch, semester, section, email, phone FROM students ORDER BY roll_no ASC";
            try (PreparedStatement stmt = conn.prepareStatement(allSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapStudent(rs));
                }
            }
        }

        return students;
    }

    private Attendance mapAttendance(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setSubjectId(rs.getInt("subject_id"));
        a.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
        a.setStatus(Attendance.Status.valueOf(rs.getString("status")));
        java.sql.Timestamp ts = rs.getTimestamp("marked_at");
        if (ts != null) {
            a.setMarkedAt(ts.toLocalDateTime());
        }
        return a;
    }

    private AttendanceRecord mapAttendanceRecord(ResultSet rs) throws SQLException {
        AttendanceRecord ar = new AttendanceRecord();
        ar.setId(rs.getInt("id"));
        ar.setStudentId(rs.getInt("student_id"));
        ar.setRollNo(rs.getString("roll_no"));
        ar.setStudentName(rs.getString("student_name"));
        ar.setSubjectId(rs.getInt("subject_id"));
        ar.setSubjectCode(rs.getString("subject_code"));
        ar.setSubjectName(rs.getString("subject_name"));
        ar.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
        ar.setStatus(Attendance.Status.valueOf(rs.getString("status")));
        java.sql.Timestamp ts = rs.getTimestamp("marked_at");
        if (ts != null) {
            ar.setMarkedAt(ts.toLocalDateTime());
        }
        return ar;
    }

    private Student mapStudent(ResultSet rs) throws SQLException {
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
