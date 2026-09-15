package com.college.attendance.dao;

import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.AttendanceTrendItem;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.OverallAttendanceSummary;
import com.college.attendance.model.StudentAttendanceReportItem;
import com.college.attendance.model.SubjectAttendanceReportItem;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object executing high-efficiency aggregated reporting queries
 * with multi-criteria filters (subject, student, date range, threshold, status).
 */
public class ReportDAO {

    /**
     * Calculates the overall attendance summary metrics.
     */
    public OverallAttendanceSummary getOverallSummary(Connection conn, Integer subjectId,
                                                      LocalDate startDate, LocalDate endDate,
                                                      double threshold) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT a.student_id) AS total_students,
                       COUNT(DISTINCT a.subject_id) AS total_subjects,
                       COUNT(a.id) AS total_records,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count
                FROM attendance a
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        appendCommonFilters(sql, params, subjectId, null, startDate, endDate);

        int totalStudents = 0;
        int totalSubjects = 0;
        int totalRecords = 0;
        int presentCount = 0;
        int absentCount = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalStudents = rs.getInt("total_students");
                    totalSubjects = rs.getInt("total_subjects");
                    totalRecords = rs.getInt("total_records");
                    presentCount = rs.getInt("present_count");
                    absentCount = rs.getInt("absent_count");
                }
            }
        }

        // If no filter on subject, ensure totalStudents and totalSubjects reflect the master catalogs if empty records
        if (subjectId == null && startDate == null && endDate == null) {
            try (PreparedStatement sStmt = conn.prepareStatement("SELECT COUNT(*) FROM students");
                 ResultSet rs = sStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > totalStudents) {
                    totalStudents = rs.getInt(1);
                }
            }
            try (PreparedStatement subStmt = conn.prepareStatement("SELECT COUNT(*) FROM subjects");
                 ResultSet rs = subStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > totalSubjects) {
                    totalSubjects = rs.getInt(1);
                }
            }
        }

        // Calculate student defaulter count within current scope
        int defaulters = 0;
        int safe = 0;

        StringBuilder defSql = new StringBuilder("""
                SELECT student_id,
                       (SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*)) * 100.0 AS pct
                FROM attendance a
                WHERE 1=1
                """);
        List<Object> defParams = new ArrayList<>();
        appendCommonFilters(defSql, defParams, subjectId, null, startDate, endDate);
        defSql.append(" GROUP BY student_id");

        try (PreparedStatement stmt = conn.prepareStatement(defSql.toString())) {
            for (int i = 0; i < defParams.size(); i++) {
                stmt.setObject(i + 1, defParams.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double pct = rs.getDouble("pct");
                    if (pct < threshold) {
                        defaulters++;
                    } else {
                        safe++;
                    }
                }
            }
        }

        return new OverallAttendanceSummary(
                totalStudents, totalSubjects, totalRecords,
                presentCount, absentCount, defaulters, safe, threshold
        );
    }

    /**
     * Retrieves student-wise attendance records with filters and status classification.
     */
    public List<StudentAttendanceReportItem> getStudentAttendanceReports(Connection conn, Integer subjectId,
                                                                        Integer studentId,
                                                                        LocalDate startDate, LocalDate endDate,
                                                                        String statusFilter, String searchKeyword,
                                                                        double threshold) throws SQLException {
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
        appendCommonFilters(sql, params, subjectId, studentId, startDate, endDate);

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

        List<StudentAttendanceReportItem> allItems = new ArrayList<>();
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
                    String code = rs.getString("subject_code");
                    String sName = rs.getString("subject_name");

                    int total = rs.getInt("total_classes");
                    int present = rs.getInt("present_classes");
                    int absent = rs.getInt("absent_classes");

                    allItems.add(new StudentAttendanceReportItem(
                            stuId, roll, name, branch, sem,
                            subId, code, sName,
                            total, present, absent, threshold
                    ));
                }
            }
        }

        // Apply status filter
        List<StudentAttendanceReportItem> filtered = new ArrayList<>();
        for (StudentAttendanceReportItem item : allItems) {
            if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter)) {
                filtered.add(item);
            } else if ("DEFAULTER".equalsIgnoreCase(statusFilter) && item.getStatus() == DefaulterRecord.Status.DEFAULTER) {
                filtered.add(item);
            } else if ("SAFE".equalsIgnoreCase(statusFilter) && item.getStatus() == DefaulterRecord.Status.SAFE) {
                filtered.add(item);
            } else if ("NO_DATA".equalsIgnoreCase(statusFilter) && item.getStatus() == DefaulterRecord.Status.NO_DATA) {
                filtered.add(item);
            }
        }

        // Sort: lowest attendance percentage first, then roll number
        filtered.sort(Comparator.comparingDouble(StudentAttendanceReportItem::getAttendancePercentage)
                .thenComparing(StudentAttendanceReportItem::getRollNo));

        return filtered;
    }

    /**
     * Retrieves subject-wise performance reports including total records, percentage, and defaulter counts.
     */
    public List<SubjectAttendanceReportItem> getSubjectAttendanceReports(Connection conn,
                                                                        LocalDate startDate, LocalDate endDate,
                                                                        double threshold) throws SQLException {
        // 1. Query overall attendance per subject
        StringBuilder sql = new StringBuilder("""
                SELECT sub.id AS subject_id, sub.subject_code, sub.subject_name, sub.branch, sub.semester,
                       COUNT(a.id) AS total_records,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count
                FROM subjects sub
                LEFT JOIN attendance a ON sub.id = a.subject_id
                """);

        List<Object> params = new ArrayList<>();
        if (startDate != null || endDate != null) {
            sql.append(" AND 1=1");
            if (startDate != null) {
                sql.append(" AND a.attendance_date >= ?");
                params.add(Date.valueOf(startDate));
            }
            if (endDate != null) {
                sql.append(" AND a.attendance_date <= ?");
                params.add(Date.valueOf(endDate));
            }
        }

        sql.append(" GROUP BY sub.id, sub.subject_code, sub.subject_name, sub.branch, sub.semester");

        // 2. Query student-level performance per subject to calculate defaulters per subject
        StringBuilder stuSubSql = new StringBuilder("""
                SELECT a.subject_id, a.student_id,
                       COUNT(a.id) AS total_classes,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_classes
                FROM attendance a
                WHERE 1=1
                """);
        List<Object> stuSubParams = new ArrayList<>();
        appendCommonFilters(stuSubSql, stuSubParams, null, null, startDate, endDate);
        stuSubSql.append(" GROUP BY a.subject_id, a.student_id");

        Map<Integer, int[]> subjectStudentStats = new HashMap<>(); // subjectId -> [totalStudents, defaulters, safe]

        try (PreparedStatement stmt = conn.prepareStatement(stuSubSql.toString())) {
            for (int i = 0; i < stuSubParams.size(); i++) {
                stmt.setObject(i + 1, stuSubParams.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int subId = rs.getInt("subject_id");
                    int total = rs.getInt("total_classes");
                    int present = rs.getInt("present_classes");

                    int[] stats = subjectStudentStats.computeIfAbsent(subId, k -> new int[3]);
                    stats[0]++; // total students evaluated

                    if (total > 0) {
                        double pct = ((double) present / total) * 100.0;
                        if (pct < threshold) {
                            stats[1]++; // defaulters
                        } else {
                            stats[2]++; // safe
                        }
                    }
                }
            }
        }

        List<SubjectAttendanceReportItem> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int subId = rs.getInt("subject_id");
                    String code = rs.getString("subject_code");
                    String name = rs.getString("subject_name");
                    String branch = rs.getString("branch");
                    int sem = rs.getInt("semester");

                    int totalRec = rs.getInt("total_records");
                    int present = rs.getInt("present_count");
                    int absent = rs.getInt("absent_count");

                    int[] stats = subjectStudentStats.getOrDefault(subId, new int[]{0, 0, 0});

                    list.add(new SubjectAttendanceReportItem(
                            subId, code, name, branch, sem,
                            totalRec, present, absent,
                            stats[0], stats[1], stats[2]
                    ));
                }
            }
        }

        // Default sort: lowest attendance first
        list.sort(Comparator.comparingDouble(SubjectAttendanceReportItem::getAttendancePercentage)
                .thenComparing(SubjectAttendanceReportItem::getSubjectCode));

        return list;
    }

    /**
     * Retrieves status distribution (Present vs Absent counts) for charts.
     */
    public AttendanceDistribution getAttendanceDistribution(Connection conn, Integer subjectId,
                                                            LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count
                FROM attendance a
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        appendCommonFilters(sql, params, subjectId, null, startDate, endDate);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
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
     * Retrieves daily attendance records for time-series trend analysis.
     */
    public List<AttendanceTrendItem> getAttendanceTrends(Connection conn, Integer subjectId,
                                                         LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.attendance_date,
                       COUNT(a.id) AS total_count,
                       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count
                FROM attendance a
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        appendCommonFilters(sql, params, subjectId, null, startDate, endDate);
        sql.append(" GROUP BY a.attendance_date ORDER BY a.attendance_date ASC");

        List<AttendanceTrendItem> trends = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("attendance_date");
                    LocalDate date = (d != null) ? d.toLocalDate() : null;
                    if (date != null) {
                        int total = rs.getInt("total_count");
                        int present = rs.getInt("present_count");
                        int absent = rs.getInt("absent_count");
                        trends.add(new AttendanceTrendItem(date, total, present, absent));
                    }
                }
            }
        }
        return trends;
    }

    private void appendCommonFilters(StringBuilder sql, List<Object> params,
                                     Integer subjectId, Integer studentId,
                                     LocalDate startDate, LocalDate endDate) {
        if (subjectId != null && subjectId > 0) {
            sql.append(" AND a.subject_id = ?");
            params.add(subjectId);
        }
        if (studentId != null && studentId > 0) {
            sql.append(" AND a.student_id = ?");
            params.add(studentId);
        }
        if (startDate != null) {
            sql.append(" AND a.attendance_date >= ?");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND a.attendance_date <= ?");
            params.add(Date.valueOf(endDate));
        }
    }
}
