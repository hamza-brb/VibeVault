package com.vibevault.service;

import com.vibevault.dao.SongDAO;
import com.vibevault.dao.WatchedFolderDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class LibraryScanService {
    private static final String SHARED_SONGS_ENV = "VIBEVAULT_SHARED_SONGS_DIR";

    private final LibraryService libraryService;
    private final SongDAO songDAO;
    private final WatchedFolderDAO watchedFolderDAO;
    private final Path sharedSongsDir;

    private static Path resolveSharedSongsDir() {
        String configured = System.getenv(SHARED_SONGS_ENV);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir"), "songs").toAbsolutePath().normalize();
    }

    public LibraryScanService(DatabaseManager databaseManager, LibraryService libraryService) {
        this(databaseManager, libraryService, resolveSharedSongsDir());
    }

    LibraryScanService(DatabaseManager databaseManager, LibraryService libraryService, Path sharedSongsDir) {
        this.libraryService = libraryService;
        this.songDAO = new SongDAO(databaseManager);
        this.watchedFolderDAO = new WatchedFolderDAO(databaseManager);
        this.sharedSongsDir = sharedSongsDir.toAbsolutePath().normalize();
    }

    // ── Watched Folders ───────────────────────────────────────────────────

    public List<String> getWatchedFolders(int userId) {
        return watchedFolderDAO.findByUser(userId);
    }

    public void addWatchedFolder(int userId, String folderPath) {
        Path path = Path.of(folderPath);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Not a valid directory: " + folderPath);
        }
        watchedFolderDAO.add(userId, path.toAbsolutePath().normalize().toString());
    }

    public void removeWatchedFolder(int userId, String folderPath) {
        watchedFolderDAO.remove(userId, folderPath);
    }

    // ── Dead File Cleanup ─────────────────────────────────────────────────

    /**
     * Checks all songs in a user's library.
     * Any whose file_path no longer exists on disk is removed from user_library.
     *
     * @return number of songs removed
     */
    public int validateAndCleanLibrary(int userId) {
        List<Song> songs = libraryService.getUserLibrarySongs(userId);
        int removed = 0;
        for (Song song : songs) {
            if (!Files.exists(Path.of(song.getFilePath()))) {
                libraryService.removeSongFromUserLibrary(userId, song.getSongId());
                removed++;
            }
        }
        return removed;
    }

    // ── Folder Scanning ───────────────────────────────────────────────────

    /**
     * Scans all watched folders for this user.
     * First cleans dead files, then imports any new MP3s found.
     *
     * @param userId           the user to import songs for
     * @param progressCallback called with (filesProcessed, totalFiles) as scanning proceeds
     * @return a ScanResult summary
     */
    public ScanResult scanWatchedFolders(int userId,
                                          BiConsumer<Integer, Integer> progressCallback) {
        // Step 1: Clean dead files
        int removedCount = validateAndCleanLibrary(userId);

        // Step 2: Collect all MP3 paths from all watched folders
        List<String> folders = watchedFolderDAO.findByUser(userId);
        Set<Path> scanRoots = new LinkedHashSet<>();
        scanRoots.add(sharedSongsDir);
        for (String folder : folders) {
            scanRoots.add(Path.of(folder).toAbsolutePath().normalize());
        }
        List<Path> allMp3s = new ArrayList<>();
        for (Path root : scanRoots) {
            collectMp3s(root, allMp3s);
        }

        int total = allMp3s.size();
        int imported = 0;
        int skipped = 0;
        int failed = 0;
        Set<String> existingPaths = new HashSet<>();
        for (Song song : libraryService.getUserLibrarySongs(userId)) {
            if (song.getFilePath() != null) {
                existingPaths.add(Path.of(song.getFilePath()).toAbsolutePath().normalize().toString());
            }
        }

        for (int i = 0; i < allMp3s.size(); i++) {
            Path mp3 = allMp3s.get(i);
            String normalizedPath = mp3.toAbsolutePath().normalize().toString();
            if (existingPaths.contains(normalizedPath)) {
                skipped++;
                if (progressCallback != null) {
                    progressCallback.accept(i + 1, total);
                }
                continue;
            }
            try {
                libraryService.importSongFromFile(userId, mp3);
                imported++;
                existingPaths.add(normalizedPath);
            } catch (IllegalArgumentException e) {
                failed++;
            } catch (Exception e) {
                failed++;
            }
            if (progressCallback != null) {
                progressCallback.accept(i + 1, total);
            }
        }

        return new ScanResult(imported, removedCount, skipped, failed, total);
    }

    /**
     * Recursively collects all .mp3 files under a directory.
     * Skips hidden files and directories (starting with '.').
     */
    private void collectMp3s(Path directory, List<Path> results) {
        if (!Files.isDirectory(directory)) return;
        try (var stream = Files.walk(directory)) {
            stream.filter(p -> !Files.isDirectory(p))
                  .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mp3"))
                  .filter(p -> !p.getFileName().toString().startsWith("."))
                  .forEach(results::add);
        } catch (IOException e) {
            // Skip folders we can't read (permissions, etc.)
        }
    }

    // ── Result Record ─────────────────────────────────────────────────────

    public record ScanResult(
        int newSongsImported,
        int deadFilesRemoved,
        int alreadyInLibrary,
        int failed,
        int totalScanned
    ) {
        public String toSummaryString() {
            return String.format(
                "Scan complete: %d new songs added, %d already in library, " +
                "%d dead files removed, %d failed. (%d total scanned)",
                newSongsImported, alreadyInLibrary, deadFilesRemoved, failed, totalScanned
            );
        }
    }
}
