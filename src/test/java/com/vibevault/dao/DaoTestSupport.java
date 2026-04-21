package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.Album;
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

    Album createAlbum(AlbumDAO albumDAO, int artistId, String title) {
        return albumDAO.create(new Album(null, title, artistId, 2024, null));
    }

    Song createSong(SongDAO songDAO, int artistId, Integer albumId, String title, String filePath) {
        Song song = new Song(null, title, artistId, albumId, "Pop", 180, filePath, 1, 2024);
        return songDAO.create(song);
    }
}
