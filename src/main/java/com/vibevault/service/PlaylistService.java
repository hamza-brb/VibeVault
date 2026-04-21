package com.vibevault.service;

import com.vibevault.dao.PlaylistDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Playlist;
import com.vibevault.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class PlaylistService {
    private final DatabaseManager databaseManager;
    private final PlaylistDAO playlistDAO;

    public PlaylistService(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.playlistDAO = new PlaylistDAO(this.databaseManager);
    }

    public Playlist createPlaylist(int userId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name must not be blank");
        }
        return playlistDAO.create(new Playlist(null, userId, name.trim(), null));
    }

    public List<Playlist> getUserPlaylists(int userId) {
        return playlistDAO.findByUserId(userId);
    }

    public List<Song> getPlaylistSongs(int playlistId) {
        return playlistDAO.getSongs(playlistId);
    }

    public boolean renamePlaylist(int playlistId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name must not be blank");
        }
        return playlistDAO.rename(playlistId, name.trim());
    }

    public boolean deletePlaylist(int playlistId) {
        return playlistDAO.delete(playlistId);
    }

    public void addSong(int playlistId, int songId) {
        String nextPositionSql = "SELECT COALESCE(MAX(position), 0) + 1 AS next_position FROM playlist_songs WHERE playlist_id = ?";
        String insertSql = "INSERT INTO playlist_songs(playlist_id, song_id, position) VALUES(?, ?, ?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement nextPositionStatement = connection.prepareStatement(nextPositionSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            nextPositionStatement.setInt(1, playlistId);
            int nextPosition;
            try (ResultSet rs = nextPositionStatement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Failed to compute next playlist position");
                }
                nextPosition = rs.getInt("next_position");
            }

            insertStatement.setInt(1, playlistId);
            insertStatement.setInt(2, songId);
            insertStatement.setInt(3, nextPosition);
            insertStatement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add song to playlist", e);
        }
    }

    public boolean removeSong(int playlistId, int songId) {
        String positionSql = "SELECT position FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        String deleteSql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        String compactSql = "UPDATE playlist_songs SET position = position - 1 WHERE playlist_id = ? AND position > ?";

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement positionStatement = connection.prepareStatement(positionSql);
                 PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement compactStatement = connection.prepareStatement(compactSql)) {
                positionStatement.setInt(1, playlistId);
                positionStatement.setInt(2, songId);

                Integer currentPosition;
                try (ResultSet rs = positionStatement.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return false;
                    }
                    currentPosition = rs.getInt("position");
                }

                deleteStatement.setInt(1, playlistId);
                deleteStatement.setInt(2, songId);
                deleteStatement.executeUpdate();

                compactStatement.setInt(1, playlistId);
                compactStatement.setInt(2, currentPosition);
                compactStatement.executeUpdate();

                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove song from playlist", e);
        }
    }

    public boolean moveSong(int playlistId, int songId, int newPosition) {
        if (newPosition <= 0) {
            throw new IllegalArgumentException("newPosition must be greater than zero");
        }

        String currentPositionSql = "SELECT position FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        String maxPositionSql = "SELECT COALESCE(MAX(position), 0) AS max_position FROM playlist_songs WHERE playlist_id = ?";
        String shiftUpSql = "UPDATE playlist_songs SET position = position + 1 WHERE playlist_id = ? AND position >= ? AND position < ?";
        String shiftDownSql = "UPDATE playlist_songs SET position = position - 1 WHERE playlist_id = ? AND position > ? AND position <= ?";
        String setPositionSql = "UPDATE playlist_songs SET position = ? WHERE playlist_id = ? AND song_id = ?";

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement currentPositionStatement = connection.prepareStatement(currentPositionSql);
                 PreparedStatement maxPositionStatement = connection.prepareStatement(maxPositionSql);
                 PreparedStatement shiftUpStatement = connection.prepareStatement(shiftUpSql);
                 PreparedStatement shiftDownStatement = connection.prepareStatement(shiftDownSql);
                 PreparedStatement setPositionStatement = connection.prepareStatement(setPositionSql)) {
                currentPositionStatement.setInt(1, playlistId);
                currentPositionStatement.setInt(2, songId);

                Integer currentPosition;
                try (ResultSet rs = currentPositionStatement.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return false;
                    }
                    currentPosition = rs.getInt("position");
                }

                maxPositionStatement.setInt(1, playlistId);
                int maxPosition;
                try (ResultSet rs = maxPositionStatement.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return false;
                    }
                    maxPosition = rs.getInt("max_position");
                }

                if (newPosition > maxPosition) {
                    throw new IllegalArgumentException("newPosition exceeds playlist size");
                }
                if (newPosition == currentPosition) {
                    connection.commit();
                    return true;
                }

                if (newPosition < currentPosition) {
                    shiftUpStatement.setInt(1, playlistId);
                    shiftUpStatement.setInt(2, newPosition);
                    shiftUpStatement.setInt(3, currentPosition);
                    shiftUpStatement.executeUpdate();
                } else {
                    shiftDownStatement.setInt(1, playlistId);
                    shiftDownStatement.setInt(2, currentPosition);
                    shiftDownStatement.setInt(3, newPosition);
                    shiftDownStatement.executeUpdate();
                }

                setPositionStatement.setInt(1, newPosition);
                setPositionStatement.setInt(2, playlistId);
                setPositionStatement.setInt(3, songId);
                setPositionStatement.executeUpdate();

                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to move song in playlist", e);
        }
    }
}
