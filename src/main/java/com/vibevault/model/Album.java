package com.vibevault.model;

public class Album {
    private Integer albumId;
    private String title;
    private Integer artistId;
    private Integer releaseYear;
    private String coverArtPath;

    public Album() {
    }

    public Album(Integer albumId, String title, Integer artistId, Integer releaseYear, String coverArtPath) {
        this.albumId = albumId;
        this.title = title;
        this.artistId = artistId;
        this.releaseYear = releaseYear;
        this.coverArtPath = coverArtPath;
    }

    public Integer getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
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

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getCoverArtPath() {
        return coverArtPath;
    }

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }
}
