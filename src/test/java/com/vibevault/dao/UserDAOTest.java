package com.vibevault.dao;

import com.vibevault.db.DatabaseManager;
import com.vibevault.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest {
    private final DaoTestSupport support = new DaoTestSupport();
    private DatabaseManager databaseManager;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        databaseManager = support.createInMemoryDatabase();
        userDAO = new UserDAO(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void shouldCreateFindUpdateAndDeleteUser() {
        User created = userDAO.create(new User(null, "hamza", "hash-1", null));
        assertTrue(created.getUserId() != null);

        User fetched = userDAO.findByUsername("hamza").orElseThrow();
        assertEquals("hamza", fetched.getUsername());

        boolean updated = userDAO.updatePasswordHash(created.getUserId(), "hash-2");
        assertTrue(updated);
        assertEquals("hash-2", userDAO.findById(created.getUserId()).orElseThrow().getPasswordHash());

        boolean deleted = userDAO.delete(created.getUserId());
        assertTrue(deleted);
        assertTrue(userDAO.findById(created.getUserId()).isEmpty());
    }
}
