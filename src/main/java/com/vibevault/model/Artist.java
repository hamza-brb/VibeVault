package com.vibevault.model;

public class Artist {
    private Integer artistId;
    private String name;
    private String bio;

    public Artist() {
    }

    public Artist(Integer artistId, String name, String bio) {
        this.artistId = artistId;
        this.name = name;
        this.bio = bio;
    }

    public Integer getArtistId() {
        return artistId;
    }

    public void setArtistId(Integer artistId) {
        this.artistId = artistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
