package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    private final DatabaseManager databaseManager;

    public UserDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public User create(User user) {
        String sql = "INSERT INTO users(username, password_hash) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create user", e);
        }
    }

    public Optional<User> findById(int userId) {
        String sql = "SELECT user_id, username, password_hash, created_at FROM users WHERE user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by id", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, created_at FROM users WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by username", e);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT user_id, username, password_hash, created_at FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list users", e);
        }
    }

    public boolean updatePasswordHash(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update user password hash", e);
        }
    }

    public boolean delete(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user", e);
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("created_at")
        );
    }
}
