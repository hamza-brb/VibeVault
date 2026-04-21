package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;
import com.vibevault.model.Artist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayHistoryDAOTest {
    private final DaoTestSupport support = new DaoTestSupport();
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private ArtistDAO artistDAO;
    private AlbumDAO albumDAO;
    private SongDAO songDAO;
    private PlayHistoryDAO playHistoryDAO;

    @BeforeEach
    void setUp() {
        databaseManager = support.createInMemoryDatabase();
        userDAO = new UserDAO(databaseManager);
        artistDAO = new ArtistDAO(databaseManager);
        albumDAO = new AlbumDAO(databaseManager);
        songDAO = new SongDAO(databaseManager);
        playHistoryDAO = new PlayHistoryDAO(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldLogPlayEventsAndCalculateListeningMinutes() {
        User user = support.createUser(userDAO, "stats-user");
        Artist artist = support.createArtist(artistDAO, "Imagine Dragons");
        Album album = support.createAlbum(albumDAO, artist.getArtistId(), "Evolve");
        Song song = support.createSong(songDAO, artist.getArtistId(), album.getAlbumId(), "Believer", "C:\\music\\believer.mp3");

        playHistoryDAO.logPlay(user.getUserId(), song.getSongId(), 120);
        playHistoryDAO.logPlay(user.getUserId(), song.getSongId(), 180);

        assertEquals(2, playHistoryDAO.findRecentByUser(user.getUserId(), 20).size());
        assertEquals(5.0, playHistoryDAO.getTotalListeningMinutes(user.getUserId()));
    }
}
