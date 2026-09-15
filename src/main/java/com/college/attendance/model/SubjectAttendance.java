package com.college.attendance.model;

/**
 * Breakdown of a student's attendance metrics for a specific subject.
 */
public class SubjectAttendance {

    private final int subjectId;
    private final String subjectCode;
    private final String subjectName;
    private final int totalClasses;
    private final int presentClasses;
    private final int absentClasses;
    private final double percentage;

    public SubjectAttendance(int subjectId, String subjectCode, String subjectName,
                             int totalClasses, int presentClasses, int absentClasses) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.presentClasses = presentClasses;
        this.absentClasses = absentClasses;
        this.percentage = (totalClasses > 0)
                ? ((double) presentClasses / totalClasses) * 100.0
                : 0.0;
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

    public int getTotalClasses() {
        return totalClasses;
    }

    public int getPresentClasses() {
        return presentClasses;
    }

    public int getAbsentClasses() {
        return absentClasses;
    }

    public double getPercentage() {
        return percentage;
    }

    public boolean isDefaulter(double threshold) {
        return totalClasses > 0 && percentage < threshold;
    }
}
