# Flint MCP Hub: Guide

The Hub is an Android app that runs an MCP server directly on the phone. It exposes 25+ device control tools and automatically discovers any Flint SDK apps installed on the device. MCP clients (Claude, Cursor, custom agents) connect over WiFi.

Use this when you need generic device control (not just one app), production/wireless access, or multi-app workflows.

## Setup

### 1. Install the Hub

Download from [Releases](https://github.com/AcpLabs/flint/releases) or build from source:

```bash
git clone https://github.com/AcpLabs/flint.git
cd flint
./gradlew :hub:app:installDebug
```

### 2. Grant permissions

Open the Hub. Grant: Accessibility Service, Notification Listener, SMS, Phone, Contacts. Start the server.

### 3. Connect an MCP client

Add to your project's `.mcp.json`:

```json
{
  "mcpServers": {
    "flint-hub": {
      "type": "sse",
      "url": "http://<DEVICE_IP>:8080/sse"
    }
  }
}
```

Replace `<DEVICE_IP>` with your phone's IP address (shown in the Hub app).

## Built-in tools

These work with every app on the device. No SDK needed.

### Device interaction

| Tool | Description |
|------|-------------|
| `device.screenshot` | Capture current screen (returns image) |
| `device.get_tree` | Accessibility tree of current screen |
| `device.tap` | Tap at coordinates |
| `device.long_press` | Long press at coordinates |
| `device.swipe` | Swipe between two points |
| `device.scroll` | Scroll up/down/left/right |
| `device.type` | Input text into focused field |
| `device.press_key` | Press back, home, recents, volume |

### Communication

| Tool | Description |
|------|-------------|
| `sms.send` | Send an SMS |
| `sms.read` | Read SMS messages |
| `call.dial` | Make a phone call |
| `call.answer` | Answer an incoming call |
| `contacts.search` | Search contacts |
| `contacts.create` | Create a new contact |
| `notifications.list` | List current notifications |
| `notifications.tap` | Tap a notification |
| `notifications.dismiss` | Dismiss a notification |
| `notifications.reply` | Reply to a notification |

### Apps and system

| Tool | Description |
|------|-------------|
| `apps.list` | List installed apps |
| `apps.launch` | Launch an app by package name |
| `apps.close` | Close an app |
| `system.battery` | Get battery level |
| `system.wifi` | Check WiFi status |
| `system.bluetooth` | Check Bluetooth status |
| `system.volume` | Get/set volume |
| `clipboard.get` | Read clipboard content |
| `clipboard.set` | Set clipboard content |

## Flint SDK app tools

When an app integrates the SDK, the Hub discovers it automatically via ContentProvider and registers extra tools:

| Tool | Description |
|------|-------------|
| `<app>.get_screen` | Current screen name |
| `<app>.read_screen` | Structured screen snapshot |
| `<app>.search` (example) | App-defined tool with typed parameters |
| `<app>.action` | Invoke a named action on a UI element |

The AI reads structured JSON and calls actions by name. No coordinate guessing.

## Usage tips

1. Use `device.get_tree` first to understand what's on screen. It returns structured data and is faster than screenshots.
2. Use `device.screenshot` only when you need visual context (colors, images, layout).
3. For Flint-enabled apps, prefer their dedicated tools over generic device tools.
4. Use element bounds from `device.get_tree` for tap coordinates. Never guess from screenshots.
5. For scrolling, use `device.swipe` with larger distances. `device.scroll` moves very little (~40% of screen).

## Architecture

```
MCP Client (Claude, Cursor, etc.)
    |
    |  HTTP / SSE
    |
Flint Hub (Android app, port 8080)
    |
    |-- MCP Server (Ktor SSE)
    |-- Accessibility Service (screen reading, gestures)
    |-- Device Tools (tap, swipe, type, screenshot)
    |-- Communication Tools (SMS, calls, contacts, notifications)
    |-- System Tools (battery, wifi, volume, clipboard)
    |-- Flint Discovery (scans for SDK apps via ContentProvider)
    |
    +--> Flint SDK Apps (registered automatically)
```

## OpenClaw integration

The `openclaw-flint` plugin connects OpenClaw agents to the Hub. Install with npm:

```bash
npm install openclaw-flint
```

Configure:
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

See [integrations/openclaw/README.md](../integrations/openclaw/README.md) for full docs.

## When to use Hub vs ADB

| | ADB Direct | MCP Hub |
|---|---|---|
| Best for | AI agents building/testing one app | Production automation, multi-app workflows |
| Requires | ADB connection, debug build | Hub APK installed, WiFi |
| Controls | Single app (ContentProvider) | Entire device (25+ tools) |
| App-specific tools | Yes (same ContentProvider) | Yes (auto-discovered) |
| Generic device control | Standard ADB commands | Built-in tools |
| Connection | USB or wireless ADB | WiFi (no laptop needed) |

For single-app development and testing, ADB direct is simpler. For production device automation or when you need the full device tool suite through one MCP connection, use the Hub.
