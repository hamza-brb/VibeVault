package com.vibevault.service;

import com.vibevault.dao.UserDAO;
import com.vibevault.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

public class AuthService {
    private static final int MIN_USERNAME_LENGTH = 6;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDAO userDAO;
    private User currentUser;

    public AuthService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO must not be null");
    }

    public User register(String username, String plainPassword) {
        validateRegisterNoSpaces(username, plainPassword);
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(plainPassword);

        if (userDAO.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalStateException("Username is already taken");
        }

        User user = new User(null, normalizedUsername, hashPassword(normalizedPassword), null);
        return userDAO.create(user);
    }

    public Optional<User> login(String username, String plainPassword) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(plainPassword);
        String expectedHash = hashPassword(normalizedPassword);

        Optional<User> user = userDAO.findByUsername(normalizedUsername);
        if (user.isPresent() && expectedHash.equals(user.get().getPasswordHash())) {
            currentUser = user.get();
            return user;
        }

        return Optional.empty();
    }

    public void logout() {
        currentUser = null;
    }

    public boolean deleteCurrentUserAccount() {
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user");
        }
        int userId = currentUser.getUserId();
        boolean deleted = userDAO.delete(userId);
        currentUser = null;
        return deleted;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String normalized = username.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return normalized;
    }

    private static String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return password;
    }

    private static void validateRegisterNoSpaces(String username, String plainPassword) {
        if (username != null && username.contains(" ")) {
            throw new IllegalArgumentException("Username cannot contain spaces");
        }
        if (plainPassword != null && plainPassword.contains(" ")) {
            throw new IllegalArgumentException("Password cannot contain spaces");
        }
        String normalizedUsername = username == null ? null : username.trim();
        if (normalizedUsername != null && !normalizedUsername.isEmpty() && normalizedUsername.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be at least 6 characters");
        }
        if (plainPassword != null && !plainPassword.isBlank() && plainPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }

    private static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
