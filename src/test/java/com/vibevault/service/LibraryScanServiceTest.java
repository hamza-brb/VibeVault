package com.vibevault.service;

import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryScanServiceTest {
    private DatabaseManager databaseManager;
    private LibraryService libraryService;
    private LibraryScanService libraryScanService;
    private User user;
    private Path watchDir;

    @BeforeEach
    void setUp() throws IOException {
        String dbName = "vibevault_scan_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        databaseManager = new DatabaseManager(dbUrl);
        databaseManager.initializeSchema();

        libraryService = new LibraryService(databaseManager);
        libraryScanService = new LibraryScanService(databaseManager, libraryService);
        user = new UserDAO(databaseManager).create(new User(null, "scan-user", "hash", null));
        watchDir = Files.createTempDirectory("vibevault-scan-watch");
    }

    @AfterEach
    void tearDown() throws IOException {
        databaseManager.close();
        if (watchDir != null) {
            Files.walk(watchDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    void shouldCountOnlyTrulyNewSongsDuringScan() throws IOException {
        Path existingSong = watchDir.resolve("existing.mp3");
        Path newSong = watchDir.resolve("new.mp3");
        Files.writeString(existingSong, "not-real-audio");
        Files.writeString(newSong, "not-real-audio");

        libraryService.importSongFromFile(user.getUserId(), existingSong);
        libraryScanService.addWatchedFolder(user.getUserId(), watchDir.toString());

        LibraryScanService.ScanResult result = libraryScanService.scanWatchedFolders(user.getUserId(), null);

        assertEquals(1, result.newSongsImported());
        assertEquals(1, result.alreadyInLibrary());
        assertEquals(0, result.failed());
        assertEquals(2, libraryService.getUserLibrarySongs(user.getUserId()).size());
    }
}
