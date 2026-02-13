# Flint Hub - Android Device Control

Control your Android phone directly from OpenClaw. Tap, swipe, read screen content, manage apps, send SMS, handle calls, and more.

## What makes Flint different

Flint Hub runs an MCP server directly on your phone. No laptop, USB or ADB needed. Just install the app, connect over Wi-Fi, and your AI assistant has full device control.

## Tags

`android`, `mobile`, `device-control`, `phone`, `automation`, `mcp`, `flint`

## Requirements

```yaml
requires:
  env:
    - FLINT_HOST
  packages:
    - openclaw-flint
```

## Install

```yaml
install:
  npm: openclaw-flint
  config:
    host: ${FLINT_HOST}
    port: 8080
```

## Available tools

### Device control
- **device.tap** - Tap at screen coordinates
- **device.swipe** - Swipe between two points
- **device.scroll** - Scroll in a direction
- **device.long_press** - Long press at coordinates
- **device.type** - Type text into focused field
- **device.press_key** - Press a hardware/software key (back, home, enter)
- **device.screenshot** - Capture a screenshot (returns image)
- **device.get_tree** - Get the accessibility tree (structured screen content)

### Apps
- **apps.list** - List installed apps
- **apps.launch** - Launch an app by package name
- **apps.close** - Close an app

### SMS and calls
- **sms.read** - Read SMS messages
- **sms.send** - Send an SMS
- **call.dial** - Make a phone call
- **call.answer** - Answer an incoming call

### Notifications
- **notifications.list** - List current notifications
- **notifications.tap** - Tap a notification
- **notifications.dismiss** - Dismiss a notification
- **notifications.reply** - Reply to a notification

### Contacts
- **contacts.search** - Search contacts
- **contacts.create** - Create a new contact

### Clipboard
- **clipboard.get** - Read clipboard content
- **clipboard.set** - Set clipboard content

### System
- **system.volume** - Get/set volume
- **system.wifi** - Check Wi-Fi status
- **system.bluetooth** - Check Bluetooth status
- **system.battery** - Get battery level

### Flint-enabled apps
When apps integrate the Flint SDK, additional app-specific tools appear automatically (e.g. `myapp.get_screen`, `myapp.search`). These tools are discovered dynamically.

## Usage rules

1. Use `device.get_tree` first to understand what's on screen. It returns structured data and is much faster than screenshots.
2. Use `device.screenshot` only when you need visual information (colors, images, layout).
3. For Flint-enabled apps, prefer their dedicated tools over generic device tools.
4. Use element bounds from `device.get_tree` for tap coordinates. Never guess from screenshots.
5. For scrolling content, use `device.swipe` with larger distances. `device.scroll` moves very little.

## Example

```
User: Send a text to Mom saying I'll be late

1. contacts.search({ query: "Mom" })
2. sms.send({ to: "+1234567890", message: "I'll be late" })
```
