# Flint React Native: Agent Guide

How to interact with a Flint-enabled React Native app from an AI agent or CLI tool. Uses CDP (Chrome DevTools Protocol) over Metro's WebSocket. No native modules, no ADB, no MCP server.

## Prerequisites

- React Native app running with Metro dev server (default port 8081)
- App wraps its root in `<FlintProvider>` and `<FlintNavigationContainer>`
- The `flint-cdp` CLI is included in `flint-react-native/cli/`

## Quick start

```bash
# Read current screen state
npx flint-cdp read

# Get available tools and their parameters
npx flint-cdp schema

# Call a tool
npx flint-cdp call search --params '{"query":"jazz"}'

# Invoke an action on a list item
npx flint-cdp action select playlists 0

# Get current screen name only
npx flint-cdp screen
```

## How it works

```
AI Agent / CLI
    |  CDP WebSocket (ws://localhost:8081/...)
Metro Dev Server
    |  bridges to Hermes JS runtime
Flint Registry (pure JS, global.__flint__)
    |  hooks register content, tools, actions
React Native UI
```

In dev mode, Flint exposes `globalThis.__flint__` in the JS runtime. The agent connects to Metro's CDP WebSocket and evaluates JS expressions via `Runtime.evaluate`. Every method returns a plain string.

## Commands

### readScreen()

Returns a flat text snapshot of the current screen: screen name, content fields, lists with items, actions, tools.

```bash
npx flint-cdp read
```

Output:
```
screen: home
playlists:
  [0] name: Jazz Essentials | description: Best jazz tracks | actions: select
  [1] name: Rock Classics | description: Classic rock hits | actions: select
tools: search, open_playlist
```

### getSchema()

Returns MCP-compatible JSON with tool definitions and typed parameters.

```bash
npx flint-cdp schema
```

Output:
```json
{
  "protocol": "flint",
  "version": "2.0",
  "tools": [
    {
      "name": "search",
      "description": "Search for tracks",
      "inputSchema": {
        "type": "object",
        "properties": {
          "query": { "type": "string", "description": "Search query" }
        },
        "required": ["query"]
      }
    }
  ]
}
```

### getScreen()

Returns the current screen name only.

```bash
npx flint-cdp screen
```

Output: `home`

### callTool(name, params)

Invokes a registered tool. Tools typically navigate between screens.

```bash
npx flint-cdp call search --params '{"query":"jazz"}'
```

The CLI automatically waits 500ms then reads the new screen. Adjust with `--delay`:

```bash
npx flint-cdp call search --params '{"query":"jazz"}' --delay 1000
```

### invokeAction(name, listId?, itemIndex?)

Triggers a semantic action. Actions come from `readScreen()` output.

```bash
# Screen-level action
npx flint-cdp action play_pause

# List item action
npx flint-cdp action select playlists 0
```

## Raw CDP (without the CLI)

If you prefer direct WebSocket control:

### 1. Discover the target

```bash
curl http://localhost:8081/json
```

Returns:
```json
[{
  "id": "page-1",
  "title": "Hermes React Native",
  "webSocketDebuggerUrl": "ws://localhost:8081/inspector/debug?device=0&page=1"
}]
```

### 2. Connect and evaluate

```js
const ws = new WebSocket("ws://localhost:8081/inspector/debug?device=0&page=1");

ws.send(JSON.stringify({
  id: 1,
  method: "Runtime.evaluate",
  params: {
    expression: "globalThis.__flint__.readScreen()",
    returnByValue: true
  }
}));

// Response:
// { id: 1, result: { result: { type: "string", value: "screen: home\n..." }}}
```

### 3. Multi-step flow

Use a single WebSocket connection. Creating a new connection after navigation may hit a stale CDP target.

```js
// 1. Read current state
evaluate("globalThis.__flint__.readScreen()");

// 2. Call a tool
evaluate('globalThis.__flint__.callTool("search", {"query":"jazz"})');

// 3. Wait for React to re-render
await sleep(500);

// 4. Read new state
evaluate("globalThis.__flint__.readScreen()");
```

## Typical agent workflow

```bash
# 1. Start the app (Metro must be running)
npx react-native run-android   # or run-ios

# 2. Discover what the app can do
npx flint-cdp schema

# 3. Read current screen
npx flint-cdp read

# 4. Navigate using a tool
npx flint-cdp call search --params '{"query":"jazz"}'

# 5. Read the new screen (auto-reads after call)
# Already printed by the call command

# 6. Interact with a list item
npx flint-cdp action select results 0

# 7. Read again
npx flint-cdp read
```

## CLI options

```
npx flint-cdp <command> [options]

Commands:
  read                              Read screen via readScreen()
  screen                            Get screen name via getScreen()
  schema                            Get schema via getSchema()
  call <name> [--params '{...}']    Call a tool, auto-read after
  action <name> [listId] [itemIndex]  Invoke an action

Options:
  --port <port>    Metro port (default: 8081)
  --delay <ms>     Delay after callTool before reading (default: 500)
```

## Platform support

CDP goes through Metro, not the device. Works identically on:

- Android emulator
- iOS simulator
- Physical Android device (via `adb reverse`, already set up for dev)
- Physical iOS device (Metro on same WiFi)
- Expo dev client
- Bare React Native

## Async behavior

`callTool` triggers navigation but `Runtime.evaluate` is synchronous. It returns immediately with `"ok"`. The pattern:

1. Call the tool. Returns `"ok"` immediately.
2. Wait 300-500ms for React Navigation transition + re-render.
3. Read the new screen state.

The CLI handles this automatically. If using raw CDP, add your own delay.

## Error responses

```
"error: unknown tool"       # callTool with invalid name
"error: action not found"   # invokeAction with invalid name/target
""                          # getScreen when no screen is active
```
