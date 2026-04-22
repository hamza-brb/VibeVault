package com.vibevault.service;

import com.vibevault.dao.AlbumDAO;
import com.vibevault.dao.ArtistDAO;
import com.vibevault.dao.SongDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;
import com.vibevault.model.Artist;
import com.vibevault.model.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LibraryService {
    private final DatabaseManager databaseManager;
    private final ArtistDAO artistDAO;
    private final AlbumDAO albumDAO;
    private final SongDAO songDAO;
    private final StatsService statsService;

    public LibraryService(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.artistDAO = new ArtistDAO(databaseManager);
        this.albumDAO = new AlbumDAO(databaseManager);
        this.songDAO = new SongDAO(databaseManager);
        this.statsService = new StatsService(databaseManager);
    }

    public Song importSong(int userId, SongImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        String artistName = normalizeRequired(request.artistName(), "artistName");
        String title = normalizeRequired(request.title(), "title");
        String mediaSource = normalizeRequired(request.mediaSource(), "mediaSource");
        String albumTitle = normalizeOptional(request.albumTitle());
        String genre = normalizeOptional(request.genre());

        Artist artist = artistDAO.findByName(artistName)
                .orElseGet(() -> artistDAO.create(new Artist(null, artistName, null)));

        Integer albumId = null;
        if (albumTitle != null) {
            Album album = albumDAO.findByTitleAndArtistId(albumTitle, artist.getArtistId())
                    .orElseGet(() -> albumDAO.create(new Album(null, albumTitle, artist.getArtistId(), request.year(), null)));
            albumId = album.getAlbumId();
        }
        Integer finalAlbumId = albumId;

        Song song = songDAO.findByFilePath(mediaSource).orElseGet(() -> songDAO.create(new Song(
                null,
                title,
                artist.getArtistId(),
                finalAlbumId,
                genre,
                request.durationSeconds(),
                mediaSource,
                request.trackNumber(),
                request.year()
        )));

        songDAO.addToUserLibraryIfMissing(userId, song.getSongId());
        return song;
    }

    public int importSongs(int userId, List<Path> audioFilePaths, java.util.function.Consumer<Integer> progressCallback) {
        int count = 0;
        for (int i = 0; i < audioFilePaths.size(); i++) {
            try {
                importSongFromFile(userId, audioFilePaths.get(i));
                count++;
            } catch (Exception ignored) {
                // Skip files that fail or already exist (SongDAO uses INSERT OR IGNORE logic via service)
            }
            if (progressCallback != null) {
                progressCallback.accept(i + 1);
            }
        }
        return count;
    }

    public Song importSongFromFile(int userId, Path audioFilePath) {
        Objects.requireNonNull(audioFilePath, "audioFilePath must not be null");
        Path normalized = audioFilePath.toAbsolutePath().normalize();
        if (!Files.exists(normalized) || !Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("Selected audio file does not exist: " + normalized);
        }
        String fileName = normalized.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".mp3")) {
            throw new IllegalArgumentException("Only MP3 files are supported for playback");
        }

        FileMetadata metadata = readFileMetadata(normalized);
        Song song = importSong(userId, new SongImportRequest(
                metadata.title(),
                metadata.artistName(),
                metadata.albumTitle(),
                metadata.genre(),
                metadata.durationSeconds(),
                normalized.toString(),
                metadata.trackNumber(),
                metadata.year()
        ));
        persistAlbumCoverArtIfNeeded(song, metadata.coverArtBytes());
        return song;
    }

    public List<Song> getUserLibrarySongs(int userId) {
        return songDAO.findByUserLibrary(userId);
    }

    public boolean removeSongFromUserLibrary(int userId, int songId) {
        return songDAO.removeFromUserLibrary(userId, songId);
    }

    public List<Album> getAlbumsByArtist(int artistId) {
        return albumDAO.findByArtistId(artistId);
    }

    public List<StatsService.ArtistPlayStat> getLibraryArtistsByPlays(int userId, int limit) {
        return statsService.getTopArtists(userId, limit);
    }

    public List<ArtistLibrarySummary> getArtistBrowseSummaries(int userId) {
        String sql = "SELECT a.artist_id, a.name, COUNT(s.song_id) AS song_count, COALESCE(SUM(s.duration_seconds), 0) AS total_duration_seconds " +
                "FROM user_library ul " +
                "JOIN songs s ON s.song_id = ul.song_id " +
                "JOIN artists a ON a.artist_id = s.artist_id " +
                "WHERE ul.user_id = ? " +
                "GROUP BY a.artist_id, a.name " +
                "ORDER BY a.name ASC";
        List<ArtistLibrarySummary> summaries = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    summaries.add(new ArtistLibrarySummary(
                            rs.getInt("artist_id"),
                            rs.getString("name"),
                            rs.getInt("song_count"),
                            rs.getInt("total_duration_seconds")
                    ));
                }
            }
            return summaries;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query artist browse summaries", e);
        }
    }

    public List<AlbumLibrarySummary> getAlbumBrowseSummaries(int userId, int artistId) {
        String sql = "SELECT a.album_id, a.title, COUNT(s.song_id) AS song_count, COALESCE(SUM(s.duration_seconds), 0) AS total_duration_seconds " +
                "FROM user_library ul " +
                "JOIN songs s ON s.song_id = ul.song_id " +
                "JOIN albums a ON a.album_id = s.album_id " +
                "WHERE ul.user_id = ? AND s.artist_id = ? " +
                "GROUP BY a.album_id, a.title " +
                "ORDER BY a.title ASC";
        List<AlbumLibrarySummary> summaries = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, artistId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    summaries.add(new AlbumLibrarySummary(
                            rs.getInt("album_id"),
                            rs.getString("title"),
                            rs.getInt("song_count"),
                            rs.getInt("total_duration_seconds")
                    ));
                }
            }
            return summaries;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query album browse summaries", e);
        }
    }

    public List<Song> getSongsByArtistInUserLibrary(int userId, int artistId) {
        String sql = "SELECT s.song_id, s.title, s.artist_id, s.album_id, s.genre, s.duration_seconds, s.file_path, s.track_number, s.year " +
                "FROM songs s " +
                "JOIN user_library ul ON s.song_id = ul.song_id " +
                "WHERE ul.user_id = ? AND s.artist_id = ? " +
                "ORDER BY COALESCE(s.album_id, 0), COALESCE(s.track_number, 2147483647), s.title ASC";
        return querySongs(sql, userId, artistId);
    }

    public List<Song> getSongsByAlbumInUserLibrary(int userId, int albumId) {
        String sql = "SELECT s.song_id, s.title, s.artist_id, s.album_id, s.genre, s.duration_seconds, s.file_path, s.track_number, s.year " +
                "FROM songs s " +
                "JOIN user_library ul ON s.song_id = ul.song_id " +
                "WHERE ul.user_id = ? AND s.album_id = ? " +
                "ORDER BY COALESCE(s.track_number, 2147483647), s.title ASC";
        return querySongs(sql, userId, albumId);
    }

    private List<Song> querySongs(String sql, int userId, int filterId) {
        List<Song> songs = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, filterId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(
                            rs.getInt("song_id"),
                            rs.getString("title"),
                            rs.getInt("artist_id"),
                            (Integer) rs.getObject("album_id"),
                            rs.getString("genre"),
                            (Integer) rs.getObject("duration_seconds"),
                            rs.getString("file_path"),
                            (Integer) rs.getObject("track_number"),
                            (Integer) rs.getObject("year")
                    ));
                }
            }
            return songs;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query songs for browse view", e);
        }
    }

    private FileMetadata readFileMetadata(Path audioFilePath) {
        String fallbackTitle = stripExtension(audioFilePath.getFileName().toString());
        String title = fallbackTitle;
        String artistName = "Unknown Artist";
        String albumTitle = null;
        String genre = null;
        Integer durationSeconds = null;
        Integer trackNumber = null;
        Integer year = null;
        byte[] coverArtBytes = null;

        try {
            AudioFile audioFile = AudioFileIO.read(audioFilePath.toFile());
            Tag tag = audioFile.getTag();
            if (tag != null) {
                title = firstNonBlank(tag.getFirst(FieldKey.TITLE), title);
                artistName = firstNonBlank(tag.getFirst(FieldKey.ARTIST), artistName);
                albumTitle = normalizeOptional(tag.getFirst(FieldKey.ALBUM));
                genre = normalizeOptional(tag.getFirst(FieldKey.GENRE));
                trackNumber = parseOptionalInteger(tag.getFirst(FieldKey.TRACK));
                year = parseOptionalInteger(tag.getFirst(FieldKey.YEAR));
                Artwork firstArtwork = tag.getFirstArtwork();
                if (firstArtwork != null && firstArtwork.getBinaryData() != null && firstArtwork.getBinaryData().length > 0) {
                    coverArtBytes = firstArtwork.getBinaryData();
                }
            }

            AudioHeader audioHeader = audioFile.getAudioHeader();
            if (audioHeader != null && audioHeader.getTrackLength() > 0) {
                durationSeconds = audioHeader.getTrackLength();
            }
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException ignored) {
            // Fallback metadata is used when tags cannot be read.
        }

        return new FileMetadata(
                title,
                artistName,
                albumTitle,
                genre,
                durationSeconds,
                trackNumber,
                year,
                coverArtBytes
        );
    }

    private void persistAlbumCoverArtIfNeeded(Song song, byte[] coverArtBytes) {
        if (song.getAlbumId() == null || coverArtBytes == null || coverArtBytes.length == 0) {
            return;
        }
        Album album = albumDAO.findById(song.getAlbumId()).orElse(null);
        if (album == null) {
            return;
        }
        String currentCoverPath = normalizeOptional(album.getCoverArtPath());
        if (currentCoverPath != null && Files.exists(Path.of(currentCoverPath))) {
            return;
        }

        Path coversDirectory = Path.of("covers").toAbsolutePath().normalize();
        Path coverPath = coversDirectory.resolve("album-" + album.getAlbumId() + ".jpg");
        try {
            Files.createDirectories(coversDirectory);
            Files.write(coverPath, coverArtBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist album cover art", e);
        }
        albumDAO.updateCoverArtPath(album.getAlbumId(), coverPath.toString());
    }

    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private static String firstNonBlank(String candidate, String fallback) {
        String normalized = normalizeOptional(candidate);
        return normalized != null ? normalized : fallback;
    }

    private static Integer parseOptionalInteger(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        int slashIndex = normalized.indexOf('/');
        String firstPart = slashIndex >= 0 ? normalized.substring(0, slashIndex) : normalized;
        String numeric = firstPart.trim();
        if (numeric.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(numeric);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record SongImportRequest(
            String title,
            String artistName,
            String albumTitle,
            String genre,
            Integer durationSeconds,
            String mediaSource,
            Integer trackNumber,
            Integer year
    ) {
    }

    private record FileMetadata(
            String title,
            String artistName,
            String albumTitle,
            String genre,
            Integer durationSeconds,
            Integer trackNumber,
            Integer year,
            byte[] coverArtBytes
    ) {
    }

    public record ArtistLibrarySummary(int artistId, String artistName, int songCount, int totalDurationSeconds) {
    }

    public record AlbumLibrarySummary(int albumId, String albumTitle, int songCount, int totalDurationSeconds) {
    }
}
