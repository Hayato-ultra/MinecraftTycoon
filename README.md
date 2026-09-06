# MinecraftTycoon

A configuration-driven **Minecraft SkyBlock Tycoon** plugin for Paper 1.21.1 servers. Players start with nothing, earn money through generators, and progress through a full tycoon economy with prestige resets.

---

## Features

### Generator System (Core)
- **14 generator types** across 6 categories (Basic, Advanced, Ultimate, Diamond, Netherite, Money)
- **10 upgrade levels** per generator with increasing costs and output
- **Offline generation** — generators produce while server is off (calculated on join)
- **Sell chest generator** — auto-sells items, `/sell` command for manual selling
- **Money generators** — produce coins directly via Vault economy

### Economy
- **Vault integration** — works with any Vault-compatible economy plugin
- **Shop system** — 8 categories (Blocks, Ores, Food, Tools, Armor, Redstone, Decoration, Special)
- **Daily rewards** — 7-day rotation with increasing rewards
- **Player level system** — XP from block break, mob kill, generator actions

### PvP Arena
- **FFA and Duel modes**
- **3 predefined kits** — Basic, Diamond, Netherite
- **ELO ranking system** — Bronze, Silver, Gold, Diamond ranks
- **Kill/death tracking** with coin rewards based on rank
- **Auto-countdown** when enough players join

### Anti-Grief & Land Claiming
- **Block protection** — chests, shulkers, obsidian, bedrock, portals, beacons, spawners
- **PvP enforcement** — PvP only in configured worlds
- **Land claiming** — up to 5 claims per player, 5-100 block size
- **Trust system** — trust/untrust players in your claims

### Warp & Home System
- **3 homes per player** with `/home` commands
- **Server warps** — configurable cost to teleport
- **TPA/TPAHere** — teleport requests with 2-second delay
- **Movement cancellation** — teleport cancels if player moves

### Prestige System
- **100 prestige levels**
- **Requirements** — island level ≥ 50, coins ≥ 1,000,000
- **Permanent bonuses** — coin multiplier, speed boost, extra homes
- **Prestige shop** — spend prestige coins on upgrades
- **Leaderboard** — top prestige players

### Server Management
- **Admin dashboard** — GUI for server management
- **Player reports** — report system with UUID-based resolution
- **Configurable PvP worlds** — dynamic world list in config.yml
- **Role-based permissions** — BentoBox island roles via reflection

---

## Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/menu` | Open server main menu |
| `/mode` | Switch game modes |
| `/profile` | View your stats |
| `/sell` | Sell inventory items |
| `/coins` | Check your balance |
| `/daily` | Claim daily reward |
| `/shop` | Open GUI shop |
| `/kit` | Claim starter kits |
| `/trade <player>` | Trade with another player |
| `/claim` | Land claiming commands |
| `/pvp` | PvP arena commands |
| `/warp` | Warp commands |
| `/home` | Home commands |
| `/tpa <player>` | Send teleport request |
| `/tpahere <player>` | Request player to teleport to you |
| `/prestige` | Open prestige system |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/generatoradmin` | Admin generator management |
| `/antigrief` | Toggle anti-grief protection |

---

## Installation

### 1. Server Setup
```bash
# Download Paper 1.21.1
# Place paper-1.21.1.jar in server directory
# Start server to generate world files
```

### 2. Install Dependencies
```bash
# Download and place in plugins/ folder:
# - BentoBox + BSkyBlock (for SkyBlock)
# - Vault + economy plugin (EssentialsX, etc.)
# - Multiverse-Core (for multi-world)
```

### 3. Install GeneratorPlugin
```bash
# Copy GeneratorPlugin-1.0.0.jar to plugins/ folder
# Start/restart server
```

### 4. Configuration
Edit `plugins/GeneratorPlugin/config.yml`:
```yaml
generators:
  # Define your generators here
  basic-stone:
    display-name: "Basic Stone Generator"
    category: "basic"
    material: "STONE"
    cost: 100
    output: "COBBLESTONE"
    output-amount: 1
    generation-time: 30
    required-island-level: 0

