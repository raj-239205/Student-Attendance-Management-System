package com.college.attendance.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Encapsulates the aggregated attendance statistics for an individual lecture session
 * identified by the combination of attendance date and subject.
 */
public class LectureAttendanceSummary {

    private final LocalDate date;
    private final int subjectId;
    private final String subjectCode;
    private final String subjectName;
    private final int presentCount;
    private final int absentCount;
    private final int totalCount;
    private final double attendancePercentage;

    public LectureAttendanceSummary(LocalDate date, int subjectId, String subjectCode, String subjectName,
                                    int presentCount, int absentCount, int totalCount) {
        this.date = Objects.requireNonNull(date, "date cannot be null");
        this.subjectId = subjectId;
        this.subjectCode = (subjectCode != null) ? subjectCode : "";
        this.subjectName = (subjectName != null) ? subjectName : "";
        this.presentCount = Math.max(0, presentCount);
        this.absentCount = Math.max(0, absentCount);
        this.totalCount = Math.max(0, totalCount);
        this.attendancePercentage = (this.totalCount > 0)
                ? ((double) this.presentCount / this.totalCount) * 100.0
                : 0.0;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    @Override
    public String toString() {
        return "LectureAttendanceSummary{" +
                "date=" + date +
                ", subjectCode='" + subjectCode + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", presentCount=" + presentCount +
                ", absentCount=" + absentCount +
                ", totalCount=" + totalCount +
                ", attendancePercentage=" + String.format("%.1f%%", attendancePercentage) +
                '}';
    }
}
