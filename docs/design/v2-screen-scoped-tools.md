# FLINT v2: Screen-Scoped Dynamic Tools

## The Problem with v1

In v1, tools are defined in a standalone class (`MusicTools`, `TechDeckTools`) registered once in `Application.onCreate()`. This creates a structural problem: the tool class needs access to things it doesn't own.

A music app's `search` tool needs the NavController. A game's `ollie` tool needs the GameController. These objects live in Compose or ViewModel scope. The tool class lives in Application scope. To bridge the gap, apps end up creating singletons with volatile references (`FlintBridge`) that hold pointers to whatever the tool needs. Compose sets them, tools read them.

This breaks down in practice:
- The bridge is a global mutable singleton — the exact pattern Compose and DI exist to avoid
- Tools are always "available" even when they make no sense (ollie from the menu screen)
- The schema is static — generated at compile time, never reflects actual app state
- Every new tool needs wiring in the bridge, the tool class, and the screen
- For a real app with dozens of screens, the tool class becomes a god object

The deeper issue: tools are defined far from where they execute. The screen knows what it can do. The tool class is a middleman.

## The Design Principle

Each screen declares what it can handle, right where it handles it. When the screen is active, its tools exist. When it's gone, they're gone. The LLM always sees exactly what's possible right now.

This is how intents and deep links already work in Android — the active component handles what it supports. FLINT should follow the same pattern.

## Target Developer Experience

### Declaring tools on a screen

```kotlin
// GameScreen.kt
@Composable
fun GameScreen(gameController: GameController, speedController: GameSpeedController) {
    Flint.screen("Game")

    Flint.tools {
        tool("ollie", "Trigger an ollie trick") {
            gameController.triggerOllie()
        }
        tool("set_speed", "Set game speed (0-20)") { params ->
            val speed = params["speed"]?.toFloatOrNull() ?: return@tool
            speedController.setSpeed(speed.coerceIn(0f, 20f))
        }
    }

    // ... rest of composable
}
```

```kotlin
// HomeScreen.kt
@Composable
fun HomeScreen(onNavigate: (Route) -> Unit) {
    Flint.screen("Menu")

    Flint.tools {
        tool("start_game", "Start the skateboard game") {
            onNavigate(Route.Game)
        }
        tool("open_settings", "Open settings") {
            onNavigate(Route.Settings)
        }
    }

    // ... rest of composable
}
```

No separate tool class. No bridge. No god object. The tools live with the code that executes them, using whatever's already in scope.

### What the LLM sees

When on Menu screen:
```
screen: Menu
tools: start_game, open_settings
```

After calling `start_game`:
```
screen: Game
speed: 0.0
score: 0
tools: ollie, set_speed
```

The LLM doesn't need to guess what's available. The response tells it.

### Chaining and params

The `tool()` builder supports parameters inline:

```kotlin
Flint.tools {
    tool("search", "Search for tracks") {
        param("query", "string", "Search query", required = true)
        action { params ->
            viewModel.search(params["query"] as String)
        }
    }
}
```

Or the short form when there are no params:

```kotlin
Flint.tools {
    tool("go_back", "Navigate back") { navController.popBackStack() }
}
```

## What Changes in the SDK

### 1. New: `Flint.tools {}` composable with builder DSL

This is the core addition. A `@Composable` function that:
- Takes a builder lambda where the developer declares tools
- Internally uses `DisposableEffect` to register/unregister a handler
- Tools declared here are only active while the composable is in the tree

The builder creates a `FlintToolHandler` from the declared tools. On composition, it registers with `Flint.add()`. On dispose, it calls `Flint.remove()`. The developer never sees this — they just declare tools.

The DSL needs a `FlintToolsScope` class that collects tool declarations:
- `tool(name, description, action)` — no-param tool
- `tool(name, description, builder)` — tool with params and action
- `param(name, type, description, required)` — within the builder

### 2. New: Dynamic schema via `Flint.liveSchema()`

Replace the static `FlintSchemaHolder.JSON` lookup with a runtime method that builds the schema from currently registered handlers.

Each handler registered via `Flint.tools {}` carries its own tool metadata (name, description, params). `liveSchema()` collects all currently registered handlers and builds the schema JSON on the fly.

The `get_schema` ContentProvider endpoint switches from loading `FlintSchemaHolder` via reflection to calling `Flint.liveSchema()`.

This means `get_schema` always returns exactly the tools that are available right now, on the current screen.

### 3. New: Unified tool response format

Every `call_tool` response automatically includes the post-action state. After executing the tool's action, the ContentProvider:
1. Runs the tool action
2. Waits one frame for Compose to recompose (the tool may have triggered navigation)
3. Reads the current screen name
4. Reads the live schema (what tools are now available)
5. Reads the semantic tree (screen content)
6. Returns it all in one response

Target response format — flat, readable, LLM-friendly:

```
screen: Game
speed: 12.3
score: 200
tools: ollie, set_speed
```

Not nested JSON with `_type` discriminators. The LLM reads this like a human reads a dashboard.

For more complex screens with lists, use a simple indented format:

```
screen: search_results
query: "jazz"
results:
  [0] title: Blue Train | artist: John Coltrane
  [1] title: Kind of Blue | artist: Miles Davis
tools: play_track, go_back
```

