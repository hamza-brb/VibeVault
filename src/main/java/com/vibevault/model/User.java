package com.vibevault.model;

public class User {
    private Integer userId;
    private String username;
    private String passwordHash;
    private String createdAt;

    public User() {
    }

    public User(Integer userId, String username, String passwordHash, String createdAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
