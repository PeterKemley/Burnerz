# 🔥 Burnerz

**Burnerz** is a lightweight, server-side Fabric mod that allows server administrators to configure custom furnace fuels and burn times.

Turn almost any item into fuel using a simple JSON configuration file — no client installation required.

## ✨ Features

- 🔥 Add custom furnace fuels
- ⏱️ Configure custom burn times in ticks
- 🧱 Works with furnaces, smokers and blast furnaces
- 🖥️ Server-side only — players do not need the mod installed
- ⚡ Enable or disable custom fuels at runtime
- 🔄 Reload the configuration without restarting the server
- 💾 Simple JSON configuration
- 🪶 Lightweight with no unnecessary features

## ⚙️ Configuration

Burnerz creates:

`config/burnerz.json`

Example:

```json
{
  "enabled": true,
  "fuels": {
    "minecraft:cobblestone": 200,
    "minecraft:rotten_flesh": 400,
    "minecraft:poisonous_potato": 100
  }
}
```

Burn times are measured in **ticks**.

- `20` ticks = 1 second
- `200` ticks = 10 seconds / 1 normal furnace smelt
- `1600` ticks = 80 seconds / equivalent to coal

## 💬 Commands

| Command            | Description                     |
| ------------------ | ------------------------------- |
| `/burnerz help`    | Displays the available commands |
| `/burnerz reload`  | Reloads `burnerz.json`          |
| `/burnerz enable`  | Enables custom fuels            |
| `/burnerz disable` | Disables custom fuels           |

Commands require administrator/operator permissions.

When Burnerz is disabled, fuel that is **already burning will finish its current burn cycle**, but additional custom fuel will not be consumed.

## 📦 Requirements

- Minecraft 26.2
- Fabric Loader 0.19.5+
- Fabric API
- Java 25+

Burnerz only needs to be installed on the **server**.

## 📜 License

CC0-1.0

## 👤 Author

**EaZeeeHD**
