package com.vibevault.service;

import com.vibevault.dao.UserDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        User created = authService.register("hamzaa", "plain-password");
        User saved = userDAO.findById(created.getUserId()).orElseThrow();

        assertTrue(created.getUserId() != null);
        assertNotEquals("plain-password", saved.getPasswordHash());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        authService.register("hamzaa", "password-1");

        assertThrows(IllegalStateException.class, () -> authService.register("hamzaa", "password-2"));
    }

    @Test
    void shouldLoginAndLogoutWithSessionState() {
        authService.register("hamzaa", "secret123");

        assertTrue(authService.login("hamzaa", "secret123").isPresent());
        assertTrue(authService.isAuthenticated());
        assertTrue(authService.getCurrentUser().isPresent());

        authService.logout();

        assertFalse(authService.isAuthenticated());
        assertTrue(authService.getCurrentUser().isEmpty());
    }

    @Test
    void shouldRejectInvalidLogin() {
        authService.register("hamzaa", "secret123");

        assertTrue(authService.login("hamzaa", "wrong-password").isEmpty());
        assertFalse(authService.isAuthenticated());
    }

    @Test
    void shouldDeleteCurrentUserAccountAndClearSession() {
        User created = authService.register("hamzaa", "secret123");
        assertTrue(authService.login("hamzaa", "secret123").isPresent());

        assertTrue(authService.deleteCurrentUserAccount());
        assertTrue(userDAO.findById(created.getUserId()).isEmpty());
        assertFalse(authService.isAuthenticated());
        assertTrue(authService.getCurrentUser().isEmpty());
    }

    @Test
    void shouldRejectUsernameWithSpacesWhenRegistering() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("ham za", "secret")
        );
        assertEquals("Username cannot contain spaces", ex.getMessage());
    }

    @Test
    void shouldRejectPasswordWithSpacesWhenRegistering() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("hamza", "sec ret")
        );
        assertEquals("Password cannot contain spaces", ex.getMessage());
    }

    @Test
    void shouldPreferUsernameErrorWhenBothContainSpaces() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("ham za", "sec ret")
        );
        assertEquals("Username cannot contain spaces", ex.getMessage());
    }

    @Test
    void shouldRejectShortUsernameWhenRegistering() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("hamza", "password123")
        );
        assertEquals("Username must be at least 6 characters", ex.getMessage());
    }

    @Test
    void shouldRejectShortPasswordWhenRegistering() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("hamzaa", "secret7")
        );
        assertEquals("Password must be at least 8 characters", ex.getMessage());
    }
}
