package com.college.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single attendance record for a student in a subject on a date.
 * Maps to the {@code attendance} table.
 *
 * <p>The combination (student_id, subject_id, attendance_date) is unique —
 * a student cannot have duplicate attendance for the same subject on the same day.</p>
 */
public class Attendance {

    private int id;
    private int studentId;
    private int subjectId;
    private LocalDate attendanceDate;
    private Status status;
    private LocalDateTime markedAt;

    /** Attendance status options. */
    public enum Status {
        PRESENT, ABSENT
    }

    public Attendance() {
    }

    public Attendance(int studentId, int subjectId, LocalDate attendanceDate, Status status) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.attendanceDate = attendanceDate;
        this.status = status;
    }

    // --- Getters & Setters ---

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

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    @Override
    public String toString() {
        return "Attendance{id=" + id
                + ", studentId=" + studentId
                + ", subjectId=" + subjectId
                + ", date=" + attendanceDate
                + ", status=" + status
                + '}';
    }
}
