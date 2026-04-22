package com.vibevault;

import com.formdev.flatlaf.FlatDarkLaf;
import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.service.AuthService;
import com.vibevault.service.LibraryService;
import com.vibevault.service.LibraryScanService;
import com.vibevault.service.PlayerService;
import com.vibevault.service.PlaylistService;
import com.vibevault.service.StatsService;
import com.vibevault.ui.VibeVaultFrame;

import javax.swing.UIManager;
import java.awt.Color;

public class Main {
    public static void main(String[] args) {
        applyThemeDefaults();

        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initializeSchema();

        Runtime.getRuntime().addShutdownHook(new Thread(databaseManager::close));

        UserDAO userDAO = new UserDAO(databaseManager);
        AuthService authService = new AuthService(userDAO);
        LibraryService libraryService = new LibraryService(databaseManager);
        libraryService.consolidateUnknownArtists();
        PlaylistService playlistService = new PlaylistService(databaseManager);
        PlayerService playerService = new PlayerService(databaseManager);
        StatsService statsService = new StatsService(databaseManager);
        LibraryScanService libraryScanService = new LibraryScanService(databaseManager, libraryService);

        VibeVaultFrame.launch(authService, libraryService, playlistService, playerService, statsService, libraryScanService);
    }

    private static void applyThemeDefaults() {
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize look and feel", e);
        }

        UIManager.put("Panel.background", new Color(0x0D1321));
        UIManager.put("Table.background", new Color(0x0D1321));
        UIManager.put("Table.selectionBackground", new Color(0x2E5077));
        UIManager.put("Table.foreground", new Color(0xEDE8D0));
        UIManager.put("Table.selectionForeground", new Color(0xEDE8D0));
        UIManager.put("ScrollPane.background", new Color(0x0D1321));
        UIManager.put("List.background", new Color(0x0D1321));
        UIManager.put("List.selectionBackground", new Color(0x2E5077));
        UIManager.put("TextField.background", new Color(0x1A2A3A));
        UIManager.put("TextField.foreground", new Color(0xEDE8D0));
        UIManager.put("TextField.caretForeground", new Color(0x6B8FA8));
        UIManager.put("PasswordField.background", new Color(0x1A2A3A));
        UIManager.put("PasswordField.foreground", new Color(0xEDE8D0));
    }
}
