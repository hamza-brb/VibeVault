package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Artist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryDAOTest {
    private final DaoTestSupport support = new DaoTestSupport();
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private ArtistDAO artistDAO;
    private SongDAO songDAO;

    @BeforeEach
    void setUp() {
        databaseManager = support.createInMemoryDatabase();
        userDAO = new UserDAO(databaseManager);
        artistDAO = new ArtistDAO(databaseManager);
        songDAO = new SongDAO(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldCreateArtistAlbumSongAndManageUserLibrary() {
        User user = support.createUser(userDAO, "shehryar");
        Artist artist = support.createArtist(artistDAO, "Adele");
        Song song = support.createSong(songDAO, artist.getArtistId(), "Easy On Me", "C:\\music\\easy-on-me.mp3");

        assertTrue(songDAO.addToUserLibrary(user.getUserId(), song.getSongId()));
        List<Song> librarySongs = songDAO.findByUserLibrary(user.getUserId());
        assertEquals(1, librarySongs.size());
        assertEquals("Easy On Me", librarySongs.get(0).getTitle());

        assertTrue(songDAO.removeFromUserLibrary(user.getUserId(), song.getSongId()));
        assertEquals(0, songDAO.findByUserLibrary(user.getUserId()).size());
    }

}
