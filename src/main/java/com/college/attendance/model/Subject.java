package com.college.attendance.model;

import java.time.LocalDateTime;

/**
 * Represents a course/subject offered in a branch and semester.
 * Maps to the {@code subjects} table.
 */
public class Subject {

    private int id;
    private String subjectCode;
    private String subjectName;
    private String branch;
    private int semester;
    private LocalDateTime createdAt;

    public Subject() {
    }

    public Subject(String subjectCode, String subjectName, String branch, int semester) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.branch = branch;
        this.semester = semester;
    }

    // --- Getters & Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Subject{id=" + id
                + ", code='" + subjectCode + '\''
                + ", name='" + subjectName + '\''
                + ", branch='" + branch + '\''
                + ", semester=" + semester
                + '}';
    }
}
