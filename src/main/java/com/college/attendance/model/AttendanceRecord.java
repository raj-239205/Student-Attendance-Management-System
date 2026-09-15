package com.college.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an attendance log entry with joined
 * student and subject details for display in tables, history, and reports.
 */
public class AttendanceRecord {

    private int id;
    private int studentId;
    private String rollNo;
    private String studentName;
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private LocalDate attendanceDate;
    private Attendance.Status status;
    private LocalDateTime markedAt;

    public AttendanceRecord() {
    }

    public AttendanceRecord(int id, int studentId, String rollNo, String studentName,
                            int subjectId, String subjectCode, String subjectName,
                            LocalDate attendanceDate, Attendance.Status status, LocalDateTime markedAt) {
        this.id = id;
        this.studentId = studentId;
        this.rollNo = rollNo;
        this.studentName = studentName;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.markedAt = markedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Attendance.Status getStatus() {
        return status;
    }

    public void setStatus(Attendance.Status status) {
        this.status = status;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }
}
