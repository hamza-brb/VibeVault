package com.vibevault;

import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.service.AuthService;
import com.vibevault.service.LibraryService;
import com.vibevault.service.PlayerService;
import com.vibevault.service.PlaylistService;
import com.vibevault.service.StatsService;
import com.vibevault.ui.VibeVaultFrame;

public class Main {
    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initializeSchema();

        Runtime.getRuntime().addShutdownHook(new Thread(databaseManager::close));

        UserDAO userDAO = new UserDAO(databaseManager);
        AuthService authService = new AuthService(userDAO);
        LibraryService libraryService = new LibraryService(databaseManager);
        PlaylistService playlistService = new PlaylistService(databaseManager);
        PlayerService playerService = new PlayerService(databaseManager);
        StatsService statsService = new StatsService(databaseManager);

        VibeVaultFrame.launch(authService, libraryService, playlistService, playerService, statsService);
    }
}
