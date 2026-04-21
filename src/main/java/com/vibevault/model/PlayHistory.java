package com.vibevault.model;

public class PlayHistory {
    private Integer playId;
    private Integer userId;
    private Integer songId;
    private String playedAt;
    private Integer durationListened;

    public PlayHistory() {
    }

    public PlayHistory(Integer playId, Integer userId, Integer songId, String playedAt, Integer durationListened) {
        this.playId = playId;
        this.userId = userId;
        this.songId = songId;
        this.playedAt = playedAt;
        this.durationListened = durationListened;
    }

    public Integer getPlayId() {
        return playId;
    }

    public void setPlayId(Integer playId) {
        this.playId = playId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getSongId() {
        return songId;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public String getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(String playedAt) {
        this.playedAt = playedAt;
    }

    public Integer getDurationListened() {
        return durationListened;
    }

    public void setDurationListened(Integer durationListened) {
        this.durationListened = durationListened;
    }
}
