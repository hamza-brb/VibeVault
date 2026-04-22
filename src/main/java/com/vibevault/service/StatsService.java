package com.vibevault.service;

import com.vibevault.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StatsService {
    private static final int DEFAULT_SESSION_GAP_MINUTES = 30;
    private final DatabaseManager databaseManager;

    public StatsService(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public List<SongPlayStat> getTopSongs(int userId, int limit) {
        String sql = "SELECT s.song_id, s.title, COUNT(ph.play_id) AS play_count, COALESCE(SUM(ph.duration_listened), 0) AS total_seconds " +
                "FROM play_history ph JOIN songs s ON s.song_id = ph.song_id " +
                "WHERE ph.user_id = ? GROUP BY s.song_id, s.title " +
                "ORDER BY play_count DESC, total_seconds DESC, s.title ASC LIMIT ?";
        return querySongStats(userId, limit, sql);
    }

    public List<SongPlayStat> getTopSongsAllUsers(int limit) {
        validateLimit(limit);
        String sql = "SELECT s.song_id, s.title, COUNT(ph.play_id) AS play_count, COALESCE(SUM(ph.duration_listened), 0) AS total_seconds " +
                "FROM play_history ph JOIN songs s ON s.song_id = ph.song_id " +
                "GROUP BY s.song_id, s.title " +
                "ORDER BY play_count DESC, total_seconds DESC, s.title ASC LIMIT ?";
        List<SongPlayStat> stats = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    stats.add(new SongPlayStat(
                            rs.getInt("song_id"),
                            rs.getString("title"),
                            rs.getInt("play_count"),
                            rs.getInt("total_seconds")
                    ));
                }
            }
            return stats;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query global top songs", e);
        }
    }

    public List<ArtistPlayStat> getTopArtists(int userId, int limit) {
        validateLimit(limit);
        String sql = "SELECT a.artist_id, a.name, COUNT(ph.play_id) AS play_count, COALESCE(SUM(ph.duration_listened), 0) AS total_seconds " +
                "FROM play_history ph " +
                "JOIN songs s ON s.song_id = ph.song_id " +
                "JOIN artists a ON a.artist_id = s.artist_id " +
                "WHERE ph.user_id = ? GROUP BY a.artist_id, a.name " +
                "ORDER BY play_count DESC, total_seconds DESC, a.name ASC LIMIT ?";

        List<ArtistPlayStat> stats = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    stats.add(new ArtistPlayStat(
                            rs.getInt("artist_id"),
                            rs.getString("name"),
                            rs.getInt("play_count"),
                            rs.getInt("total_seconds")
                    ));
                }
            }
            return stats;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query top artists", e);
        }
    }

    public List<ArtistPlayStat> getTopArtistsAllUsers(int limit) {
        validateLimit(limit);
        String sql = "SELECT a.artist_id, a.name, COUNT(ph.play_id) AS play_count, COALESCE(SUM(ph.duration_listened), 0) AS total_seconds " +
                "FROM play_history ph " +
                "JOIN songs s ON s.song_id = ph.song_id " +
                "JOIN artists a ON a.artist_id = s.artist_id " +
                "GROUP BY a.artist_id, a.name " +
                "ORDER BY play_count DESC, total_seconds DESC, a.name ASC LIMIT ?";

        List<ArtistPlayStat> stats = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    stats.add(new ArtistPlayStat(
                            rs.getInt("artist_id"),
                            rs.getString("name"),
                            rs.getInt("play_count"),
                            rs.getInt("total_seconds")
                    ));
                }
            }
            return stats;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query global top artists", e);
        }
    }

    public List<RecentPlay> getRecentlyPlayed(int userId, int limit) {
        validateLimit(limit);
        String sql = "SELECT ph.play_id, s.song_id, s.title, ph.played_at, ph.duration_listened " +
                "FROM play_history ph JOIN songs s ON s.song_id = ph.song_id " +
                "WHERE ph.user_id = ? ORDER BY ph.played_at DESC, ph.play_id DESC LIMIT ?";

        List<RecentPlay> plays = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    plays.add(new RecentPlay(
                            rs.getInt("play_id"),
                            rs.getInt("song_id"),
                            rs.getString("title"),
                            rs.getString("played_at"),
                            rs.getInt("duration_listened")
                    ));
                }
            }
            return plays;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query recently played songs", e);
        }
    }

    public Optional<LongestSessionStat> getLongestListeningSession(int userId) {
        return getLongestListeningSession(userId, DEFAULT_SESSION_GAP_MINUTES);
    }

    public Optional<LongestSessionStat> getLongestListeningSession(int userId, int sessionGapMinutes) {
        if (sessionGapMinutes <= 0) {
            throw new IllegalArgumentException("sessionGapMinutes must be greater than zero");
        }
        String sql = "WITH ordered AS (" +
                "  SELECT ph.play_id, ph.played_at, ph.duration_listened, " +
                "         LAG(ph.played_at) OVER (ORDER BY ph.played_at, ph.play_id) AS prev_played_at " +
                "  FROM play_history ph WHERE ph.user_id = ?" +
                "), marked AS (" +
                "  SELECT play_id, played_at, duration_listened, " +
                "         CASE " +
                "           WHEN prev_played_at IS NULL THEN 1 " +
                "           WHEN (julianday(played_at) - julianday(prev_played_at)) * 24 * 60 > ? THEN 1 " +
                "           ELSE 0 " +
                "         END AS starts_new_session " +
                "  FROM ordered" +
                "), sessionized AS (" +
                "  SELECT play_id, played_at, duration_listened, " +
                "         SUM(starts_new_session) OVER (ORDER BY played_at, play_id ROWS UNBOUNDED PRECEDING) AS session_id " +
                "  FROM marked" +
                "), aggregated AS (" +
                "  SELECT session_id, MIN(played_at) AS session_start, MAX(played_at) AS session_end, " +
                "         COUNT(*) AS play_count, COALESCE(SUM(duration_listened), 0) AS total_duration_seconds " +
                "  FROM sessionized GROUP BY session_id" +
                ") " +
                "SELECT session_start, session_end, play_count, total_duration_seconds " +
                "FROM aggregated ORDER BY total_duration_seconds DESC, session_end DESC LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, sessionGapMinutes);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new LongestSessionStat(
                            rs.getString("session_start"),
                            rs.getString("session_end"),
                            rs.getInt("play_count"),
                            rs.getInt("total_duration_seconds")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query longest listening session", e);
        }
    }

    public List<DailyListeningStat> getWeeklyActivity(int userId) {
        String sql = "SELECT DATE(played_at) AS day, COALESCE(SUM(duration_listened), 0) AS total_seconds " +
                "FROM play_history " +
                "WHERE user_id = ? AND DATE(played_at) >= DATE('now', '-6 day') " +
                "GROUP BY DATE(played_at) ORDER BY day ASC";

        List<DailyListeningStat> stats = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int totalSeconds = rs.getInt("total_seconds");
                    stats.add(new DailyListeningStat(
                            rs.getString("day"),
                            totalSeconds,
                            Math.round((totalSeconds / 60.0) * 10.0) / 10.0
                    ));
                }
            }
            return stats;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query weekly activity", e);
        }
    }

    public double getTotalListeningMinutes(int userId) {
        String sql = "SELECT ROUND(COALESCE(SUM(duration_listened), 0) / 60.0, 1) AS total_minutes FROM play_history WHERE user_id = ?";
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
            throw new IllegalStateException("Failed to query total listening minutes", e);
        }
    }

    public int getTotalPlayCount(int userId) {
        String sql = "SELECT COUNT(*) AS total_plays FROM play_history WHERE user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_plays");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query total play count", e);
        }
    }

    public double getTotalListeningMinutesAllUsers() {
        String sql = "SELECT ROUND(COALESCE(SUM(duration_listened), 0) / 60.0, 1) AS total_minutes FROM play_history";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total_minutes");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query global total listening minutes", e);
        }
    }

    private List<SongPlayStat> querySongStats(int userId, int limit, String sql) {
        validateLimit(limit);
        List<SongPlayStat> stats = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    stats.add(new SongPlayStat(
                            rs.getInt("song_id"),
                            rs.getString("title"),
                            rs.getInt("play_count"),
                            rs.getInt("total_seconds")
                    ));
                }
            }
            return stats;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query top songs", e);
        }
    }

    private static void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
    }

    public record SongPlayStat(int songId, String songTitle, int playCount, int totalSeconds) {
    }

    public record ArtistPlayStat(int artistId, String artistName, int playCount, int totalSeconds) {
    }

    public record RecentPlay(int playId, int songId, String songTitle, String playedAt, int durationListened) {
    }

    public record LongestSessionStat(String sessionStart, String sessionEnd, int playCount, int totalDurationSeconds) {
    }

    public record DailyListeningStat(String day, int totalSeconds, double totalMinutes) {
    }
}
