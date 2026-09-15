package com.college.attendance.model;

import java.time.LocalDateTime;

/**
 * Represents an enrolled student.
 * Maps to the {@code students} table.
 *
 * <p>Attendance percentage is NOT stored here — it is calculated
 * dynamically from the {@code attendance} table.</p>
 */
public class Student {

    private int id;
    private String rollNo;
    private String name;
    private String branch;
    private int semester;
    private String section;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Student() {
    }

    public Student(String rollNo, String name, String branch, int semester, String section) {
        this.rollNo = rollNo;
        this.name = name;
        this.branch = branch;
        this.semester = semester;
        this.section = section;
    }

    // --- Getters & Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Student{id=" + id
                + ", rollNo='" + rollNo + '\''
                + ", name='" + name + '\''
                + ", branch='" + branch + '\''
                + ", semester=" + semester
                + ", section='" + section + '\''
                + '}';
    }
}
