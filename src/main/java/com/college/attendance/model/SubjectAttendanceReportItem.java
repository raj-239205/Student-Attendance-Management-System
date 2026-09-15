package com.college.attendance.model;

/**
 * Report row item representing subject-level attendance aggregates,
 * average attendance percentages, and defaulter counts.
 */
public class SubjectAttendanceReportItem {

    private final int subjectId;
    private final String subjectCode;
    private final String subjectName;
    private final String branch;
    private final int semester;
    private final int totalRecords;
    private final int presentCount;
    private final int absentCount;
    private final double attendancePercentage;
    private final int totalStudents;
    private final int defaulterCount;
    private final int safeCount;
    private final double defaulterRate;

    public SubjectAttendanceReportItem(int subjectId, String subjectCode, String subjectName,
                                      String branch, int semester,
                                      int totalRecords, int presentCount, int absentCount,
                                      int totalStudents, int defaulterCount, int safeCount) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.branch = branch;
        this.semester = semester;
        this.totalRecords = totalRecords;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.totalStudents = totalStudents;
        this.defaulterCount = defaulterCount;
        this.safeCount = safeCount;

        if (totalRecords <= 0) {
            this.attendancePercentage = 0.0;
        } else {
            double pct = ((double) presentCount / totalRecords) * 100.0;
            this.attendancePercentage = Math.round(pct * 100.0) / 100.0;
        }

        if (totalStudents <= 0) {
            this.defaulterRate = 0.0;
        } else {
            double rate = ((double) defaulterCount / totalStudents) * 100.0;
            this.defaulterRate = Math.round(rate * 100.0) / 100.0;
        }
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

    public String getBranch() {
        return branch;
    }

    public int getSemester() {
        return semester;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getDefaulterCount() {
        return defaulterCount;
    }

    public int getSafeCount() {
        return safeCount;
    }

    public double getDefaulterRate() {
        return defaulterRate;
    }
}
