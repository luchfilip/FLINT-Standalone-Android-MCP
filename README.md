# ACP - Android Capability Protocol

Give an LLM full control of an Android device through one MCP connection.

ACP is an Android hub app that runs an MCP server directly on the device. Out of the box it exposes 25+ tools for device control: tapping, swiping, typing, reading the screen, sending SMS, making calls, managing contacts, reading notifications, launching apps, toggling system settings, and more. It works with every app on the device through Android's accessibility service and screenshots.

But raw accessibility trees are slow and noisy. ACP also includes an SDK that lets app developers annotate their code so the LLM can interact with their app directly: call named tools, read structured screen data, invoke actions by name instead of guessing coordinates. Apps don't need to go all-in. Any level of integration helps, and the Hub automatically picks the best available path.

## Why this exists

LLMs can write code. They can reason about UIs. But they can't touch a phone.

ACP closes that gap. Some things it enables:

- **Automated testing**: An LLM writes Android code, builds the app, installs it, then tests the actual UI directly. No mocking. No Espresso boilerplate. Just call tools and read screens.
- **Device automation**: Send messages, manage contacts, adjust settings, control apps. All through natural language.
- **End-to-end flow testing**: Navigate real app flows, verify screen content, interact with UI elements. Catch issues that unit tests miss.
- **App integration testing**: Test how your app behaves alongside other apps on a real device. Launch app A, do something, switch to app B, verify the result.
- **Accessibility auditing**: The same semantic annotations that power ACP also improve the Android accessibility tree. Two birds.

## How it works

The Hub is a single APK. No desktop server. No ADB. No Termux. Install it, grant permissions, and it starts an HTTP/SSE MCP server on port 8080. Any MCP client (Claude, Cursor, custom agents) connects over the network.

**Without SDK integration** the Hub controls any app using:
- Accessibility service for screen reading and gesture dispatch
- Screenshots for visual context
- System APIs for SMS, calls, contacts, notifications, settings

This works everywhere. It's just slower and less structured.

**With SDK integration** apps get:
- Named tools the LLM can call directly (search, navigate, open playlist)
- Structured screen snapshots instead of raw accessibility trees
- Named actions on UI elements instead of coordinate-based tapping
- Screen tracking so the Hub knows exactly where the app is

The more you annotate, the faster and more reliable the LLM interaction becomes. But you can start with zero annotations and add them gradually.

## Built-in tools (no SDK needed)

These work with every app on the device:

**Device interaction**
- `device.screenshot` - capture current screen
- `device.get_tree` - accessibility tree of current screen
- `device.tap` - tap at coordinates
- `device.long_press` - long press at coordinates
- `device.swipe` - swipe gesture
- `device.scroll` - scroll up/down/left/right
- `device.type` - input text into focused field
- `device.press_key` - back, home, recents, volume

**Communication**
- `sms.send` / `sms.read` - send and read SMS
- `call.dial` / `call.answer` - make and answer calls
- `contacts.search` / `contacts.create` - manage contacts
- `notifications.list` / `notifications.dismiss` / `notifications.tap` / `notifications.reply` - full notification control

**Apps and system**
- `apps.list` / `apps.launch` / `apps.close` - app management
- `system.battery` / `system.wifi` / `system.bluetooth` / `system.volume` - system controls
- `clipboard.get` / `clipboard.set` - clipboard access

## Additional tools for ACP-enabled apps

When an app integrates the SDK, the Hub discovers it automatically and registers extra tools:

- `<app>.search` - call app-defined tools directly (navigate, search, open content)
- `<app>.read_screen` - get structured screen data (named fields, typed values, action lists)
- `<app>.get_screen` - know which screen the app is showing
- `<app>.action` - invoke a named action on a specific UI element

The LLM doesn't need to parse a raw tree or guess coordinates. It reads structured JSON and calls actions by name.

## Progressive integration

Apps don't need full SDK integration. Each level adds more capability:

**Level 0 - No SDK (every app)**
Hub uses accessibility + screenshots. Works but slow, unstructured.

**Level 1 - Tools + screen tracking**
Add the SDK, define a few `@AcpTool` methods, add `Acp.screen()` calls. Hub can navigate the app and knows which screen is active.

**Level 2 - Annotated content**
Add `acpContent("title")` to key UI elements. Hub reads structured data instead of raw text nodes.

**Level 3 - Named actions**
Add `acpAction("play", "Play this track")` to interactive elements. Hub invokes actions by name. No coordinates. Survives layout changes.

**Level 4 - Full annotation**
Add `acpList`, `acpItem`, `acpOverlay` throughout. Hub gets a complete structured representation. LLM operates with full context.

## Quick start

### Build

```bash
git clone https://github.com/<org>/acp.git
cd acp
./gradlew build
```

### Install the Hub

```bash
./gradlew :hub:app:installDebug
```

Open the Hub, grant permissions (Accessibility, Notifications, SMS, Phone, Contacts), start the service.

### Connect an MCP client

Create `.mcp.json` in your project (see `.mcp.json.example`):

```json
{
  "mcpServers": {
    "acp-hub": {
      "type": "sse",
      "url": "http://<DEVICE_IP>:8080/sse"
    }
  }
}
```

### Try the sample app

```bash
./gradlew :sample:musicapp:installDebug
```

The Hub discovers the music app and registers its tools: `acp_music_search`, `acp_music_open_playlist`, `acp_music_read_screen`, `acp_music_action`.

## Modules

| Module | What it does |
|--------|-------------|
| `sdk/annotations` | Kotlin annotations: `@AcpTool`, `@AcpScreen`, `@AcpAction`, `@AcpSemanticTree` |
| `sdk/compiler` | KSP processor that generates `acp-manifest.json` at build time |
| `sdk/runtime` | Android library: ContentProvider, action invoker, tree walker, data models |
| `hub/app` | Android app with embedded MCP server, 25+ built-in tools, ACP app discovery |
| `sample/musicapp` | Example ACP-enabled music app with tools, screens, and semantic UI |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  MCP Client (Claude, Cursor, etc.)                      │
│                                                         │
│  Connects via HTTP/SSE to the Hub's MCP server          │
└────────────────────────┬────────────────────────────────┘
                         │
                    HTTP / SSE
                         │
┌────────────────────────▼────────────────────────────────┐
│  Hub App (Android)                                      │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ MCP Server   │  │ Device Tools │  │ ACP Discovery │  │
│  │ (Ktor SSE)   │  │ tap, swipe,  │  │ scan apps,    │  │
│  │              │  │ type, scroll │  │ register tools│  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ Comms Tools  │  │ System Tools │  │ Accessibility │  │
│  │ SMS, calls,  │  │ battery,     │  │ Service       │  │
│  │ contacts,    │  │ wifi, volume │  │ (screen read, │  │
│  │ notifications│  │ clipboard    │  │  gestures)    │  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
                  ContentProvider
                         │
┌────────────────────────▼────────────────────────────────┐
│  ACP-Enabled Apps                                       │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Music App (sample)                               │   │
│  │ @AcpTool search, open_playlist                   │   │
│  │ @AcpScreen home, playlist_detail, search_results │   │
│  │ @AcpSemanticTree annotated composables           │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  Any app using sdk/annotations + sdk/runtime            │
└─────────────────────────────────────────────────────────┘
```

## Docs

See [acp-full-spec-v1.md](acp-full-spec-v1.md) for the full protocol spec covering SDK integration, manifest format, ContentProvider communication, semantic UI tree structure, and security model.

## License

Apache License 2.0. See [LICENSE](LICENSE).
