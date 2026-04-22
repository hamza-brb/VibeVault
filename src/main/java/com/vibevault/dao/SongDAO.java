package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SongDAO {
    private final DatabaseManager databaseManager;

    public SongDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Song create(Song song) {
        String sql = "INSERT INTO songs(title, artist_id, duration_seconds, file_path, track_number, year) " +
                "VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, song.getTitle());
            statement.setInt(2, song.getArtistId());
            statement.setObject(3, song.getDurationSeconds());
            statement.setString(4, song.getFilePath());
            statement.setObject(5, song.getTrackNumber());
            statement.setObject(6, song.getYear());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    song.setSongId(keys.getInt(1));
                }
            }
            return song;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create song", e);
        }
    }

    public Optional<Song> findById(int songId) {
        String sql = "SELECT song_id, title, artist_id, duration_seconds, file_path, track_number, year " +
                "FROM songs WHERE song_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find song by id", e);
        }
    }

    public List<Song> findByUserLibrary(int userId) {
        String sql = "SELECT s.song_id, s.title, s.artist_id, s.duration_seconds, s.file_path, s.track_number, s.year " +
                "FROM songs s JOIN user_library ul ON s.song_id = ul.song_id WHERE ul.user_id = ? ORDER BY s.title";
        List<Song> songs = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapRow(rs));
                }
            }
            return songs;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list songs in user library", e);
        }
    }

    public Optional<Song> findByFilePath(String filePath) {
        String sql = "SELECT song_id, title, artist_id, duration_seconds, file_path, track_number, year " +
                "FROM songs WHERE file_path = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, filePath);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find song by file path", e);
        }
    }

    public boolean addToUserLibrary(int userId, int songId) {
        String sql = "INSERT INTO user_library(user_id, song_id) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add song to user library", e);
        }
    }

    public boolean addToUserLibraryIfMissing(int userId, int songId) {
        String sql = "INSERT OR IGNORE INTO user_library(user_id, song_id) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add song to user library", e);
        }
    }

    public boolean removeFromUserLibrary(int userId, int songId) {
        String sql = "DELETE FROM user_library WHERE user_id = ? AND song_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove song from user library", e);
        }
    }

    public boolean isInUserLibrary(int userId, int songId) {
        String sql = "SELECT 1 FROM user_library WHERE user_id = ? AND song_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check if song is in user library", e);
        }
    }

    public boolean delete(int songId) {
        String sql = "DELETE FROM songs WHERE song_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, songId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete song", e);
        }
    }

    private static Song mapRow(ResultSet rs) throws SQLException {
        return new Song(
                rs.getInt("song_id"),
                rs.getString("title"),
                rs.getInt("artist_id"),
                (Integer) rs.getObject("duration_seconds"),
                rs.getString("file_path"),
                (Integer) rs.getObject("track_number"),
                (Integer) rs.getObject("year")
        );
    }
}
