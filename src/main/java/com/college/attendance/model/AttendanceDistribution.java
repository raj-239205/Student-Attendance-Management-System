package com.college.attendance.model;

/**
 * Data Transfer Object representing status distribution breakdown
 * (Present vs Absent, Safe vs Defaulter) for donut and pie charts.
 */
public class AttendanceDistribution {

    private final int presentCount;
    private final int absentCount;
    private final int totalCount;
    private final double presentPercentage;
    private final double absentPercentage;

    public AttendanceDistribution(int presentCount, int absentCount) {
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.totalCount = presentCount + absentCount;

        if (this.totalCount <= 0) {
            this.presentPercentage = 0.0;
            this.absentPercentage = 0.0;
        } else {
            double pPct = ((double) presentCount / this.totalCount) * 100.0;
            double aPct = ((double) absentCount / this.totalCount) * 100.0;
            this.presentPercentage = Math.round(pPct * 100.0) / 100.0;
            this.absentPercentage = Math.round(aPct * 100.0) / 100.0;
        }
    }

    public static AttendanceDistribution empty() {
        return new AttendanceDistribution(0, 0);
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

    public double getPresentPercentage() {
        return presentPercentage;
    }

    public double getAbsentPercentage() {
        return absentPercentage;
    }
}
