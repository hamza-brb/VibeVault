package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Artist;
import com.vibevault.model.Song;
import com.vibevault.model.User;

import java.util.UUID;

class DaoTestSupport {
    DatabaseManager createInMemoryDatabase() {
        String dbName = "vibevault_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        DatabaseManager manager = new DatabaseManager(dbUrl);
        manager.initializeSchema();
        return manager;
    }

    User createUser(UserDAO userDAO, String username) {
        return userDAO.create(new User(null, username, "hash-" + username, null));
    }

    Artist createArtist(ArtistDAO artistDAO, String name) {
        return artistDAO.create(new Artist(null, name, null));
    }

    Song createSong(SongDAO songDAO, int artistId, String title, String filePath) {
        Song song = new Song(null, title, artistId, 180, filePath);
        return songDAO.create(song);
    }
}
