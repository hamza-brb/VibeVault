package com.vibevault.model;

public class Song {
    private Integer songId;
    private String title;
    private Integer artistId;
    private Integer durationSeconds;
    private String filePath;

    public Song() {
    }

    public Song(Integer songId, String title, Integer artistId,
                Integer durationSeconds, String filePath) {
        this.songId = songId;
        this.title = title;
        this.artistId = artistId;
        this.durationSeconds = durationSeconds;
        this.filePath = filePath;
    }

    public Integer getSongId() {
        return songId;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
