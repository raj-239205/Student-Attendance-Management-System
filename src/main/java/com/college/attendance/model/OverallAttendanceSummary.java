package com.college.attendance.model;

/**
 * Data Transfer Object representing system-wide high-level attendance and defaulter metrics.
 */
public class OverallAttendanceSummary {

    private final int totalStudents;
    private final int totalSubjects;
    private final int totalAttendanceRecords;
    private final int presentCount;
    private final int absentCount;
    private final double overallAttendancePercentage;
    private final int totalDefaulters;
    private final int totalSafeStudents;
    private final double threshold;

    public OverallAttendanceSummary(int totalStudents, int totalSubjects, int totalAttendanceRecords,
                                    int presentCount, int absentCount, double threshold) {
        this(totalStudents, totalSubjects, totalAttendanceRecords, presentCount, absentCount,
                0, 0, threshold);
    }

    public OverallAttendanceSummary(int totalStudents, int totalSubjects, int totalAttendanceRecords,
                                    int presentCount, int absentCount,
                                    int totalDefaulters, int totalSafeStudents, double threshold) {
        this.totalStudents = totalStudents;
        this.totalSubjects = totalSubjects;
        this.totalAttendanceRecords = totalAttendanceRecords;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.totalDefaulters = totalDefaulters;
        this.totalSafeStudents = totalSafeStudents;
        this.threshold = threshold;

        if (totalAttendanceRecords <= 0) {
            this.overallAttendancePercentage = 0.0;
        } else {
            double pct = ((double) presentCount / totalAttendanceRecords) * 100.0;
            this.overallAttendancePercentage = Math.round(pct * 100.0) / 100.0;
        }
    }

    public static OverallAttendanceSummary empty(double threshold) {
        return new OverallAttendanceSummary(0, 0, 0, 0, 0, 0, 0, threshold);
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getTotalSubjects() {
        return totalSubjects;
    }

    public int getTotalAttendanceRecords() {
        return totalAttendanceRecords;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public double getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public int getTotalDefaulters() {
        return totalDefaulters;
    }

    public int getTotalSafeStudents() {
        return totalSafeStudents;
    }

    public double getThreshold() {
        return threshold;
    }
}
