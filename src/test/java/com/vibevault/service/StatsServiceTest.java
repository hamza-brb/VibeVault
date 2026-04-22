package com.vibevault.service;

import com.vibevault.dao.ArtistDAO;
import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.dao.SongDAO;
import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Artist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsServiceTest {
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private ArtistDAO artistDAO;
    private SongDAO songDAO;
    private PlayHistoryDAO playHistoryDAO;
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        String dbName = "vibevault_stats_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        databaseManager = new DatabaseManager(dbUrl);
        databaseManager.initializeSchema();

        userDAO = new UserDAO(databaseManager);
        artistDAO = new ArtistDAO(databaseManager);
        songDAO = new SongDAO(databaseManager);
        playHistoryDAO = new PlayHistoryDAO(databaseManager);
        statsService = new StatsService(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldReturnTopSongsAndTopArtistsForOnlyCurrentUser() {
        User user1 = userDAO.create(new User(null, "hamza", "hash-1", null));
        User user2 = userDAO.create(new User(null, "shehryar", "hash-2", null));

        Artist artist1 = artistDAO.create(new Artist(null, "Adele", null));
        Artist artist2 = artistDAO.create(new Artist(null, "Imagine Dragons", null));
        Song song1 = songDAO.create(new Song(null, "Easy On Me", artist1.getArtistId(), 224, "C:\\music\\easy-on-me.mp3"));
        Song song2 = songDAO.create(new Song(null, "Believer", artist2.getArtistId(), 204, "C:\\music\\believer.mp3"));
        Song song3 = songDAO.create(new Song(null, "Enemy", artist2.getArtistId(), 173, "C:\\music\\enemy.mp3"));

        playHistoryDAO.logPlay(user1.getUserId(), song1.getSongId(), 120);
        playHistoryDAO.logPlay(user1.getUserId(), song1.getSongId(), 90);
        playHistoryDAO.logPlay(user1.getUserId(), song2.getSongId(), 60);

        playHistoryDAO.logPlay(user2.getUserId(), song3.getSongId(), 300);

        List<StatsService.SongPlayStat> topSongs = statsService.getTopSongs(user1.getUserId(), 5);
        assertEquals(2, topSongs.size());
        assertEquals("Easy On Me", topSongs.get(0).songTitle());
        assertEquals(2, topSongs.get(0).playCount());
        assertEquals(210, topSongs.get(0).totalSeconds());

        List<StatsService.ArtistPlayStat> topArtists = statsService.getTopArtists(user1.getUserId(), 5);
        assertEquals(2, topArtists.size());
        assertEquals("Adele", topArtists.get(0).artistName());
        assertEquals(2, topArtists.get(0).playCount());
        assertEquals(210, topArtists.get(0).totalSeconds());

        assertEquals(4.5, statsService.getTotalListeningMinutes(user1.getUserId()));
        assertEquals(3, statsService.getTotalPlayCount(user1.getUserId()));
    }

    @Test
    void shouldReturnRecentlyPlayedInNewestFirstOrder() {
        User user = userDAO.create(new User(null, "listener", "hash-listener", null));
        Artist artist = artistDAO.create(new Artist(null, "OneRepublic", null));
        Song song = songDAO.create(new Song(null, "Counting Stars", artist.getArtistId(), 257, "C:\\music\\counting-stars.mp3"));

        playHistoryDAO.logPlay(user.getUserId(), song.getSongId(), 50);
        playHistoryDAO.logPlay(user.getUserId(), song.getSongId(), 80);

        List<StatsService.RecentPlay> recent = statsService.getRecentlyPlayed(user.getUserId(), 2);
        assertEquals(2, recent.size());
        assertEquals(80, recent.get(0).durationListened());
        assertEquals(50, recent.get(1).durationListened());
    }

    @Test
    void shouldReturnLongestSessionAndWeeklyActivity() {
        User user = userDAO.create(new User(null, "report-user", "hash", null));
        Artist artist = artistDAO.create(new Artist(null, "The Weeknd", null));
        Song songA = songDAO.create(new Song(null, "Blinding Lights", artist.getArtistId(), 200, "library://blinding-lights"));
        Song songB = songDAO.create(new Song(null, "Save Your Tears", artist.getArtistId(), 215, "library://save-your-tears"));
        Song songC = songDAO.create(new Song(null, "Take My Breath", artist.getArtistId(), 220, "library://take-my-breath"));

        int play1 = playHistoryDAO.logPlay(user.getUserId(), songA.getSongId(), 120).getPlayId();
        int play2 = playHistoryDAO.logPlay(user.getUserId(), songB.getSongId(), 240).getPlayId();
        int play3 = playHistoryDAO.logPlay(user.getUserId(), songC.getSongId(), 90).getPlayId();

        String oneDayAgo = LocalDate.now().minusDays(1).toString();
        String sixDaysAgo = LocalDate.now().minusDays(6).toString();

        updatePlayedAt(play1, oneDayAgo + " 10:00:00");
        updatePlayedAt(play2, oneDayAgo + " 10:20:00");
        updatePlayedAt(play3, sixDaysAgo + " 12:00:00");

        StatsService.LongestSessionStat longestSession = statsService.getLongestListeningSession(user.getUserId()).orElseThrow();
        assertEquals(2, longestSession.playCount());
        assertEquals(360, longestSession.totalDurationSeconds());
        assertTrue(longestSession.sessionStart().startsWith(oneDayAgo + " 10:00:00"));
        assertTrue(longestSession.sessionEnd().startsWith(oneDayAgo + " 10:20:00"));

        List<StatsService.DailyListeningStat> weekly = statsService.getWeeklyActivity(user.getUserId());
        assertEquals(7, weekly.size());
        assertEquals(LocalDate.now().minusDays(6).toString(), weekly.get(0).day());
        assertEquals(LocalDate.now().toString(), weekly.get(6).day());

        Map<String, Integer> weeklySecondsByDay = weekly.stream()
                .collect(Collectors.toMap(StatsService.DailyListeningStat::day, StatsService.DailyListeningStat::totalSeconds));
        assertEquals(90, weeklySecondsByDay.get(sixDaysAgo));
        assertEquals(360, weeklySecondsByDay.get(oneDayAgo));
    }

    @Test
    void shouldRejectInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> statsService.getTopSongs(1, 0));
        assertThrows(IllegalArgumentException.class, () -> statsService.getTopSongsAllUsers(0));
        assertThrows(IllegalArgumentException.class, () -> statsService.getTopArtists(1, -1));
        assertThrows(IllegalArgumentException.class, () -> statsService.getTopArtistsAllUsers(-1));
        assertThrows(IllegalArgumentException.class, () -> statsService.getRecentlyPlayed(1, 0));
        assertThrows(IllegalArgumentException.class, () -> statsService.getLongestListeningSession(1, 0));
    }

    @Test
    void shouldReturnEmptyOptionalForSessionWhenNoHistory() {
        User user = userDAO.create(new User(null, "empty-user", "hash", null));
        assertTrue(statsService.getLongestListeningSession(user.getUserId()).isEmpty());
    }

    @Test
    void shouldReturnGlobalAdminStatsAcrossAllUsers() {
        User user1 = userDAO.create(new User(null, "admin-user-1", "hash-1", null));
        User user2 = userDAO.create(new User(null, "admin-user-2", "hash-2", null));

        Artist artist1 = artistDAO.create(new Artist(null, "Admin Artist A", null));
        Artist artist2 = artistDAO.create(new Artist(null, "Admin Artist B", null));
        Song song1 = songDAO.create(new Song(null, "Global Song 1", artist1.getArtistId(), 200, "library://admin/song1"));
        Song song2 = songDAO.create(new Song(null, "Global Song 2", artist2.getArtistId(), 180, "library://admin/song2"));

        playHistoryDAO.logPlay(user1.getUserId(), song1.getSongId(), 60);
        playHistoryDAO.logPlay(user2.getUserId(), song1.getSongId(), 90);
        playHistoryDAO.logPlay(user2.getUserId(), song2.getSongId(), 120);

        List<StatsService.SongPlayStat> topSongs = statsService.getTopSongsAllUsers(5);
        assertEquals(2, topSongs.size());
        assertEquals("Global Song 1", topSongs.get(0).songTitle());
        assertEquals(2, topSongs.get(0).playCount());
        assertEquals(150, topSongs.get(0).totalSeconds());

        List<StatsService.ArtistPlayStat> topArtists = statsService.getTopArtistsAllUsers(5);
        assertEquals(2, topArtists.size());
        assertEquals("Admin Artist A", topArtists.get(0).artistName());
        assertEquals(2, topArtists.get(0).playCount());
        assertEquals(150, topArtists.get(0).totalSeconds());

        assertEquals(4.5, statsService.getTotalListeningMinutesAllUsers());
    }

    private void updatePlayedAt(int playId, String playedAt) {
        String sql = "UPDATE play_history SET played_at = ? WHERE play_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playedAt);
            statement.setInt(2, playId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update played_at for test", e);
        }
    }
}
