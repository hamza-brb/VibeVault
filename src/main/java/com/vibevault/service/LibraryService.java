package com.vibevault.service;

import com.vibevault.dao.AlbumDAO;
import com.vibevault.dao.ArtistDAO;
import com.vibevault.dao.SongDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;
import com.vibevault.model.Artist;
import com.vibevault.model.Song;

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

    public List<Song> getUserLibrarySongs(int userId) {
        return songDAO.findByUserLibrary(userId);
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

    public record ArtistLibrarySummary(int artistId, String artistName, int songCount, int totalDurationSeconds) {
    }

    public record AlbumLibrarySummary(int albumId, String albumTitle, int songCount, int totalDurationSeconds) {
    }
}
