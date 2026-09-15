package com.college.attendance.model;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated attendance summary and personal metrics for an authenticated student.
 */
public class StudentAttendanceSummary {

    private final Student student;
    private final int totalClasses;
    private final int presentClasses;
    private final int absentClasses;
    private final double overallPercentage;
    private final List<SubjectAttendance> subjectAttendances;

    public StudentAttendanceSummary(Student student, int totalClasses, int presentClasses,
                                  int absentClasses, List<SubjectAttendance> subjectAttendances) {
        this.student = student;
        this.totalClasses = totalClasses;
        this.presentClasses = presentClasses;
        this.absentClasses = absentClasses;
        this.overallPercentage = (totalClasses > 0)
                ? ((double) presentClasses / totalClasses) * 100.0
                : 0.0;
        this.subjectAttendances = (subjectAttendances != null)
                ? Collections.unmodifiableList(subjectAttendances)
                : Collections.emptyList();
    }

    public static StudentAttendanceSummary empty(Student student) {
        return new StudentAttendanceSummary(student, 0, 0, 0, Collections.emptyList());
    }

    public Student getStudent() {
        return student;
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

    public double getOverallPercentage() {
        return overallPercentage;
    }

    public boolean isDefaulter(double threshold) {
        return totalClasses > 0 && overallPercentage < threshold;
    }

    public List<SubjectAttendance> getSubjectAttendances() {
        return subjectAttendances;
    }
}