This replaces the current `FlintScreenSnapshot` JSON serialization for the ADB mode response path. The Bundle-based `call()` path can keep structured data for the Hub.

### 4. Modified: `FlintProvider` response flow

Currently `call_tool` returns just `{"_target": "Game"}`. Change it to:
1. Execute the tool action on main thread (existing behavior)
2. Post a follow-up frame callback to let Compose settle
3. Collect: screen name, semantic tree, live tool list
4. Return the combined response

This eliminates the LLM's need to call `get_screen` and `read_screen` after every tool call. One round trip, full state.

### 5. Modified: Handler interface

The current `FlintToolHandler` interface only routes tool calls. For dynamic schema, handlers need to also report their tool metadata.

Add a method to the interface:

```kotlin
interface FlintToolHandler {
    fun onToolCall(name: String, params: Map<String, Any?>): Map<String, Any?>?
    fun describeTools(): List<FlintToolDescriptor>  // new
}
```

Where `FlintToolDescriptor` carries name, description, params, and screen scope. The DSL-built handler implements this automatically from the builder declarations.

### 6. Keep: KSP annotations as an option

The annotation-based approach (`@FlintToolHost`, `@FlintTool`) still works for tools that are truly global — always available regardless of screen. A music app might want a `pause` tool that works everywhere. These register in `Application.onCreate()` as before.

But the KSP-generated `FlintSchemaHolder` is no longer the source of truth for `get_schema`. Instead, the live registry is. KSP-generated routers implement the new `describeTools()` method so they participate in the dynamic schema.

### 7. Modified: Navigation handling

Navigation is not special. It's just a tool declared by the screen that handles it. The menu screen declares `start_game`. The settings screen declares `go_back`. No built-in navigate tool, no NavController reference in the SDK.

This means navigation works exactly like any other action — scoped to the screen that can perform it. The LLM sees `start_game` on Menu, calls it, gets the Game screen back with its tools.

Remove the `target` field from `@FlintTool`. It was a hint for where a tool navigates to, but with unified responses the LLM sees the result directly.

## What Changes in the Sample App (MusicApp)

The sample app demonstrates the new pattern:

### Before (v1)
```
MusicApplication.onCreate() → Flint.add(FlintRouter_MusicTools(MusicTools(navigator)))
MusicTools.kt → @FlintToolHost class with search, open_playlist, go_home
HomeScreen.kt → Flint.screen("home") + flintList/flintAction annotations
```

### After (v2)
```
MusicApplication.onCreate() → Flint.init(this, adbMode = BuildConfig.DEBUG)
HomeScreen.kt → Flint.screen("home") + Flint.tools { tool("search", ...) { ... } }
SearchResultsScreen.kt → Flint.screen("search_results") + Flint.tools { tool("play_track", ...) { ... } }
```

MusicTools.kt is deleted. FlintRouter_MusicTools is never generated. Each screen owns its tools.

The AppNavigatorHolder DI module is also unnecessary — screens already have `navController` in scope from the NavGraph lambda.

## What Changes in Consumer Apps (TechDeck)

### Delete
- `flint/FlintBridge.kt` — no more singleton bridge
- `flint/TechDeckTools.kt` — no more centralized tool class

### Modify
- `TechDeckApp.kt` — remove `Flint.add(FlintRouter_TechDeckTools(...))`, keep only `Flint.init()`
- `NavGraph.kt` — each composable block declares its own `Flint.tools {}` alongside existing `Flint.screen()`
- `GameScreen.kt` — tools declared inline using controllers already in scope, remove DisposableEffect bridge wiring

### Result
Each screen is self-contained. Adding a new tool means adding one `tool()` call in the screen that handles it. No other files touched.

## Implementation Order

### Phase 1: Runtime DSL
- `FlintToolsScope` builder class
- `Flint.tools {}` composable with lifecycle management
- `FlintToolDescriptor` data class
- Updated `FlintToolHandler` interface with `describeTools()`

### Phase 2: Dynamic schema
- `Flint.liveSchema()` that builds JSON from registered handlers
- `FlintProvider.handleGetSchema()` switches to live schema
- `FlintProvider.handleCallToolJson()` handles string-to-type coercion for params (fixes the current bug where ADB URI params are all strings)

### Phase 3: Unified response
- `FlintProvider.handleCallTool()` returns screen + tools + semantic state after each call
- New flat text response format for ADB mode
- One-frame delay after tool execution to let Compose settle before reading state

### Phase 4: Sample app migration
- Migrate MusicApp screens to `Flint.tools {}`
- Delete MusicTools.kt
- Delete AppNavigatorHolder (screens use NavController directly)
- Update tests

### Phase 5: Cleanup
- Remove `target` from `@FlintTool` annotation (or deprecate)
- Mark `FlintSchemaHolder` generation as optional in KSP processor
- Update KSP-generated routers to implement `describeTools()`
- Update documentation

## Backward Compatibility

The v1 annotation-based approach continues to work. Apps don't have to migrate immediately. The key change is that `get_schema` becomes dynamic — it returns tools from both KSP-generated routers and DSL-declared tools. An app can mix both: global tools via annotations, screen-scoped tools via `Flint.tools {}`.

The ContentProvider URI scheme and method names stay the same. The Hub doesn't need changes. The only external-facing difference is richer `call_tool` responses and a dynamic schema.
