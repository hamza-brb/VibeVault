package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;
import com.vibevault.model.Artist;
import com.vibevault.model.Playlist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistDAOTest {
    private final DaoTestSupport support = new DaoTestSupport();
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private ArtistDAO artistDAO;
    private AlbumDAO albumDAO;
    private SongDAO songDAO;
    private PlaylistDAO playlistDAO;

    @BeforeEach
    void setUp() {
        databaseManager = support.createInMemoryDatabase();
        userDAO = new UserDAO(databaseManager);
        artistDAO = new ArtistDAO(databaseManager);
        albumDAO = new AlbumDAO(databaseManager);
        songDAO = new SongDAO(databaseManager);
        playlistDAO = new PlaylistDAO(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldCreatePlaylistAddSongAndDelete() {
        User user = support.createUser(userDAO, "hamza");
        Artist artist = support.createArtist(artistDAO, "Coldplay");
        Album album = support.createAlbum(albumDAO, artist.getArtistId(), "Parachutes");
        Song song = support.createSong(songDAO, artist.getArtistId(), album.getAlbumId(), "Yellow", "C:\\music\\yellow.mp3");

        Playlist playlist = playlistDAO.create(new Playlist(null, user.getUserId(), "Favourites", null));
        assertTrue(playlist.getPlaylistId() != null);
        assertTrue(playlistDAO.addSong(playlist.getPlaylistId(), song.getSongId(), 1));

        List<Song> songs = playlistDAO.getSongs(playlist.getPlaylistId());
        assertEquals(1, songs.size());
        assertEquals("Yellow", songs.get(0).getTitle());

        assertTrue(playlistDAO.removeSong(playlist.getPlaylistId(), song.getSongId()));
        assertEquals(0, playlistDAO.getSongs(playlist.getPlaylistId()).size());

        assertTrue(playlistDAO.delete(playlist.getPlaylistId()));
    }
}
