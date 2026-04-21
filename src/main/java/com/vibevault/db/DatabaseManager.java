package com.vibevault.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager implements AutoCloseable {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:vibevault.db";

    private final String dbUrl;
    private volatile boolean schemaInitialized;
    private Connection keepAliveConnection;

    public DatabaseManager() {
        this(DEFAULT_DB_URL);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public synchronized void initializeSchema() {
        if (schemaInitialized) {
            return;
        }

        try {
            if (isInMemoryUrl(dbUrl)) {
                keepAliveConnection = createConnection();
            }
            String schemaSql = readSchemaResource();
            executeScript(schemaSql);
            schemaInitialized = true;
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (!schemaInitialized) {
            initializeSchema();
        }
        return createConnection();
    }

    private Connection createConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(dbUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static boolean isInMemoryUrl(String url) {
        return url.contains(":memory:") || url.contains("mode=memory");
    }

    private static String readSchemaResource() throws IOException {
        try (var stream = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("schema.sql not found in classpath");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void executeScript(String script) throws SQLException {
        List<String> statements = splitStatements(script);
        try (Connection connection = createConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static List<String> splitStatements(String script) {
        String[] raw = script.split(";");
        List<String> result = new ArrayList<>();
        for (String candidate : raw) {
            String sql = candidate.trim();
            if (!sql.isEmpty()) {
                result.add(sql);
            }
        }
        return result;
    }

    @Override
    public synchronized void close() {
        if (keepAliveConnection != null) {
            try {
                keepAliveConnection.close();
            } catch (SQLException ignored) {
                // no-op
            } finally {
                keepAliveConnection = null;
            }
        }
    }
}
