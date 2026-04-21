package com.vibevault.service;

import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServiceTest {
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private LibraryService libraryService;
    private PlayHistoryDAO playHistoryDAO;

    @BeforeEach
    void setUp() {
        String dbName = "vibevault_library_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        databaseManager = new DatabaseManager(dbUrl);
        databaseManager.initializeSchema();

        userDAO = new UserDAO(databaseManager);
        libraryService = new LibraryService(databaseManager);
        playHistoryDAO = new PlayHistoryDAO(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldImportSongCreateArtistAlbumAndAddToUserLibrary() {
        User user = userDAO.create(new User(null, "library-user", "hash", null));

        Song song = libraryService.importSong(user.getUserId(), new LibraryService.SongImportRequest(
                "Numb", "Linkin Park", "Meteora", "Rock", 185, "library://tracks/numb", 3, 2003
        ));

        assertNotNull(song.getSongId());

        List<Song> songs = libraryService.getUserLibrarySongs(user.getUserId());
        assertEquals(1, songs.size());
        assertEquals("Numb", songs.get(0).getTitle());
        assertEquals("library://tracks/numb", songs.get(0).getFilePath());

        List<Album> albums = libraryService.getAlbumsByArtist(songs.get(0).getArtistId());
        assertEquals(1, albums.size());
        assertEquals("Meteora", albums.get(0).getTitle());
    }

    @Test
    void shouldReuseExistingSongAndNotDuplicateLibraryEntry() {
        User user = userDAO.create(new User(null, "reuse-user", "hash", null));
        LibraryService.SongImportRequest request = new LibraryService.SongImportRequest(
                "Viva La Vida", "Coldplay", "Viva La Vida", "Pop", 242, "https://example.com/audio/viva-la-vida.mp3", 1, 2008
        );

        Song first = libraryService.importSong(user.getUserId(), request);
        Song second = libraryService.importSong(user.getUserId(), request);

        assertEquals(first.getSongId(), second.getSongId());
        assertEquals(1, libraryService.getUserLibrarySongs(user.getUserId()).size());
    }

    @Test
    void shouldExposeArtistsByPlayCounts() {
        User user = userDAO.create(new User(null, "stats-user", "hash", null));
        Song songA = libraryService.importSong(user.getUserId(), new LibraryService.SongImportRequest(
                "Song A", "Artist A", "Album A", "Pop", 180, "library://song-a", 1, 2024
        ));
        Song songB = libraryService.importSong(user.getUserId(), new LibraryService.SongImportRequest(
                "Song B", "Artist B", "Album B", "Rock", 200, "library://song-b", 1, 2024
        ));

        playHistoryDAO.logPlay(user.getUserId(), songA.getSongId(), 120);
        playHistoryDAO.logPlay(user.getUserId(), songA.getSongId(), 60);
        playHistoryDAO.logPlay(user.getUserId(), songB.getSongId(), 30);

        List<StatsService.ArtistPlayStat> artists = libraryService.getLibraryArtistsByPlays(user.getUserId(), 5);
        assertEquals(2, artists.size());
        assertEquals("Artist A", artists.get(0).artistName());
        assertEquals(2, artists.get(0).playCount());
    }

    @Test
    void shouldRejectInvalidImportInput() {
        User user = userDAO.create(new User(null, "invalid-user", "hash", null));

        assertThrows(IllegalArgumentException.class, () -> libraryService.importSong(user.getUserId(), null));
        assertThrows(IllegalArgumentException.class, () -> libraryService.importSong(user.getUserId(),
                new LibraryService.SongImportRequest("", "Artist", "Album", "Pop", 100, "library://x", 1, 2024)));
        assertThrows(IllegalArgumentException.class, () -> libraryService.importSong(user.getUserId(),
                new LibraryService.SongImportRequest("Song", "", "Album", "Pop", 100, "library://x", 1, 2024)));
        assertThrows(IllegalArgumentException.class, () -> libraryService.importSong(user.getUserId(),
                new LibraryService.SongImportRequest("Song", "Artist", "Album", "Pop", 100, " ", 1, 2024)));
    }

    @Test
    void shouldProvideArtistAlbumAndSongBrowseViewsForUserLibrary() {
        User user = userDAO.create(new User(null, "browse-user", "hash", null));
        User otherUser = userDAO.create(new User(null, "other-browse-user", "hash", null));

        Song numb = libraryService.importSong(user.getUserId(), new LibraryService.SongImportRequest(
                "Numb", "Linkin Park", "Meteora", "Rock", 185, "library://browse/numb", 2, 2003
        ));
        Song faint = libraryService.importSong(user.getUserId(), new LibraryService.SongImportRequest(
                "Faint", "Linkin Park", "Meteora", "Rock", 162, "library://browse/faint", 7, 2003
        ));
        libraryService.importSong(user.getUserId(), new LibraryService.SongImportRequest(
                "Fix You", "Coldplay", "X&Y", "Pop", 295, "library://browse/fix-you", 4, 2005
        ));

        libraryService.importSong(otherUser.getUserId(), new LibraryService.SongImportRequest(
                "Paradise", "Coldplay", "Mylo Xyloto", "Pop", 278, "library://browse/paradise", 3, 2011
        ));

        List<LibraryService.ArtistLibrarySummary> artistSummaries = libraryService.getArtistBrowseSummaries(user.getUserId());
        assertEquals(2, artistSummaries.size());
        assertEquals("Coldplay", artistSummaries.get(0).artistName());
        assertEquals("Linkin Park", artistSummaries.get(1).artistName());
        assertEquals(2, artistSummaries.get(1).songCount());
        assertEquals(347, artistSummaries.get(1).totalDurationSeconds());

        int linkinParkArtistId = artistSummaries.get(1).artistId();
        List<LibraryService.AlbumLibrarySummary> albumSummaries = libraryService.getAlbumBrowseSummaries(user.getUserId(), linkinParkArtistId);
        assertEquals(1, albumSummaries.size());
        assertEquals("Meteora", albumSummaries.get(0).albumTitle());
        assertEquals(2, albumSummaries.get(0).songCount());

        List<Song> artistSongs = libraryService.getSongsByArtistInUserLibrary(user.getUserId(), linkinParkArtistId);
        assertEquals(2, artistSongs.size());
        assertEquals("Numb", artistSongs.get(0).getTitle());
        assertEquals("Faint", artistSongs.get(1).getTitle());

        int meteoraAlbumId = albumSummaries.get(0).albumId();
        List<Song> albumSongs = libraryService.getSongsByAlbumInUserLibrary(user.getUserId(), meteoraAlbumId);
        assertEquals(2, albumSongs.size());
        assertEquals("Numb", albumSongs.get(0).getTitle());
        assertEquals("Faint", albumSongs.get(1).getTitle());

        assertTrue(libraryService.getSongsByArtistInUserLibrary(otherUser.getUserId(), linkinParkArtistId).isEmpty());
    }

    @Test
    void shouldImportSongFromAudioFileUsingFallbackMetadataWhenTagReadFails() throws IOException {
        User user = userDAO.create(new User(null, "file-import-user", "hash", null));
        Path tempFile = Files.createTempFile("vibevault-test-track", ".mp3");
        Files.writeString(tempFile, "not-real-audio");
        tempFile.toFile().deleteOnExit();

        Song imported = libraryService.importSongFromFile(user.getUserId(), tempFile);

        assertNotNull(imported.getSongId());
        assertEquals("Unknown Artist", libraryService.getArtistBrowseSummaries(user.getUserId()).get(0).artistName());
        assertEquals(tempFile.toAbsolutePath().normalize().toString(), imported.getFilePath());
        assertTrue(imported.getTitle().startsWith("vibevault-test-track"));
    }
}
