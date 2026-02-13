# openclaw-flint

OpenClaw plugin for [Flint Hub](https://github.com/AcpLabs/flint). Control your Android phone through Flint's on-device MCP server.

## How it works

Flint Hub runs an MCP server directly on your Android phone. This plugin connects to it over Wi-Fi and exposes all 25+ device control tools as native OpenClaw agent tools. No laptop, USB or ADB needed.

## Quick start

### 1. Install Flint Hub on your phone

Download from [Google Play](https://play.google.com/store/apps/details?id=com.flintsdk.hub) or build from source.

### 2. Start the MCP server

Open Flint Hub on your phone and tap "Start Server". Note your phone's IP address shown in the app.

### 3. Install the plugin

```bash
npm install openclaw-flint
```

### 4. Configure

Add to your OpenClaw config:

```json
{
  "plugins": {
    "openclaw-flint": {
      "host": "192.168.1.42",
      "port": 8080
    }
  }
}
```

If you've set an auth token in Flint Hub:

```json
{
  "plugins": {
    "openclaw-flint": {
      "host": "192.168.1.42",
      "port": 8080,
      "authToken": "your-token-here"
    }
  }
}
```

### 5. Restart OpenClaw

The plugin connects automatically and registers all available tools.

## Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `host` | string | (required) | Phone IP address |
| `port` | number | 8080 | MCP server port |
| `authToken` | string | | Bearer token for authentication |
| `toolPrefix` | boolean | true | Prefix tools with `flint_` |
| `reconnectIntervalMs` | number | 5000 | Auto-reconnect interval (ms) |

## Flint vs ADB-based solutions

| | Flint Hub | android-use / DroidMind / mobile-mcp |
|---|---|---|
| Runs on | Phone (on-device) | Laptop (over ADB) |
| Requires USB | No | Yes (or wireless ADB setup) |
| Requires laptop | No | Yes |
| ADB setup | None | Required |
| Dynamic tool discovery | Yes (Flint SDK apps) | No |
| Connection | Wi-Fi | USB / wireless ADB |

## Commands

- `/flint` - Show connection status and tool count

## License

Apache-2.0
