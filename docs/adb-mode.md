# Flint ADB Mode

Query and control Flint apps directly from ADB. No Hub app, no MCP server. Just `adb shell content read` commands that return clean JSON.

## When to use

You are an AI agent building or testing a Flint app. You have ADB access. The workflow:
1. Write code with Flint annotations
2. Build and install: `./gradlew :app:installDebug`
3. Query capabilities, read screens, call tools, invoke actions via ADB
4. Use `adb shell screencap` to visually confirm results

## Integration

In your Application class, enable ADB mode for debug builds:

```kotlin
import com.flintsdk.Flint

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Flint.init(this, adbMode = BuildConfig.DEBUG)
    }
}
```

Enable `buildConfig` in your app's `build.gradle.kts`:

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
}
```

That's it. The existing `FlintProvider` (declared in the SDK's manifest) handles everything.

## Security

ADB mode is gated by two checks:
- The `adbMode` flag passed to `Flint.init()`
- The app's `FLAG_DEBUGGABLE` (fallback for cold-start queries before `Application.onCreate()`)

Release builds have neither. Zero attack surface in production.

## Authority format

The ContentProvider authority is always `${applicationId}.flint`.

Example: if `applicationId = "com.example.myapp"`, the authority is `com.example.myapp.flint`.

## Commands

All commands use `adb shell content read --uri`. Each returns JSON to stdout.

### Get schema

Returns all tools, their parameters, and available screens.

```bash
adb shell content read --uri content://com.example.myapp.flint/get_schema
```

Response:
```json
{
    "protocol": "flint",
    "version": "1.0",
    "tools": [
        {
            "name": "search",
            "description": "Search for tracks.",
            "target": "search_results",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "query": { "type": "string", "description": "Search query" }
                },
                "required": ["query"]
            }
        }
    ],
    "screens": ["home", "search_results"]
}
```

### Get current screen

Returns the name of the currently visible Flint screen.

```bash
adb shell content read --uri content://com.example.myapp.flint/get_screen
```

Response:
```json
{"screen":"home"}
```

Returns `{"screen":""}` if no screen is active (app in background or not yet rendered).

### Read screen content

Returns a structured snapshot of everything on the current screen: content elements, lists with items, actions, and overlays.

```bash
adb shell content read --uri content://com.example.myapp.flint/read_screen
```

Response:
```json
{
    "screen": "home",
    "content": {
        "elements": [
            {"_type": "content", "key": "title", "value": "Now Playing"},
            {"_type": "action", "name": "play_pause", "description": "Play or pause"},
            {
                "_type": "list",
                "id": "playlists",
                "description": "Featured playlists",
                "items": [
                    {
                        "index": 0,
                        "content": {"name": "Chill Vibes"},
                        "actions": [{"name": "select", "description": "Open playlist"}]
                    }
                ]
            }
        ]
    },
    "overlays": [
        {
            "id": "player_mini",
            "description": "Mini player",
            "content": {"title": "Song Name", "artist": "Artist"},
            "actions": [{"name": "play_pause", "description": "Play or pause"}]
        }
    ]
}
```

### Call a tool

Invokes a registered Flint tool. Tools typically navigate between screens.

```bash
adb shell "content read --uri 'content://com.example.myapp.flint/call_tool?_tool=search&query=rock'"
```

The `_tool` parameter is required. All other query parameters are passed as tool arguments.

Response:
```json
{"_target":"search_results"}
```

Note: wrap the full command in double quotes when using `&` for multiple parameters. The shell interprets bare `&` as background execution.

### Invoke an action

Triggers a semantic action on the current screen. Actions come from `read_screen` output.

**Screen-level action:**
```bash
adb shell "content read --uri 'content://com.example.myapp.flint/invoke_action?_action=play_pause'"
```

**List item action:**
```bash
adb shell "content read --uri 'content://com.example.myapp.flint/invoke_action?_action=select&_list_id=playlists&_item_index=0'"
```

Parameters:
- `_action` (required): action name from the screen snapshot
- `_list_id` (optional): targets a specific list
- `_item_index` (optional): targets a specific item within that list

Response:
```json
{"success":true}
```

## Device-level controls

Use standard ADB commands for device interactions outside the app:

```bash
# Press back button
adb shell input keyevent KEYCODE_BACK

# Go to device home screen
adb shell input keyevent KEYCODE_HOME

# Take a screenshot (verify visual state)
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png

# Tap at coordinates
adb shell input tap 540 1200

# Launch the app
adb shell am start -n com.example.myapp/.MainActivity
```

## Typical agent workflow

```bash
# 1. Install the app
./gradlew :app:installDebug

# 2. Discover what the app can do
adb shell content read --uri content://com.example.myapp.flint/get_schema

# 3. Launch and wait for UI
adb shell am start -n com.example.myapp/.MainActivity
sleep 2

# 4. Check current screen
adb shell content read --uri content://com.example.myapp.flint/get_screen

# 5. Read screen content
adb shell content read --uri content://com.example.myapp.flint/read_screen

# 6. Navigate using a tool
adb shell "content read --uri 'content://com.example.myapp.flint/call_tool?_tool=search&query=jazz'"

# 7. Read the new screen
adb shell content read --uri content://com.example.myapp.flint/read_screen

# 8. Interact with a list item
adb shell "content read --uri 'content://com.example.myapp.flint/invoke_action?_action=select&_list_id=results&_item_index=0'"

# 9. Verify visually
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png
```

## Cold start behavior

Querying the ContentProvider via ADB starts the app process automatically. `get_schema` works without any UI visible. `read_screen` returns an empty snapshot gracefully. Once you launch the Activity with `am start`, screen data becomes available.

## Error handling

All errors return JSON with an `error` field:

```json
{"error":"missing _tool parameter"}
{"error":"unknown tool: nonexistent"}
{"error":"FlintSchemaHolder not found. Is KSP configured?"}
```

## Multiple devices

When multiple devices/emulators are connected, specify the target with `-s`:

```bash
adb -s emulator-5554 shell content read --uri content://com.example.myapp.flint/get_schema
adb -s DEVICE_SERIAL shell content read --uri content://com.example.myapp.flint/get_schema
```
