package com.generator.storage;

import com.generator.GeneratorPlugin;
import com.generator.generator.Generator;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class DatabaseManager {

    private final GeneratorPlugin plugin;
    private Connection connection;
    private static final Pattern SAFE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.]+$");

    public DatabaseManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    private static String sanitize(String input, String fallback) {
        if (input == null) return fallback;
        if (!SAFE_PATTERN.matcher(input).matches()) {
            return fallback;
        }
        return input;
    }

    public void connect() {
        try {
            String dbType = plugin.getConfigManager().getDatabaseType();
            if (dbType.equalsIgnoreCase("mysql")) {
                String host = sanitize(plugin.getConfig().getString("database.host", "localhost"), "localhost");
                int port = plugin.getConfig().getInt("database.port", 3306);
                String database = sanitize(plugin.getConfig().getString("database.name", "generators"), "generators");
                String username = sanitize(plugin.getConfig().getString("database.username", "root"), "root");
                String password = plugin.getConfig().getString("database.password", "");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=10000";
                connection = DriverManager.getConnection(url, username, password);
                connection.setAutoCommit(true);
                plugin.getLogger().info("Connected to MySQL database.");
            } else {
                File dbFile = new File(plugin.getDataFolder(), "generators.db");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                connection.setAutoCommit(true);
                plugin.getLogger().info("Connected to SQLite database.");
            }

            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    public void reconnect() {
        disconnect();
        plugin.getLogger().info("Attempting database reconnection...");
        connect();
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            reconnect();
        }
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection unavailable after reconnection attempt");
        }
        return connection;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS generators (
                    id TEXT PRIMARY KEY,
                    owner_uuid TEXT NOT NULL,
                    island_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    stored_amount INTEGER NOT NULL DEFAULT 0,
                    last_generation BIGINT NOT NULL,
                    created_at BIGINT NOT NULL,
                    dirty INTEGER NOT NULL DEFAULT 0
                )
            """);
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_generators_owner
                ON generators(owner_uuid)
            """);
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_generators_island
                ON generators(island_id)
            """);
        }
    }

    public void saveGenerator(Generator gen) {
        try {
            Connection conn = getConnection();
            if (gen.getLocation() == null || gen.getLocation().getWorld() == null) {
                plugin.getLogger().warning("Cannot save generator with null world: " + gen.getId());
                return;
            }
            String worldName = gen.getLocation().getWorld().getName();
            boolean isNew = isGeneratorNew(conn, gen.getId().toString());

            String sql;
            if (isNew) {
                sql = "INSERT INTO generators (id, owner_uuid, island_id, type, level, world, x, y, z, stored_amount, last_generation, created_at, dirty) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                sql = "UPDATE generators SET owner_uuid=?, island_id=?, type=?, level=?, world=?, x=?, y=?, z=?, stored_amount=?, last_generation=?, dirty=? WHERE id=?";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (isNew) {
                    ps.setString(1, gen.getId().toString());
                    ps.setString(2, gen.getOwnerUUID().toString());
                    ps.setString(3, gen.getIslandId());
                    ps.setString(4, gen.getType());
                    ps.setInt(5, gen.getLevel());
                    ps.setString(6, worldName);
                    ps.setInt(7, gen.getLocation().getBlockX());
                    ps.setInt(8, gen.getLocation().getBlockY());
                    ps.setInt(9, gen.getLocation().getBlockZ());
                    ps.setInt(10, gen.getStoredAmount());
                    ps.setLong(11, gen.getLastGeneration());
                    ps.setLong(12, System.currentTimeMillis());
                    ps.setInt(13, gen.isDirty() ? 1 : 0);
                } else {
                    ps.setString(1, gen.getOwnerUUID().toString());
                    ps.setString(2, gen.getIslandId());
                    ps.setString(3, gen.getType());
                    ps.setInt(4, gen.getLevel());
                    ps.setString(5, worldName);
                    ps.setInt(6, gen.getLocation().getBlockX());
                    ps.setInt(7, gen.getLocation().getBlockY());
                    ps.setInt(8, gen.getLocation().getBlockZ());
                    ps.setInt(9, gen.getStoredAmount());
                    ps.setLong(10, gen.getLastGeneration());
                    ps.setInt(11, gen.isDirty() ? 1 : 0);
                    ps.setString(12, gen.getId().toString());
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save generator: " + e.getMessage());
        }
    }

    private boolean isGeneratorNew(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM generators WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }

    public void deleteGenerator(UUID generatorId) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM generators WHERE id = ?")) {
            ps.setString(1, generatorId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete generator: " + e.getMessage());
        }
    }

    public Generator loadGenerator(ResultSet rs) throws SQLException {
        String idStr = rs.getString("id");
        String ownerStr = rs.getString("owner_uuid");
        String islandId = rs.getString("island_id");
        String type = rs.getString("type");
        int level = rs.getInt("level");
        String worldName = rs.getString("world");
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");
        int storedAmount = rs.getInt("stored_amount");
        long lastGeneration = rs.getLong("last_generation");

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for generator " + idStr);
            return null;
        }

        Location location = new Location(world, x, y, z);

        return Generator.fromLocation(
                UUID.fromString(idStr),
                UUID.fromString(ownerStr),
                islandId,
                type,
                location,
                level,
                storedAmount,
                lastGeneration
        );
    }

    public List<Generator> loadAllGenerators() {
        List<Generator> generators = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM generators")) {
            while (rs.next()) {
                Generator gen = loadGenerator(rs);
                if (gen != null) {
                    generators.add(gen);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load generators: " + e.getMessage());
        }
        return generators;
    }

    public List<Generator> loadPlayerGenerators(UUID playerUUID) {
        List<Generator> generators = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM generators WHERE owner_uuid = ?")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Generator gen = loadGenerator(rs);
                    if (gen != null) {
                        generators.add(gen);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player generators: " + e.getMessage());
        }
        return generators;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
