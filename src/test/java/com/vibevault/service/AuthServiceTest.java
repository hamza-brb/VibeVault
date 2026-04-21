package com.vibevault.service;

import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private DatabaseManager databaseManager;
    private UserDAO userDAO;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        String dbName = "vibevault_auth_" + UUID.randomUUID();
        String dbUrl = "jdbc:sqlite:file:" + dbName + "?mode=memory&cache=shared";
        databaseManager = new DatabaseManager(dbUrl);
        databaseManager.initializeSchema();

        userDAO = new UserDAO(databaseManager);
        authService = new AuthService(userDAO);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldRegisterAndStorePasswordHash() {
        User created = authService.register("hamza", "plain-password");
        User saved = userDAO.findById(created.getUserId()).orElseThrow();

        assertTrue(created.getUserId() != null);
        assertNotEquals("plain-password", saved.getPasswordHash());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        authService.register("hamza", "password-1");

        assertThrows(IllegalStateException.class, () -> authService.register("hamza", "password-2"));
    }

    @Test
    void shouldLoginAndLogoutWithSessionState() {
        authService.register("hamza", "secret");

        assertTrue(authService.login("hamza", "secret").isPresent());
        assertTrue(authService.isAuthenticated());
        assertTrue(authService.getCurrentUser().isPresent());

        authService.logout();

        assertFalse(authService.isAuthenticated());
        assertTrue(authService.getCurrentUser().isEmpty());
    }

    @Test
    void shouldRejectInvalidLogin() {
        authService.register("hamza", "secret");

        assertTrue(authService.login("hamza", "wrong-password").isEmpty());
        assertFalse(authService.isAuthenticated());
    }
}
