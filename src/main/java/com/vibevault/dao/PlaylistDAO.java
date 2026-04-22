package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Playlist;
import com.vibevault.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaylistDAO {
    private final DatabaseManager databaseManager;

    public PlaylistDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Playlist create(Playlist playlist) {
        String sql = "INSERT INTO playlists(user_id, name) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, playlist.getUserId());
            statement.setString(2, playlist.getName());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    playlist.setPlaylistId(keys.getInt(1));
                }
            }
            return playlist;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create playlist", e);
        }
    }

    public Optional<Playlist> findById(int playlistId) {
        String sql = "SELECT playlist_id, user_id, name, created_at FROM playlists WHERE playlist_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPlaylist(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find playlist by id", e);
        }
    }

    public List<Playlist> findByUserId(int userId) {
        String sql = "SELECT playlist_id, user_id, name, created_at FROM playlists WHERE user_id = ? ORDER BY created_at DESC";
        List<Playlist> playlists = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    playlists.add(mapPlaylist(rs));
                }
            }
            return playlists;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list playlists by user", e);
        }
    }

    public boolean addSong(int playlistId, int songId, int position) {
        String sql = "INSERT INTO playlist_songs(playlist_id, song_id, position) VALUES(?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, songId);
            statement.setInt(3, position);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add song to playlist", e);
        }
    }

    public boolean removeSong(int playlistId, int songId) {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, songId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove song from playlist", e);
        }
    }

    public List<Song> getSongs(int playlistId) {
        String sql = "SELECT s.song_id, s.title, s.artist_id, s.duration_seconds, s.file_path " +
                "FROM songs s JOIN playlist_songs ps ON s.song_id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.position ASC";
        List<Song> songs = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(
                            rs.getInt("song_id"),
                            rs.getString("title"),
                            rs.getInt("artist_id"),
                            (Integer) rs.getObject("duration_seconds"),
                            rs.getString("file_path")
                    ));
                }
            }
            return songs;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get playlist songs", e);
        }
    }

    public boolean delete(int playlistId) {
        String sql = "DELETE FROM playlists WHERE playlist_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete playlist", e);
        }
    }

    public boolean rename(int playlistId, String name) {
        String sql = "UPDATE playlists SET name = ? WHERE playlist_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, playlistId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to rename playlist", e);
        }
    }

    private static Playlist mapPlaylist(ResultSet rs) throws SQLException {
        return new Playlist(
                rs.getInt("playlist_id"),
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("created_at")
        );
    }
}
