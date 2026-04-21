package com.vibevault.model;

public class Song {
    private Integer songId;
    private String title;
    private Integer artistId;
    private Integer albumId;
    private String genre;
    private Integer durationSeconds;
    private String filePath;
    private Integer trackNumber;
    private Integer year;

    public Song() {
    }

    public Song(Integer songId, String title, Integer artistId, Integer albumId, String genre,
                Integer durationSeconds, String filePath, Integer trackNumber, Integer year) {
        this.songId = songId;
        this.title = title;
        this.artistId = artistId;
        this.albumId = albumId;
        this.genre = genre;
        this.durationSeconds = durationSeconds;
        this.filePath = filePath;
        this.trackNumber = trackNumber;
        this.year = year;
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

    public Integer getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
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

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
