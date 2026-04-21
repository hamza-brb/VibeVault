package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlbumDAO {
    private final DatabaseManager databaseManager;

    public AlbumDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Album create(Album album) {
        String sql = "INSERT INTO albums(title, artist_id, release_year, cover_art_path) VALUES(?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, album.getTitle());
            statement.setInt(2, album.getArtistId());
            if (album.getReleaseYear() == null) {
                statement.setObject(3, null);
            } else {
                statement.setInt(3, album.getReleaseYear());
            }
            statement.setString(4, album.getCoverArtPath());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    album.setAlbumId(keys.getInt(1));
                }
            }
            return album;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create album", e);
        }
    }

    public Optional<Album> findById(int albumId) {
        String sql = "SELECT album_id, title, artist_id, release_year, cover_art_path FROM albums WHERE album_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, albumId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find album by id", e);
        }
    }

    public List<Album> findByArtistId(int artistId) {
        String sql = "SELECT album_id, title, artist_id, release_year, cover_art_path FROM albums WHERE artist_id = ? ORDER BY title";
        List<Album> albums = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    albums.add(mapRow(rs));
                }
            }
            return albums;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find albums by artist", e);
        }
    }

    public Optional<Album> findByTitleAndArtistId(String title, int artistId) {
        String sql = "SELECT album_id, title, artist_id, release_year, cover_art_path FROM albums WHERE title = ? AND artist_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setInt(2, artistId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find album by title and artist", e);
        }
    }

    public List<Album> findAll() {
        String sql = "SELECT album_id, title, artist_id, release_year, cover_art_path FROM albums ORDER BY title";
        List<Album> albums = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                albums.add(mapRow(rs));
            }
            return albums;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list albums", e);
        }
    }

    public boolean delete(int albumId) {
        String sql = "DELETE FROM albums WHERE album_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, albumId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete album", e);
        }
    }

    public boolean updateCoverArtPath(int albumId, String coverArtPath) {
        String sql = "UPDATE albums SET cover_art_path = ? WHERE album_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, coverArtPath);
            statement.setInt(2, albumId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update album cover art path", e);
        }
    }

    private static Album mapRow(ResultSet rs) throws SQLException {
        return new Album(
                rs.getInt("album_id"),
                rs.getString("title"),
                rs.getInt("artist_id"),
                (Integer) rs.getObject("release_year"),
                rs.getString("cover_art_path")
        );
    }
}
