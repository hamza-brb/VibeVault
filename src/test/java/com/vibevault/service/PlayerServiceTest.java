package com.vibevault.service;

import com.vibevault.dao.ArtistDAO;
import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.dao.SongDAO;
import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Artist;
import com.vibevault.model.PlayHistory;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerServiceTest {
    private DatabaseManager databaseManager;
    private User user;
    private List<Song> songs;
    private List<Path> tempFiles;
    private PlayHistoryDAO playHistoryDAO;

    @BeforeEach
    void setUp() throws IOException {
        String dbName = "vibevault_player_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        databaseManager = new DatabaseManager(dbUrl);
        databaseManager.initializeSchema();

        UserDAO userDAO = new UserDAO(databaseManager);
        ArtistDAO artistDAO = new ArtistDAO(databaseManager);
        SongDAO songDAO = new SongDAO(databaseManager);
        playHistoryDAO = new PlayHistoryDAO(databaseManager);

        user = userDAO.create(new User(null, "player-user", "hash", null));
        Artist artist = artistDAO.create(new Artist(null, "Player Artist", null));
        tempFiles = new ArrayList<>();
        Path songAPath = Files.createTempFile("song-a", ".mp3");
        Path songBPath = Files.createTempFile("song-b", ".mp3");
        Path songCPath = Files.createTempFile("song-c", ".mp3");
        tempFiles.add(songAPath);
        tempFiles.add(songBPath);
        tempFiles.add(songCPath);

        Song songA = songDAO.create(new Song(null, "Song A", artist.getArtistId(), 180, songAPath.toString()));
        Song songB = songDAO.create(new Song(null, "Song B", artist.getArtistId(), 180, songBPath.toString()));
        Song songC = songDAO.create(new Song(null, "Song C", artist.getArtistId(), 180, songCPath.toString()));
        songs = List.of(songA, songB, songC);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
        for (Path path : tempFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {}
        }
    }

    @Test
    void shouldNavigateQueueWithRepeatOff() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);

        assertEquals("Song A", playerService.getCurrentSong().orElseThrow().getTitle());
        assertEquals("Song B", playerService.next().orElseThrow().getTitle());
        assertEquals("Song C", playerService.next().orElseThrow().getTitle());
        assertEquals("Song A", playerService.next().orElseThrow().getTitle());
        assertEquals("Song C", playerService.previous().orElseThrow().getTitle());
    }

    @Test
    void shouldApplyRepeatModes() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);
        playerService.playAt(1);

        playerService.setRepeatMode(PlayerService.RepeatMode.ONE);
        assertEquals("Song B", playerService.next().orElseThrow().getTitle());
        assertEquals("Song B", playerService.previous().orElseThrow().getTitle());

        playerService.setRepeatMode(PlayerService.RepeatMode.ALL);
        playerService.playAt(2);
        assertEquals("Song A", playerService.next().orElseThrow().getTitle());
        assertEquals("Song C", playerService.previous().orElseThrow().getTitle());
    }

    @Test
    void shouldRestartCurrentSongWhenRepeatOneTrackCompletes() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);
        playerService.playAt(1);
        playerService.setRepeatMode(PlayerService.RepeatMode.ONE);

        assertEquals("Song B", playerService.handleTrackCompletion().orElseThrow().getTitle());
        assertTrue(playerService.isPlaying());
        assertEquals(0, playerService.getCurrentSecond());
    }

    @Test
    void shouldStopAtEndOfQueueWhenTrackCompletesWithRepeatOff() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);
        playerService.playAt(2);

        assertTrue(playerService.handleTrackCompletion().isEmpty());
        assertFalse(playerService.isPlaying());
        assertEquals("Song C", playerService.getCurrentSong().orElseThrow().getTitle());
    }

    @Test
    void shouldPreserveCurrentSongWhenShuffleToggles() {
        PlayerService playerService = new PlayerService(databaseManager, new Random(7));
        playerService.setQueue(songs);
        playerService.playAt(1);

        String before = playerService.getCurrentSong().orElseThrow().getTitle();
        playerService.setShuffleEnabled(true);
        assertTrue(playerService.isShuffleEnabled());
        assertEquals(before, playerService.getCurrentSong().orElseThrow().getTitle());

        playerService.setShuffleEnabled(false);
        assertFalse(playerService.isShuffleEnabled());
        assertEquals(before, playerService.getCurrentSong().orElseThrow().getTitle());
    }

    @Test
    void shouldLogOnlyPlaybackLongerThanFiveSeconds() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);

        Optional<PlayHistory> ignored = playerService.logCurrentSongPlayback(user.getUserId(), 4);
        assertTrue(ignored.isEmpty());
        assertEquals(0, playHistoryDAO.findRecentByUser(user.getUserId(), 10).size());

        assertTrue(playerService.logCurrentSongPlayback(user.getUserId(), 5).isPresent());
        assertEquals(1, playHistoryDAO.findRecentByUser(user.getUserId(), 10).size());
    }

    @Test
    void shouldIncrementOnSongStartButNotOnPauseResume() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setActiveUserId(user.getUserId());
        playerService.setQueue(songs);

        playerService.play();
        assertEquals(1, playHistoryDAO.findRecentByUser(user.getUserId(), 10).size());

        playerService.seekToSecond(6);
        playerService.pause();
        assertEquals(1, playHistoryDAO.findRecentByUser(user.getUserId(), 10).size());

        playerService.resume();
        assertEquals(1, playHistoryDAO.findRecentByUser(user.getUserId(), 10).size());

        playerService.next();
        assertEquals(2, playHistoryDAO.findRecentByUser(user.getUserId(), 10).size());
    }

    @Test
    void shouldCountActualListenedTimeWhenSeekingToEnd() throws InterruptedException {
        PlayerService playerService = new PlayerService(databaseManager);
        Song unplayableSong = new Song(
                songs.get(0).getSongId(),
                songs.get(0).getTitle(),
                songs.get(0).getArtistId(),
                songs.get(0).getDurationSeconds(),
                "missing-file-for-test.mp3"
        );

        playerService.setActiveUserId(user.getUserId());
        playerService.setQueue(List.of(unplayableSong));
        playerService.play();

        Thread.sleep(1200);
        playerService.seekToSecond(179);
        playerService.pause();

        List<PlayHistory> history = playHistoryDAO.findRecentByUser(user.getUserId(), 10);
        assertEquals(1, history.size());
        assertTrue(history.get(0).getDurationListened() < 10,
                "Seeking near the end should not count full-song duration as listened time");
    }

    @Test
    void shouldMoveAndRemoveQueueItemsWhenShuffleIsOff() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);
        playerService.playAt(1);

        assertTrue(playerService.moveQueueItem(2, 0));
        List<Song> movedQueue = playerService.getQueueSnapshot();
        assertEquals("Song C", movedQueue.get(0).getTitle());
        assertEquals("Song A", movedQueue.get(1).getTitle());
        assertEquals("Song B", movedQueue.get(2).getTitle());
        assertEquals("Song B", playerService.getCurrentSong().orElseThrow().getTitle());

        assertTrue(playerService.removeQueueItem(2));
        List<Song> removedQueue = playerService.getQueueSnapshot();
        assertEquals(2, removedQueue.size());
        assertEquals("Song C", removedQueue.get(0).getTitle());
        assertEquals("Song A", removedQueue.get(1).getTitle());
        assertEquals("Song A", playerService.getCurrentSong().orElseThrow().getTitle());
    }

    @Test
    void shouldAllowManualQueueEditsWhenShuffleIsOn() {
        PlayerService playerService = new PlayerService(databaseManager, new Random(9));
        playerService.setQueue(songs);
        playerService.setShuffleEnabled(true);

        assertTrue(playerService.moveQueueItem(0, 1));
        assertTrue(playerService.removeQueueItem(0));
    }

    @Test
    void shouldHandlePlayPauseResumeSeekAndVolumeControls() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);

        assertFalse(playerService.isPlaying());
        assertTrue(playerService.play().isPresent());
        assertTrue(playerService.isPlaying());

        playerService.seekToSecond(42);
        assertEquals(42, playerService.getCurrentSecond());

        playerService.pause();
        assertFalse(playerService.isPlaying());
        assertTrue(playerService.resume().isPresent());
        assertTrue(playerService.isPlaying());

        playerService.setVolumePercent(55);
        assertEquals(55, playerService.getVolumePercent());
    }

    @Test
    void shouldFirePlayingEventsForManualPlayAndPause() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);

        List<Boolean> playingEvents = new CopyOnWriteArrayList<>();
        playerService.addPropertyChangeListener(evt -> {
            if ("playing".equals(evt.getPropertyName())) {
                playingEvents.add((Boolean) evt.getNewValue());
            }
        });

        playerService.play();
        playerService.pause();

        assertTrue(playingEvents.contains(true));
        assertTrue(playingEvents.contains(false));
    }

    @Test
    void shouldValidateSeekAndVolumeBounds() {
        PlayerService playerService = new PlayerService(databaseManager);
        playerService.setQueue(songs);

        assertThrows(IllegalArgumentException.class, () -> playerService.seekToSecond(-1));
        assertThrows(IllegalArgumentException.class, () -> playerService.seekToSecond(181));
        assertThrows(IllegalArgumentException.class, () -> playerService.setVolumePercent(-1));
        assertThrows(IllegalArgumentException.class, () -> playerService.setVolumePercent(101));

        playerService.clearQueue();
        assertThrows(IllegalStateException.class, () -> playerService.seekToSecond(10));
        assertTrue(playerService.resume().isEmpty());
        assertTrue(playerService.play().isEmpty());
    }
}
