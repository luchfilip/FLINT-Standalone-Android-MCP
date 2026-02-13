# Flint SDK

Give an LLM full control of an Android device through one MCP connection.

Flint is an Android hub app that runs an MCP server directly on the device. Out of the box it exposes 25+ tools for device control: tapping, swiping, typing, reading the screen, sending SMS, making calls, managing contacts, reading notifications, launching apps, toggling system settings, and more. It works with every app on the device through Android's accessibility service and screenshots.

But raw accessibility trees are slow and noisy. Flint also includes an SDK that lets app developers annotate their code so the LLM can interact with their app directly: call named tools, read structured screen data, invoke actions by name instead of guessing coordinates. Apps don't need to go all-in. Any level of integration helps, and the Hub automatically picks the best available path.

## Why this exists

LLMs can write code. They can reason about UIs. But they can't touch a phone.

There are great projects bridging this gap, [mobile-mcp](https://github.com/mobile-next/mobile-mcp), [Android-MCP](https://github.com/CursorTouch/Android-MCP), [android-mcp-sdk](https://github.com/kaeawc/android-mcp-sdk), and [mobile-use](https://github.com/minitap-ai/mobile-use) all enable LLM interaction with mobile devices. Flint builds on the same idea with a few different choices: the MCP server runs on the device itself rather than on a laptop, and apps can optionally integrate an SDK to expose structured tools and semantic UI data alongside the standard accessibility-based approach.

## Use cases

### QA and testing

An LLM can already interact with any app through the Hub's accessibility tools. Add the SDK and tests become faster, more reliable, and easier to write.

| | Without SDK | With SDK |
|---|---|---|
| Navigate the app | Tap coordinates from accessibility tree | Call named tools: `search("jazz")`, `open_playlist(3)` |
| Verify screen content | Parse raw text nodes, hope the order is stable | Read named fields: `title`, `artist`, `duration` |
| Interact with elements | Tap at (x, y), breaks if layout changes | Invoke named actions: `play`, `favorite`, `select` |
| Know where you are | Guess from visible text | `get_screen` returns the current screen name |
| Write a test | "tap the third item in the list at coordinates 540, 890" | "call search with query 'jazz', read the results, invoke play on the first item" |

No mocking, espresso or appium. Tests run on real devices against real UIs.

### Device automation

The Hub gives any MCP client full device control out of the box: send messages, manage contacts, adjust settings, launch apps, read notifications. Users and agents can automate multi-app workflows through natural language without any app needing to integrate the SDK.

### AI exposure

MCP is becoming the standard way AI assistants (Claude, ChatGPT, Cursor, etc.) connect to external tools. Flint makes your Android device an MCP server. Apps that integrate the SDK become directly accessible to any AI assistant that supports MCP.

Think of it like this: without the SDK, an AI assistant can use your app the way a screen reader does. With the SDK, your app speaks the AI's language. It exposes tools the assistant can call, data it can read, and actions it can invoke by name.

| | Without SDK | With SDK |
|---|---|---|
| AI finds your app | Generic app in a list | Named tools show up in the assistant's tool list |
| AI navigates your app | Screenshots + coordinate tapping | Direct function calls with parameters |
| AI reads your content | Raw accessibility dump | Structured fields, typed lists, labeled values |
| AI takes actions | Best-effort coordinate taps | Named actions that survive layout/design changes |
| Your app's reliability | Depends on screen resolution, layout, language | Stable across devices and updates |

For app developers, SDK integration means your app works well with AI assistants today and is ready for whatever comes next.

## How Flint compares

| Capability | [mobile-mcp](https://github.com/mobile-next/mobile-mcp) | [Android-MCP](https://github.com/CursorTouch/Android-MCP) | [android-mcp-sdk](https://github.com/kaeawc/android-mcp-sdk) | [mobile-use](https://github.com/minitap-ai/mobile-use) | **Flint** |
|---|---|---|---|---|---|
| Runs on device (no laptop/ADB) | | | Yes | | **Yes** |
| Generic device control | Yes | Yes | | Yes | **Yes** |
| Accessibility tree reading | Yes | Yes | | Yes | **Yes** |
| App developer SDK | | | Yes (debug only) | | **Yes (production)** |
| App-specific tools | | | Yes (debug only) | | **Yes** |
| Semantic UI annotations | | | | | **Yes** |
| Screen state tracking | | | | | **Yes** |
| Progressive integration (Level 0-4) | | | | | **Yes** |
| Compile-time tool validation | | | | | **Yes** |
| Works with zero SDK in app | Yes | Yes | | Yes | **Yes** |
| iOS support | Yes | | | Partial | |

**What Flint adds:**

- **On-device MCP server.** Runs directly on the phone, MCP clients connect over WiFi. No laptop/USB/ADB needed.
- **Semantic UI layer.** Apps that integrate the SDK expose named fields (`title`, `artist`), typed lists, and named actions (`play`, `favorite`). The LLM gets structured JSON alongside the standard accessibility tree.
- **Progressive integration.** Works with every app at Level 0 (accessibility + screenshots). Each level of SDK annotation adds more structure. Apps can adopt incrementally.
- **Production app SDK.** Designed for apps shipping to users, with compile-time validation via KSP and ContentProvider-based IPC.

## How it works

The Hub is a single APK. No desktop server, ADB or Termux needed. Install it, grant permissions, and it starts an HTTP/SSE MCP server on port 8080. Any MCP client (Claude, Cursor, custom agents) connects over the network.

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

## Additional tools for Flint-enabled apps

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
Add the SDK, define a few `@FlintTool` methods, add `Flint.screen()` calls. Hub can navigate the app and knows which screen is active.

**Level 2 - Annotated content**
Add `flintContent("title")` to key UI elements. Hub reads structured data instead of raw text nodes.

**Level 3 - Named actions**
Add `flintAction("play", "Play this track")` to interactive elements. Hub invokes actions by name. No coordinates. Survives layout changes.

**Level 4 - Full annotation**
Add `flintList`, `flintItem`, `flintOverlay` throughout. Hub gets a complete structured representation. LLM operates with full context.

## Quick start

### Install the Hub

Download the latest Hub APK from [Releases](https://github.com/aspect-build/flint/releases), or build from source:

```bash
git clone https://github.com/aspect-build/flint.git
cd flint
./gradlew :hub:app:installDebug
```

Open the Hub, grant permissions (Accessibility, Notifications, SMS, Phone, Contacts), start the service.

### Connect an MCP client

Create `.mcp.json` in your project (see `.mcp.json.example`):

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

### Add the SDK to your app

The SDK is published on Maven Central as `dev.flintsdk`. Add it to your app's Gradle config:

**settings.gradle.kts** (if not already present):
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

**build.gradle.kts** (project root):
```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
}
```

**build.gradle.kts** (app module):
```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("dev.flintsdk:runtime:1.0.0")
    ksp("dev.flintsdk:compiler:1.0.0")
}
```

The `runtime` module transitively includes `annotations`, so you don't need to add it separately.

### Try the sample app

```bash
git clone https://github.com/aspect-build/flint.git
cd flint
./gradlew :sample:musicapp:installDebug
```

The Hub discovers the music app and registers its tools: `flint_music_search`, `flint_music_open_playlist`, `flint_music_read_screen`, `flint_music_action`.

## Modules

| Module | What it does |
|--------|-------------|
| `sdk/annotations` | Kotlin annotations: `@FlintTool`, `@FlintScreen`, `@FlintAction`, `@FlintSemanticTree` |
| `sdk/compiler` | KSP processor that generates `flint-manifest.json` at build time |
| `sdk/runtime` | Android library: ContentProvider, action invoker, tree walker, data models |
| `hub/app` | Android app with embedded MCP server, 25+ built-in tools, Flint app discovery |
| `sample/musicapp` | Example Flint-enabled music app with tools, screens, and semantic UI |

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
│  │ MCP Server   │  │ Device Tools │  │ Flint         │  │
│  │ (Ktor SSE)   │  │ tap, swipe,  │  │ Discovery     │  │
│  │              │  │ type, scroll │  │ scan apps,    │  │
│  │              │  │              │  │ register tools│  │
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
│  Flint-Enabled Apps                                     │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Music App (sample)                               │   │
│  │ @FlintTool search, open_playlist                 │   │
│  │ @FlintScreen home, playlist_detail, search_results│  │
│  │ @FlintSemanticTree annotated composables         │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  Any app using sdk/annotations + sdk/runtime            │
└─────────────────────────────────────────────────────────┘
```

## Docs

See [flint-full-spec-v1.md](flint-full-spec-v1.md) for the full protocol spec covering SDK integration, manifest format, ContentProvider communication, semantic UI tree structure, and security model.

## License

Apache License 2.0. See [LICENSE](LICENSE).
