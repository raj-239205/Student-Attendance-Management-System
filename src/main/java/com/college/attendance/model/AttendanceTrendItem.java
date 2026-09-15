package com.college.attendance.model;

import java.time.LocalDate;

/**
 * Data Transfer Object representing daily aggregate attendance metrics
 * for time-series trend reporting and line charts.
 */
public class AttendanceTrendItem {

    private final LocalDate date;
    private final int totalCount;
    private final int presentCount;
    private final int absentCount;
    private final double attendancePercentage;

    public AttendanceTrendItem(LocalDate date, int totalCount, int presentCount, int absentCount) {
        this.date = date;
        this.totalCount = totalCount;
        this.presentCount = presentCount;
        this.absentCount = absentCount;

        if (totalCount <= 0) {
            this.attendancePercentage = 0.0;
        } else {
            double pct = ((double) presentCount / totalCount) * 100.0;
            this.attendancePercentage = Math.round(pct * 100.0) / 100.0;
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public int getTotalCount() {
        return totalCount;
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
}
