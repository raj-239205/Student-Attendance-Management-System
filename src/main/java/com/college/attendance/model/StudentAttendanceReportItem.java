package com.college.attendance.model;

/**
 * Report row item representing a student's attendance performance in a specific subject or overall.
 */
public class StudentAttendanceReportItem {

    private final int studentId;
    private final String rollNo;
    private final String studentName;
    private final String branch;
    private final int semester;
    private final int subjectId;
    private final String subjectCode;
    private final String subjectName;
    private final int totalClasses;
    private final int presentClasses;
    private final int absentClasses;
    private final double attendancePercentage;
    private final double deficit;
    private final DefaulterRecord.Status status;

    public StudentAttendanceReportItem(int studentId, String rollNo, String studentName,
                                      String branch, int semester,
                                      int subjectId, String subjectCode, String subjectName,
                                      int totalClasses, int presentClasses, int absentClasses,
                                      double threshold) {
        this.studentId = studentId;
        this.rollNo = rollNo;
        this.studentName = studentName;
        this.branch = branch;
        this.semester = semester;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.presentClasses = presentClasses;
        this.absentClasses = absentClasses;

        if (totalClasses <= 0) {
            this.attendancePercentage = 0.0;
            this.deficit = 0.0;
            this.status = DefaulterRecord.Status.NO_DATA;
        } else {
            double pct = ((double) presentClasses / totalClasses) * 100.0;
            this.attendancePercentage = Math.round(pct * 100.0) / 100.0;

            if (this.attendancePercentage < threshold) {
                this.status = DefaulterRecord.Status.DEFAULTER;
                double diff = threshold - this.attendancePercentage;
                this.deficit = Math.round(diff * 100.0) / 100.0;
            } else {
                this.status = DefaulterRecord.Status.SAFE;
                this.deficit = 0.0;
            }
        }
    }

    public int getStudentId() {
        return studentId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getBranch() {
        return branch;
    }

    public int getSemester() {
        return semester;
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

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public double getDeficit() {
        return deficit;
    }

    public DefaulterRecord.Status getStatus() {
        return status;
    }
}
