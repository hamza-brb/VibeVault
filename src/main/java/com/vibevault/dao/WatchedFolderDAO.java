package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WatchedFolderDAO {
    private final DatabaseManager databaseManager;

    public WatchedFolderDAO(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    /** Returns all watched folder paths for a given user. */
    public List<String> findByUser(int userId) {
        String sql = "SELECT folder_path FROM watched_folders WHERE user_id = ? ORDER BY added_at ASC";
        List<String> folders = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    folders.add(rs.getString("folder_path"));
                }
            }
            return folders;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query watched folders", e);
        }
    }

    /** Adds a folder path for a user. Ignores duplicates. */
    public boolean add(int userId, String folderPath) {
        String sql = "INSERT OR IGNORE INTO watched_folders(user_id, folder_path) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, folderPath);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add watched folder", e);
        }
    }

    /** Removes a folder path for a user. */
    public boolean remove(int userId, String folderPath) {
        String sql = "DELETE FROM watched_folders WHERE user_id = ? AND folder_path = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, folderPath);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove watched folder", e);
        }
    }
}
