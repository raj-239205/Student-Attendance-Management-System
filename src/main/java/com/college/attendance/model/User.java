package com.college.attendance.model;

import java.time.LocalDateTime;

/**
 * Represents a system user account (ADMIN, FACULTY, or STUDENT).
 * Maps to the {@code users} table.
 *
 * <p>
 * For STUDENT accounts, {@code studentId} holds the foreign key reference
 * to the associated student record in the {@code students} table.
 * For ADMIN and FACULTY accounts, {@code studentId} is {@code null}.
 * </p>
 */
public class User {

    private int id;
    private String username;
    private String password;
    private String fullName;
    private Role role;
    private Integer studentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Supported user roles. */
    public enum Role {
        ADMIN, FACULTY, STUDENT
    }

    public User() {
    }

    public User(String username, String password, String fullName, Role role) {
        this(username, password, fullName, role, null);
    }

    public User(String username, String password, String fullName, Role role, Integer studentId) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.studentId = studentId;
    }

    // --- Getters & Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
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
        return "User{id=" + id
                + ", username='" + username + '\''
                + ", fullName='" + fullName + '\''
                + ", role=" + role
                + ", studentId=" + studentId
                + '}';
    }
}
