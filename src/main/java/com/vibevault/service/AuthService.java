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
    private final UserDAO userDAO;
    private User currentUser;

    public AuthService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO must not be null");
    }

    public User register(String username, String plainPassword) {
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
