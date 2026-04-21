package com.vibevault.model;

public class Playlist {
    private Integer playlistId;
    private Integer userId;
    private String name;
    private String createdAt;

    public Playlist() {
    }

    public Playlist(Integer playlistId, Integer userId, String name, String createdAt) {
        this.playlistId = playlistId;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Integer getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Integer playlistId) {
        this.playlistId = playlistId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
