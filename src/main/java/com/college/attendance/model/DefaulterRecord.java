package com.college.attendance.model;

/**
 * Encapsulates attendance performance and compliance metrics for a student in a course subject.
 * Used for dynamic defaulter calculations, threshold comparisons, and reporting.
 */
public class DefaulterRecord {

    public enum Status {
        DEFAULTER,
        SAFE,
        NO_DATA
    }

    private int studentId;
    private String rollNo;
    private String studentName;
    private String branch;
    private int semester;

    private int subjectId;
    private String subjectCode;
    private String subjectName;

    private int totalClasses;
    private int presentClasses;
    private int absentClasses;

    private double attendancePercentage;
    private double requiredPercentage;
    private double deficit;
    private Status status;

    public DefaulterRecord() {
    }

    public DefaulterRecord(int studentId, String rollNo, String studentName, String branch, int semester,
                           int subjectId, String subjectCode, String subjectName,
                           int totalClasses, int presentClasses, int absentClasses,
                           double requiredPercentage) {
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
        this.requiredPercentage = requiredPercentage;

        if (totalClasses <= 0) {
            this.attendancePercentage = 0.0;
            this.deficit = 0.0;
            this.status = Status.NO_DATA;
        } else {
            double pct = ((double) presentClasses / totalClasses) * 100.0;
            this.attendancePercentage = Math.round(pct * 100.0) / 100.0;
            if (this.attendancePercentage < requiredPercentage) {
                this.status = Status.DEFAULTER;
                double diff = requiredPercentage - this.attendancePercentage;
                this.deficit = Math.round(diff * 100.0) / 100.0;
            } else {
                this.status = Status.SAFE;
                this.deficit = 0.0;
            }
        }
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

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
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

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getPresentClasses() {
        return presentClasses;
    }

    public void setPresentClasses(int presentClasses) {
        this.presentClasses = presentClasses;
    }

    public int getAbsentClasses() {
        return absentClasses;
    }

    public void setAbsentClasses(int absentClasses) {
        this.absentClasses = absentClasses;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public double getRequiredPercentage() {
        return requiredPercentage;
    }

    public void setRequiredPercentage(double requiredPercentage) {
        this.requiredPercentage = requiredPercentage;
    }

    public double getDeficit() {
        return deficit;
    }

    public void setDeficit(double deficit) {
        this.deficit = deficit;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