settings:
  pvp-worlds:
    - "pvp"
    - "P_V_P"
```

---

## Configuration

### config.yml Structure
```yaml
# Generator definitions
generators:
  <type-id>:
    display-name: "Display Name"
    category: "basic|advanced|ultimate|diamond|netherite|money"
    material: "MATERIAL_NAME"
    cost: <integer>
    output: "MATERIAL_NAME"
    output-amount: <integer>
    generation-time: <seconds>
    required-island-level: <level>

# Sell prices (per item)
sell-prices:
  COBBLESTONE: 1
  STONE: 2
  DIAMOND: 100

# Level XP requirements
level-xp:
  1: 100
  2: 250
  # ... up to level 10

# Daily rewards (7-day rotation)
daily-rewards:
  1: 100
  2: 200
  # ... up to day 7

# XP sources
xp-sources:
  block-break: 1
  block-place: 1
  mob-kill: 5

# PvP worlds
settings:
  pvp-worlds:
    - "pvp"
    - "P_V_P"
```

---

## Building from Source

### Prerequisites
- Java 21+
- Gradle 8.x

### Build
```bash
git clone https://github.com/Hayato-ultra/MinecraftTycoon.git
cd MinecraftTycoon
./gradlew build
```

### Output
```
build/libs/GeneratorPlugin-1.0.0.jar
```

---

## File Structure

```
GeneratorPlugin/
├── build.gradle                    # Gradle build config
├── settings.gradle                 # Gradle settings
├── gradlew / gradlew.bat          # Gradle wrapper
├── gradle/wrapper/                # Gradle wrapper JAR
└── src/main/
    ├── java/com/generator/
    │   ├── GeneratorPlugin.java   # Main plugin class
    │   ├── commands/              # Command handlers
    │   ├── config/                # Configuration manager
    │   ├── economy/               # Vault economy integration
    │   ├── generator/             # Generator core logic
    │   ├── gui/                   # GUI system
    │   ├── shop/                  # Shop, kits, trading
    │   ├── grief/                 # Anti-grief & claims
    │   ├── pvp/                   # PvP arena system
    │   ├── warp/                  # Homes & warps
    │   ├── prestige/              # Prestige system
    │   ├── profile/               # Player profiles
    │   ├── level/                 # XP system
    │   ├── reports/               # Report system
    │   ├── listeners/             # Event handlers
    │   ├── storage/               # SQLite database
    │   └── util/                  # Utilities
    └── resources/
        ├── config.yml             # Default configuration
        └── plugin.yml             # Plugin metadata
```

---

## Runtime Data Files

| File | Created | Purpose |
|------|---------|---------|
| `generators.db` | On first generator placed | SQLite database |
| `profiles/<uuid>.yml` | On player join | Player stats |
| `warps.yml` | On warp creation | Server warps |
| `homes.yml` | On home set | Player homes |
| `claims.yml` | On claim creation | Land claims |
| `arenas.yml` | On arena creation | PvP arenas |
| `pvp_stats.yml` | On first PvP kill | PvP statistics |
| `prestige.yml` | On first prestige | Prestige data |
| `reports.yml` | On player report | Report data |

---

## Permissions

| Permission | Description |
|------------|-------------|
| `generators.use` | Access to all player commands |
| `generators.admin` | Admin commands and bypass protections |

---

## Version History

### 1.0.0
- Initial release
- Generator system (14 types, 10 levels)
- Economy with Vault integration
- GUI shop (8 categories)
- Kit system (7 kits)
- Player trading
- Anti-grief protection
- Land claiming system
- PvP arena (FFA/Duels, ELO)
- Warp & home system (TPA)
- Prestige system (100 levels)
- Player profiles & level system
- Admin dashboard & reports

---

## License

MIT License

## Author

Hayato-ultra

## Repository

https://github.com/Hayato-ultra/MinecraftTycoon
