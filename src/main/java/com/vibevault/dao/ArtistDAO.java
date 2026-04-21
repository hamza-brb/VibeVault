package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Artist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArtistDAO {
    private final DatabaseManager databaseManager;

    public ArtistDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Artist create(Artist artist) {
        String sql = "INSERT INTO artists(name, bio) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, artist.getName());
            statement.setString(2, artist.getBio());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    artist.setArtistId(keys.getInt(1));
                }
            }
            return artist;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create artist", e);
        }
    }

    public Optional<Artist> findById(int artistId) {
        String sql = "SELECT artist_id, name, bio FROM artists WHERE artist_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find artist by id", e);
        }
    }

    public Optional<Artist> findByName(String name) {
        String sql = "SELECT artist_id, name, bio FROM artists WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find artist by name", e);
        }
    }

    public List<Artist> findAll() {
        String sql = "SELECT artist_id, name, bio FROM artists ORDER BY name";
        List<Artist> artists = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                artists.add(mapRow(rs));
            }
            return artists;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list artists", e);
        }
    }

    public boolean delete(int artistId) {
        String sql = "DELETE FROM artists WHERE artist_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, artistId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete artist", e);
        }
    }

    private static Artist mapRow(ResultSet rs) throws SQLException {
        return new Artist(
                rs.getInt("artist_id"),
                rs.getString("name"),
                rs.getString("bio")
        );
    }
}
