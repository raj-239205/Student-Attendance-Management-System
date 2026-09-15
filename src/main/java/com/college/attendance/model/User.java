package com.college.attendance.model;

import java.time.LocalDateTime;

/**
 * Represents a system user (admin or faculty).
 * Maps to the {@code users} table.
 */
public class User {

    private int id;
    private String username;
    private String password;
    private String fullName;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Supported user roles. */
    public enum Role {
        ADMIN, FACULTY
    }

    public User() {
    }

    public User(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
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
                + '}';
    }
}
