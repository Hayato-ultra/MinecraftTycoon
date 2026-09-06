# MinecraftTycoon — Dependencies

## Required Server Files

### Core Server
| File | Version | Purpose |
|------|---------|---------|
| `paper-1.21.1.jar` | 1.21.1 | PaperMC server JAR (primary server software) |
| `server.properties` | — | Server configuration (gamemode, pvp, players) |

### Plugins (Required)
| Plugin | Version | Purpose |
|--------|---------|---------|
| `BentoBox-*.jar` | 2.x | SkyBlock island management, challenges, levels |
| `BSkyBlock-*.jar` | 1.x | BentoBox addon — SkyBlock game mode |
| `Vault-*.jar` | 1.7+ | Economy API abstraction layer |
| `Multiverse-Core-*.jar` | 4.x | Multi-world management (world creation, teleportation) |

### Plugins (Optional)
| Plugin | Version | Purpose |
|--------|---------|---------|
| `LuckPerms-*.jar` | 5.x | Permission management (recommended) |
| `PlaceholderAPI-*.jar` | 2.x | Placeholder expansion for other plugins |
| `AuthMe-*.jar` | 5.x | Authentication for offline-mode servers |

### Build Dependencies (Gradle)
| Dependency | Version | Purpose |
|------------|---------|---------|
| `io.papermc.paper:paper-api` | 1.21.1-R0.1-SNAPSHOT | Paper API for compilation |
| Java | 21+ | Runtime and compilation |

## Files Generated at Runtime

These files are created by the plugin on first run:

| File | Purpose |
|------|---------|
| `plugins/GeneratorPlugin/config.yml` | Generator definitions, sell prices, daily rewards, level XP |
| `plugins/GeneratorPlugin/plugin.yml` | Command and permission registration |
| `plugins/GeneratorPlugin/generators.db` | SQLite database — all placed generators |
| `plugins/GeneratorPlugin/profiles/` | Player profile YAML files |
| `plugins/GeneratorPlugin/warps.yml` | Server warps |
| `plugins/GeneratorPlugin/homes.yml` | Player homes |
| `plugins/GeneratorPlugin/claims.yml` | Land claims |
| `plugins/GeneratorPlugin/arenas.yml` | PvP arena definitions |
| `plugins/GeneratorPlugin/pvp_stats.yml` | PvP player statistics (ELO, kills, wins) |
| `plugins/GeneratorPlugin/prestige.yml` | Prestige levels and bonuses |
| `plugins/GeneratorPlugin/reports.yml` | Player reports |

## Database Tables (SQLite)

| Table | Columns |
|-------|---------|
| `generators` | id, type, owner, world, x, y, z, level, placed_at, last_collected, active |
