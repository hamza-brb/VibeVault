package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.PlayHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlayHistoryDAO {
    private final DatabaseManager databaseManager;

    public PlayHistoryDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public PlayHistory logPlay(int userId, int songId, int durationListened) {
        String sql = "INSERT INTO play_history(user_id, song_id, duration_listened) VALUES(?, ?, ?)";
        PlayHistory playHistory = new PlayHistory();
        playHistory.setUserId(userId);
        playHistory.setSongId(songId);
        playHistory.setDurationListened(durationListened);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setInt(2, songId);
            statement.setInt(3, durationListened);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    playHistory.setPlayId(keys.getInt(1));
                }
            }
            return playHistory;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to log play event", e);
        }
    }

    public List<PlayHistory> findRecentByUser(int userId, int limit) {
        String sql = "SELECT play_id, user_id, song_id, played_at, duration_listened " +
                "FROM play_history WHERE user_id = ? ORDER BY played_at DESC, play_id DESC LIMIT ?";
        List<PlayHistory> history = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    history.add(mapRow(rs));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query play history", e);
        }
    }

    public double getTotalListeningMinutes(int userId) {
        String sql = "SELECT ROUND(COALESCE(SUM(duration_listened), 0) / 60.0, 1) AS total_minutes " +
                "FROM play_history WHERE user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_minutes");
                }
                return 0.0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query total listening time", e);
        }
    }

    private static PlayHistory mapRow(ResultSet rs) throws SQLException {
        return new PlayHistory(
                rs.getInt("play_id"),
                rs.getInt("user_id"),
                rs.getInt("song_id"),
                rs.getString("played_at"),
                rs.getInt("duration_listened")
        );
    }
}
