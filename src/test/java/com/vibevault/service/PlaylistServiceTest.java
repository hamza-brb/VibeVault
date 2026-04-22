package com.vibevault.service;

import com.vibevault.dao.ArtistDAO;
import com.vibevault.dao.SongDAO;
import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Artist;
import com.vibevault.model.Playlist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistServiceTest {
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private ArtistDAO artistDAO;
    private SongDAO songDAO;
    private PlaylistService playlistService;

    @BeforeEach
    void setUp() {
        String dbName = "vibevault_playlist_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        databaseManager = new DatabaseManager(dbUrl);
        databaseManager.initializeSchema();

        userDAO = new UserDAO(databaseManager);
        artistDAO = new ArtistDAO(databaseManager);
        songDAO = new SongDAO(databaseManager);
        playlistService = new PlaylistService(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldAppendSongsAndReturnInPlaylistOrder() {
        SeedData seed = seedSongs();
        Playlist playlist = playlistService.createPlaylist(seed.user.getUserId(), "Road Trip");

        playlistService.addSong(playlist.getPlaylistId(), seed.songA.getSongId());
        playlistService.addSong(playlist.getPlaylistId(), seed.songB.getSongId());
        playlistService.addSong(playlist.getPlaylistId(), seed.songC.getSongId());

        List<Song> songs = playlistService.getPlaylistSongs(playlist.getPlaylistId());
        assertEquals(3, songs.size());
        assertEquals("Song A", songs.get(0).getTitle());
        assertEquals("Song B", songs.get(1).getTitle());
        assertEquals("Song C", songs.get(2).getTitle());
    }

    @Test
    void shouldMoveSongsAndKeepContiguousOrdering() {
        SeedData seed = seedSongs();
        Playlist playlist = playlistService.createPlaylist(seed.user.getUserId(), "Workout");

        playlistService.addSong(playlist.getPlaylistId(), seed.songA.getSongId());
        playlistService.addSong(playlist.getPlaylistId(), seed.songB.getSongId());
        playlistService.addSong(playlist.getPlaylistId(), seed.songC.getSongId());

        assertTrue(playlistService.moveSong(playlist.getPlaylistId(), seed.songC.getSongId(), 1));
        List<Song> afterMoveUp = playlistService.getPlaylistSongs(playlist.getPlaylistId());
        assertEquals("Song C", afterMoveUp.get(0).getTitle());
        assertEquals("Song A", afterMoveUp.get(1).getTitle());
        assertEquals("Song B", afterMoveUp.get(2).getTitle());

        assertTrue(playlistService.moveSong(playlist.getPlaylistId(), seed.songC.getSongId(), 3));
        List<Song> afterMoveDown = playlistService.getPlaylistSongs(playlist.getPlaylistId());
        assertEquals("Song A", afterMoveDown.get(0).getTitle());
        assertEquals("Song B", afterMoveDown.get(1).getTitle());
        assertEquals("Song C", afterMoveDown.get(2).getTitle());
    }

    @Test
    void shouldRemoveSongAndCompactPositions() {
        SeedData seed = seedSongs();
        Playlist playlist = playlistService.createPlaylist(seed.user.getUserId(), "Focus");

        playlistService.addSong(playlist.getPlaylistId(), seed.songA.getSongId());
        playlistService.addSong(playlist.getPlaylistId(), seed.songB.getSongId());
        playlistService.addSong(playlist.getPlaylistId(), seed.songC.getSongId());

        assertTrue(playlistService.removeSong(playlist.getPlaylistId(), seed.songB.getSongId()));

        List<Song> songs = playlistService.getPlaylistSongs(playlist.getPlaylistId());
        assertEquals(2, songs.size());
        assertEquals("Song A", songs.get(0).getTitle());
        assertEquals("Song C", songs.get(1).getTitle());
    }

    @Test
    void shouldValidateInputAndMissingEntries() {
        SeedData seed = seedSongs();
        Playlist playlist = playlistService.createPlaylist(seed.user.getUserId(), "Validation");
        playlistService.addSong(playlist.getPlaylistId(), seed.songA.getSongId());

        assertThrows(IllegalArgumentException.class, () -> playlistService.createPlaylist(seed.user.getUserId(), " "));
        assertThrows(IllegalArgumentException.class, () -> playlistService.moveSong(playlist.getPlaylistId(), seed.songA.getSongId(), 0));
        assertThrows(IllegalArgumentException.class, () -> playlistService.moveSong(playlist.getPlaylistId(), seed.songA.getSongId(), 3));
        assertFalse(playlistService.moveSong(playlist.getPlaylistId(), seed.songB.getSongId(), 1));
        assertFalse(playlistService.removeSong(playlist.getPlaylistId(), seed.songB.getSongId()));
    }

    private SeedData seedSongs() {
        User user = userDAO.create(new User(null, "playlist-user-" + UUID.randomUUID(), "hash", null));
        Artist artist = artistDAO.create(new Artist(null, "Artist " + UUID.randomUUID(), null));
        Song songA = songDAO.create(new Song(null, "Song A", artist.getArtistId(), 180, "C:\\music\\song-a-" + UUID.randomUUID() + ".mp3", 1, 2024));
        Song songB = songDAO.create(new Song(null, "Song B", artist.getArtistId(), 190, "C:\\music\\song-b-" + UUID.randomUUID() + ".mp3", 2, 2024));
        Song songC = songDAO.create(new Song(null, "Song C", artist.getArtistId(), 200, "C:\\music\\song-c-" + UUID.randomUUID() + ".mp3", 3, 2024));

        songDAO.addToUserLibraryIfMissing(user.getUserId(), songA.getSongId());
        songDAO.addToUserLibraryIfMissing(user.getUserId(), songB.getSongId());
        songDAO.addToUserLibraryIfMissing(user.getUserId(), songC.getSongId());

        return new SeedData(user, songA, songB, songC);
    }

    private record SeedData(User user, Song songA, Song songB, Song songC) {
    }
}
