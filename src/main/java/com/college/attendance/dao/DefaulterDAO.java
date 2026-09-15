package com.college.attendance.dao;

import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.DefaulterSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Data Access Object for querying aggregated attendance metrics and deriving
 * student defaulter standing.
 */
public class DefaulterDAO {

    /**
     * Retrieves student attendance performance records across subjects, applies threshold comparisons,
     * filters by status and keyword, and sorts with lowest attendance first.
     */
    public List<DefaulterRecord> getDefaulterRecords(Connection conn, Integer subjectId, double threshold,
                                                     String statusFilter, String searchKeyword) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id AS student_id, s.roll_no, s.name AS student_name, s.branch, s.semester,
                       sub.id AS subject_id, sub.subject_code, sub.subject_name,
                       COUNT(a.id) AS total_classes,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_classes,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_classes
                FROM attendance a
                JOIN students s ON a.student_id = s.id
                JOIN subjects sub ON a.subject_id = sub.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (subjectId != null && subjectId > 0) {
            sql.append(" AND sub.id = ?");
            params.add(subjectId);
        }

        if (searchKeyword != null && !searchKeyword.isBlank()) {
            sql.append(" AND (s.roll_no LIKE ? OR s.name LIKE ? OR sub.subject_code LIKE ? OR sub.subject_name LIKE ?)");
            String pattern = "%" + searchKeyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("""
                 GROUP BY s.id, s.roll_no, s.name, s.branch, s.semester,
                          sub.id, sub.subject_code, sub.subject_name
                """);

        List<DefaulterRecord> allRecords = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int stuId = rs.getInt("student_id");
                    String roll = rs.getString("roll_no");
                    String name = rs.getString("student_name");
                    String branch = rs.getString("branch");
                    int sem = rs.getInt("semester");

                    int subId = rs.getInt("subject_id");
                    String subCode = rs.getString("subject_code");
                    String subName = rs.getString("subject_name");

                    int total = rs.getInt("total_classes");
                    int present = rs.getInt("present_classes");
                    int absent = rs.getInt("absent_classes");

                    DefaulterRecord record = new DefaulterRecord(
                            stuId, roll, name, branch, sem,
                            subId, subCode, subName,
                            total, present, absent, threshold
                    );
                    allRecords.add(record);
                }
            }
        }

        // Apply status filter in memory
        List<DefaulterRecord> filtered = new ArrayList<>();
        for (DefaulterRecord r : allRecords) {
            if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter)) {
                filtered.add(r);
            } else if ("DEFAULTER".equalsIgnoreCase(statusFilter) && r.getStatus() == DefaulterRecord.Status.DEFAULTER) {
                filtered.add(r);
            } else if ("SAFE".equalsIgnoreCase(statusFilter) && r.getStatus() == DefaulterRecord.Status.SAFE) {
                filtered.add(r);
            } else if ("NO_DATA".equalsIgnoreCase(statusFilter) && r.getStatus() == DefaulterRecord.Status.NO_DATA) {
                filtered.add(r);
            }
        }

        // Sort: lowest attendance percentage first (default)
        filtered.sort(Comparator.comparingDouble(DefaulterRecord::getAttendancePercentage)
                .thenComparing(DefaulterRecord::getRollNo));

        return filtered;
    }

    /**
     * Calculates aggregate defaulter summary statistics (total evaluated, defaulters count, safe count, rate).
     */
    public DefaulterSummary getDefaulterSummary(Connection conn, Integer subjectId, double threshold) throws SQLException {
        List<DefaulterRecord> records = getDefaulterRecords(conn, subjectId, threshold, "ALL", null);
        if (records.isEmpty()) {
            return DefaulterSummary.empty(threshold);
        }

        int total = records.size();
        int defaulters = 0;
        int safe = 0;

        for (DefaulterRecord r : records) {
            if (r.getStatus() == DefaulterRecord.Status.DEFAULTER) {
                defaulters++;
            } else if (r.getStatus() == DefaulterRecord.Status.SAFE) {
                safe++;
            }
        }

        return new DefaulterSummary(total, defaulters, safe, threshold);
    }

    /**
     * Retrieves course-wise defaulter records for a specific student.
     */
    public List<DefaulterRecord> getStudentDefaulterBreakdown(Connection conn, int studentId, double threshold) throws SQLException {
        String sql = """
                SELECT s.id AS student_id, s.roll_no, s.name AS student_name, s.branch, s.semester,
                       sub.id AS subject_id, sub.subject_code, sub.subject_name,
                       COUNT(a.id) AS total_classes,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_classes,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_classes
                FROM attendance a
                JOIN students s ON a.student_id = s.id
                JOIN subjects sub ON a.subject_id = sub.id
                WHERE s.id = ?
                GROUP BY s.id, s.roll_no, s.name, s.branch, s.semester,
                         sub.id, sub.subject_code, sub.subject_name
                ORDER BY sub.subject_code ASC
                """;

        List<DefaulterRecord> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int stuId = rs.getInt("student_id");
                    String roll = rs.getString("roll_no");
                    String name = rs.getString("student_name");
                    String branch = rs.getString("branch");
                    int sem = rs.getInt("semester");

                    int subId = rs.getInt("subject_id");
                    String subCode = rs.getString("subject_code");
                    String subName = rs.getString("subject_name");

                    int total = rs.getInt("total_classes");
                    int present = rs.getInt("present_classes");
                    int absent = rs.getInt("absent_classes");

                    list.add(new DefaulterRecord(
                            stuId, roll, name, branch, sem,
                            subId, subCode, subName,
                            total, present, absent, threshold
                    ));
                }
            }
        }
        return list;
    }
}
