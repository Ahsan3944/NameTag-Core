package com.ultraop.nametag.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class JdbcDatabase {
    static final int SCHEMA_VERSION = 1;
    private static final String MIGRATION_KEY = "yaml-migration-v1";

    private final StorageConfiguration configuration;

    JdbcDatabase(StorageConfiguration configuration) {
        this.configuration = configuration;
    }

    void initialize() {
        loadDriver();
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS nametag_core_meta (" +
                                "meta_key VARCHAR(64) PRIMARY KEY, " +
                                "meta_value VARCHAR(255) NOT NULL)"
                );
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS nametag_tags (" +
                                "tag_id VARCHAR(64) PRIMARY KEY, " +
                                "payload TEXT NOT NULL)"
                );
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS nametag_player_assignments (" +
                                "player_uuid VARCHAR(36) PRIMARY KEY, " +
                                "payload TEXT NOT NULL)"
                );
            }

            int version = schemaVersion(connection);
            if (version == 0) {
                setMeta(connection, "schema-version", String.valueOf(SCHEMA_VERSION));
            } else if (version > SCHEMA_VERSION) {
                throw new IllegalStateException(
                        "Database schema " + version +
                                " is newer than supported schema " + SCHEMA_VERSION
                );
            } else if (version < SCHEMA_VERSION) {
                throw new IllegalStateException(
                        "Database schema " + version +
                                " has no registered migration to " + SCHEMA_VERSION
                );
            }
            connection.commit();
        } catch (SQLException exception) {
            throw failure("Unable to initialize database", exception);
        }
    }

    Connection open() throws SQLException {
        loadDriver();
        Connection connection = DriverManager.getConnection(
                configuration.jdbcUrl(),
                configuration.username(),
                configuration.password()
        );
        if (configuration.type() == StorageType.SQLITE) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
        }
        return connection;
    }

    boolean migrationComplete() {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT meta_value FROM nametag_core_meta WHERE meta_key = ?")) {
            statement.setString(1, MIGRATION_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && "complete".equals(result.getString(1));
            }
        } catch (SQLException exception) {
            throw failure("Unable to read storage migration state", exception);
        }
    }

    void markMigrationComplete() {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            setMeta(connection, MIGRATION_KEY, "complete");
            connection.commit();
        } catch (SQLException exception) {
            throw failure("Unable to record storage migration state", exception);
        }
    }

    int count(String table) {
        if (!table.equals("nametag_tags")
                && !table.equals("nametag_player_assignments")) {
            throw new IllegalArgumentException("Unsupported table: " + table);
        }
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        } catch (SQLException exception) {
            throw failure("Unable to count database rows", exception);
        }
    }

    private int schemaVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT meta_value FROM nametag_core_meta WHERE meta_key = ?")) {
            statement.setString(1, "schema-version");
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return 0;
                return Integer.parseInt(result.getString(1));
            }
        }
    }

    private void setMeta(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE nametag_core_meta SET meta_value = ? WHERE meta_key = ?")) {
            update.setString(1, value);
            update.setString(2, key);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO nametag_core_meta(meta_key, meta_value) VALUES (?, ?)")) {
                    insert.setString(1, key);
                    insert.setString(2, value);
                    insert.executeUpdate();
                }
            }
        }
    }

    private void loadDriver() {
        String driver = switch (configuration.type()) {
            case SQLITE -> "org.sqlite.JDBC";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
            case YAML -> null;
        };
        if (driver == null) return;
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "JDBC driver is not available for " + configuration.type(),
                    exception
            );
        }
    }

    private static IllegalStateException failure(String message, SQLException exception) {
        return new IllegalStateException(message, exception);
    }
}
