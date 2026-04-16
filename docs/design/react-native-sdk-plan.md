# Flint React Native SDK: Implementation Plan

Detailed plan for building the Flint SDK for React Native. Follows the same architecture as the Compose SDK but adapted for RN's runtime model.

---

## Architecture Overview

```
MCP Client (Claude, Cursor)
    |  HTTP/SSE
Hub MCP Server (on Android device)
    |  ContentProvider.call()
Flint RN Native Module
    |  Bridge / TurboModule (JSI)
Flint JS Runtime (React hooks + registry)
    |  refs + context
React Native UI (with Flint annotations)
```

The JS runtime maintains a registry of annotated elements. When the Hub calls `read_screen` via ContentProvider, the native module bridges to JS, walks the registry, renders flat text, and returns it through the ContentProvider response. Same flow as Compose, different plumbing.

---

## Module Structure

```
flint-react-native/
  package.json
  tsconfig.json
  src/
    index.ts                    # Public API exports
    FlintContext.tsx             # React context for registry
    FlintRegistry.ts            # Core registry (screens, tools, elements)
    FlintSnapshot.ts            # Snapshot builder (registry -> structured data)
    FlintTextRenderer.ts        # Flat text renderer (structured data -> text)
    hooks/
      useFlintScreen.ts         # Screen registration hook
      useFlintTools.ts          # Tool registration hook
      useFlintContent.ts        # Content annotation (returns spread props)
      useFlintList.ts           # List annotation
      useFlintItem.ts           # Item annotation
      useFlintAction.ts         # Action annotation
    navigation/
      FlintNavigationContainer.tsx  # Auto screen tracking wrapper
    types.ts                    # TypeScript types
  android/
    src/main/java/.../
      FlintRNModule.kt          # TurboModule / NativeModule
      FlintRNProvider.kt        # ContentProvider (Hub discovery)
      FlintRNPackage.kt         # React Native package registration
    src/main/AndroidManifest.xml
  ios/
    FlintRN.swift               # Native module
    FlintRNServer.swift         # Embedded HTTP server
    FlintRN.podspec
```

---

## Phase 1: JS Runtime Core

The foundation. Everything runs in JS. No native code yet. Testable in isolation.

### 1.1 FlintRegistry

Central store for all Flint state. Singleton (or context-scoped for testing).

```typescript
// FlintRegistry.ts

type ContentEntry = {
  key: string;
  value: string;
  listId?: string;
  itemIndex?: number;
};

type ActionEntry = {
  name: string;
  description: string;
  handler: () => void;
  listId?: string;
  itemIndex?: number;
};

type ListEntry = {
  id: string;
  description: string;
};

type ToolDef = {
  name: string;
  description: string;
  target?: string;  // expected screen after execution
  params?: ToolParam[];
  action: (params: Record<string, any>) => void;
};

type ToolParam = {
  name: string;
  type: 'string' | 'integer' | 'boolean';
  description: string;
  required?: boolean;  // default true
};

class FlintRegistry {
  private currentScreen: string | null = null;
  private tools: Map<string, ToolDef> = new Map();
  private content: Map<string, ContentEntry> = new Map();
  private lists: Map<string, ListEntry> = new Map();
  private actions: Map<string, ActionEntry> = new Map();

  // Screen
  setScreen(name: string): void;
  getScreen(): string | null;

  // Tools
  registerTools(tools: ToolDef[]): () => void;  // returns cleanup fn
  getTools(): ToolDef[];
  callTool(name: string, params: Record<string, any>): void;

  // Content
  registerContent(id: string, entry: ContentEntry): () => void;
  getContent(): ContentEntry[];

  // Lists
  registerList(id: string, entry: ListEntry): () => void;
  getLists(): ListEntry[];

  // Actions
  registerAction(id: string, entry: ActionEntry): () => void;
  getActions(): ActionEntry[];
  invokeAction(name: string, listId?: string, itemIndex?: number): void;

  // Snapshot
  snapshot(): FlintScreenSnapshot;

  // Reset (on screen change)
  clearScreenState(): void;
}
```

Key design decisions:
- Map-based storage with string keys for O(1) lookup
- Every `register*` method returns a cleanup function (for `useEffect` teardown)
- `clearScreenState()` wipes content/lists/actions when screen changes (tools may persist across screens or be screen-scoped, TBD)
- Content entries carry optional `listId` + `itemIndex` to associate with parent list items

### 1.2 FlintSnapshot

Transforms registry state into the structured snapshot model (same as `FlintModels.kt`).

```typescript
// FlintSnapshot.ts

type FlintScreenSnapshot = {
  screen: string;
  content: SnapshotElement[];
  overlays: SnapshotOverlay[];
};

type SnapshotElement =
  | { type: 'content'; key: string; value: string }
  | { type: 'list'; id: string; description: string; items: SnapshotItem[] };

type SnapshotItem = {
  index: number;
  content: Record<string, string>;
  actions: { name: string; description: string }[];
};

function buildSnapshot(registry: FlintRegistry): FlintScreenSnapshot {
  // 1. Get current screen name
  // 2. Collect all content entries NOT inside a list
  // 3. Collect all lists, group their items by listId + itemIndex
  // 4. For each item, collect content entries and actions
  // 5. Return structured snapshot
}
```

### 1.3 FlintTextRenderer

Converts structured snapshot to flat text. Direct port of `FlintTextRenderer.kt`.

```typescript
// FlintTextRenderer.ts

function renderSnapshot(snapshot: FlintScreenSnapshot): string {
  const lines: string[] = [];
  lines.push(`screen: ${snapshot.screen}`);

  for (const el of snapshot.content) {
    if (el.type === 'content') {
      lines.push(`${el.key}: ${el.value}`);
    } else if (el.type === 'list') {
      lines.push(`${el.id}:`);
      for (const item of el.items) {
        const fields = Object.entries(item.content)
          .map(([k, v]) => `${k}: ${v}`)
          .join(' | ');
        lines.push(`  [${item.index}] ${fields}`);
      }
    }
  }

  // Tool names
  const tools = registry.getTools().map(t => t.name);
  if (tools.length > 0) {
    lines.push(`tools: ${tools.join(', ')}`);
  }

  return lines.join('\n');
}
```

Output example:
```
screen: search_results
query: jazz
results:
  [0] title: Blue Train | artist: John Coltrane
  [1] title: Kind of Blue | artist: Miles Davis
tools: play_track, go_back
```

Identical to Compose SDK output. AI sees the same format regardless of platform.

### 1.4 Schema Generator

Generates the MCP-compatible tool schema (same as KSP-generated `flint-manifest.json`).

```typescript
function generateSchema(registry: FlintRegistry): FlintSchema {
  return {
    protocol: 'flint',
    version: '2.0',
    tools: registry.getTools().map(tool => ({
      name: tool.name,
      description: tool.description,
      inputSchema: {
        type: 'object',
        properties: Object.fromEntries(
          (tool.params ?? []).map(p => [p.name, {
            type: p.type,
            description: p.description,
          }])
        ),
        required: (tool.params ?? [])
          .filter(p => p.required !== false)
          .map(p => p.name),
      },
    })),
  };
}
```

---

## Phase 2: React Hooks

Thin wrappers around the registry. Each hook registers on mount, cleans up on unmount.

### 2.1 useFlintScreen

```typescript
function useFlintScreen(name: string): void {
  const registry = useFlintRegistry();  // from context

  useEffect(() => {
    registry.setScreen(name);
    return () => {
      if (registry.getScreen() === name) {
        registry.setScreen(null);
        registry.clearScreenState();
      }
    };
  }, [name]);
}
```

### 2.2 useFlintTools

```typescript
function useFlintTools(tools: ToolDef[]): void {
  const registry = useFlintRegistry();

  useEffect(() => {
    const cleanup = registry.registerTools(tools);
    return cleanup;
  }, [/* stable ref check */]);
}
```

### 2.3 Annotation Functions

These return spread-able props. The key challenge: capturing text content.

**Option A: Explicit value (recommended for v1).**

```typescript
function flintContent(key: string, value: string): object {
  // Register in registry via ref callback
  // Also sets testID for native-side discoverability
  return {
    ref: (node: any) => {
      if (node) {
        registry.registerContent(genId(), { key, value });
      }
    },
    testID: `flint:content:${key}`,
  };
}
```

Developer usage: `<Text {...flintContent("title", track.title)}>{track.title}</Text>`

The value duplication (`track.title` appears twice) is the main ergonomic cost. Worth it for reliability. No hacks to extract text from native refs.

**Option B: Component wrapper (alternative).**

```typescript
function FlintContent({ name, children }: { name: string; children: React.ReactNode }) {
  // Extract text from children (works for simple string children)
  const text = typeof children === 'string' ? children : '';
  // Register in registry
  return <>{children}</>;
}
```

Usage: `<FlintContent name="title"><Text>{track.title}</Text></FlintContent>`

Cleaner (no value duplication) but adds JSX nesting.

**Recommendation:** Ship Option A for v1. Simple, explicit, no magic. Consider Option B as a convenience wrapper later.

### 2.4 flintList, flintItem, flintAction

```typescript
function flintList(id: string, description?: string): object {
  return {
    ref: (node: any) => {
      if (node) registry.registerList(id, { id, description: description ?? id });
    },
    testID: `flint:list:${id}`,
  };
}

function flintItem(index: number, listId?: string): object {
  // listId can be inferred from nearest parent flintList via context
  // or passed explicitly
  return {
    ref: (node: any) => { /* register item */ },
    testID: `flint:item:${index}`,
  };
}

function flintAction(name: string, description?: string): object {
  return {
    ref: (node: any) => { /* register action */ },
    testID: `flint:action:${name}`,
    // Note: the actual handler comes from the onPress prop
    // We need a way to capture it. Options:
    // 1. Wrap onPress: flintAction("play", { onPress: () => play(item) })
    // 2. Separate registration: useFlintAction("play", () => play(item))
  };
}
```

**Action handler capture** is tricky. The `flintAction` spread goes on a `Pressable`/`TouchableOpacity` that already has `onPress`. Two approaches:

**Approach 1: Include handler in flintAction.**
```tsx
<Pressable {...flintAction("play", "Play track", () => play(item))}>
```
The function returns props including an `onPress` that both invokes the handler AND registers it. Clean, one call.

**Approach 2: Separate hook.**
```tsx
const playAction = useFlintAction("play", "Play track", () => play(item));
<Pressable onPress={playAction.onPress} {...playAction.props}>
```
More explicit, but verbose.

**Recommendation:** Approach 1. One call per action, matches Compose ergonomics.

### 2.5 List-Item Context

Content and actions inside list items need to know their parent list ID and item index. Use React context:

```typescript
// FlintItemContext.tsx
const FlintItemContext = createContext<{ listId: string; index: number } | null>(null);

function flintItem(index: number): object {
  // The returned ref sets up FlintItemContext.Provider around children
  // Actually, we can't inject a Provider via spread props.
  // Alternative: use a wrapper component for items.
}
```

Problem: spread props can't inject context providers. Solutions:

**Solution A: FlintItem wrapper component.**
```tsx
<FlatList {...flintList("results")} renderItem={({item, index}) => (
  <FlintItem listId="results" index={index}>
    <Text {...flintContent("title", item.title)}>{item.title}</Text>
  </FlintItem>
)} />
```

**Solution B: Explicit listId/index in content/action calls.**
```tsx
<View {...flintItem(index)}>
  <Text {...flintContent("title", item.title, "results", index)}>{item.title}</Text>
</View>
```

**Solution C: Register items by nesting order.** The registry infers parent-child relationships from registration order (items registered between list-start and list-end belong to that list). Fragile.

**Recommendation:** Solution A for clarity. `FlintItem` is the one wrapper component in the API. Everything else is spread props.

```tsx
<FlatList {...flintList("results", "Search results")} renderItem={({item, index}) => (
  <FlintItem list="results" index={index}>
    <Text {...flintContent("title", item.title)}>{item.title}</Text>
    <Text {...flintContent("artist", item.artist)}>{item.artist}</Text>
    <Pressable {...flintAction("play", "Play this track", () => play(item))}>
      <Text>Play</Text>
    </Pressable>
  </FlintItem>
)} />
```

---

## Phase 3: Navigation Integration

### 3.1 FlintNavigationContainer

Wraps React Navigation's `NavigationContainer` to automatically track screen changes.

```typescript
function FlintNavigationContainer({ children, ...props }: NavigationContainerProps) {
  const navigationRef = useNavigationContainerRef();
  const routeNameRef = useRef<string>();
  const registry = useFlintRegistry();

  return (
    <NavigationContainer
      ref={navigationRef}
      onReady={() => {
        const name = navigationRef.current?.getCurrentRoute()?.name;
        if (name) {
          routeNameRef.current = name;
          registry.setScreen(name);
        }
      }}
      onStateChange={() => {
        const name = navigationRef.current?.getCurrentRoute()?.name;
        if (name && name !== routeNameRef.current) {
          registry.clearScreenState();
          registry.setScreen(name);
          routeNameRef.current = name;
        }
      }}
      {...props}
    >
      {children}
    </NavigationContainer>
  );
}
```

Developers can use this for automatic tracking (zero per-screen code for screen names) or still use `useFlintScreen` for explicit overrides.

### 3.2 Expo Router Support

```typescript
function useFlintExpoRouter(): void {
  const pathname = usePathname();
  const registry = useFlintRegistry();

  useEffect(() => {
    registry.clearScreenState();
    registry.setScreen(pathname);
  }, [pathname]);
}
```

One call in the root layout. All route changes tracked automatically.

---

## Phase 4: Android Native Module

### 4.1 ContentProvider (Hub Discovery)

The Hub scans for `content://<package>.flint` authorities. The RN module registers one.

```kotlin
// FlintRNProvider.kt
class FlintRNProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            "get_schema" -> handleGetSchema()
            "get_screen" -> handleGetScreen()
            "read_screen" -> handleReadScreen()
            "call_tool" -> handleCallTool(arg, extras)
            "invoke_action" -> handleInvokeAction(extras)
            else -> null
        }
    }

    private fun handleReadScreen(): Bundle {
        // Bridge to JS, read registry, get flat text
        val snapshot = bridgeToJS("flint_read_screen")
        return Bundle().apply { putString("snapshot", snapshot) }
    }

    private fun handleCallTool(toolName: String?, extras: Bundle?): Bundle {
        // Bridge to JS, invoke tool, wait for screen settle, read snapshot
        val result = bridgeToJS("flint_call_tool", toolName, extras)
        return Bundle().apply { putString("result", result) }
    }
}
```

AndroidManifest.xml:
```xml
<provider
    android:name=".FlintRNProvider"
    android:authorities="${applicationId}.flint"
    android:exported="true" />
```

### 4.2 JS Bridge

The ContentProvider runs on a Binder thread. The registry lives in JS. Need to bridge synchronously.

**With Fabric/TurboModules (new architecture):**
JSI allows synchronous C++ to JS calls. The native module calls directly into JS, reads the registry, gets the snapshot string, returns it. Fast, no async overhead.

```kotlin
// FlintRNModule.kt (TurboModule)
@ReactModule(name = "FlintRN")
class FlintRNModule(reactContext: ReactApplicationContext) : NativeFlintRNSpec(reactContext) {

    // Called from ContentProvider via static reference
    fun readScreenSync(): String {
        // JSI sync call to JS
        return callJSSync("FlintBridge.readScreen")
    }

    fun callToolSync(name: String, params: Map<String, Any>): String {
        return callJSSync("FlintBridge.callTool", name, params)
    }
}
```

**With Bridge (old architecture):**
Async only. Use `CountDownLatch` pattern (same as Compose SDK's `FlintRequestHandler`).

```kotlin
fun readScreenAsync(): String {
    val latch = CountDownLatch(1)
    var result = ""

    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("flint_read_screen", null)

    // JS responds via native module callback
    pendingCallback = { snapshot ->
        result = snapshot
        latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)
    return result
}
```

### 4.3 Frame Synchronization

After a tool call navigates the app, we need to wait for React Native to re-render before reading the snapshot. Same challenge as Compose's `Choreographer.postFrameCallback()`.

```kotlin
// Wait for RN to settle after navigation
private fun waitForRender(callback: () -> Unit) {
    // Option 1: Choreographer frame callback (same as Compose SDK)
    Choreographer.getInstance().postFrameCallback {
        // One more frame to be safe (RN batches updates)
        Choreographer.getInstance().postFrameCallback {
            callback()
        }
    }

    // Option 2: InteractionManager on JS side
    // JS code: InteractionManager.runAfterInteractions(() => resolve())
}
```

Recommendation: Use JS-side `InteractionManager.runAfterInteractions` for the wait, since RN rendering is JS-driven. The native side sends "call_tool", JS invokes the tool action, waits for `InteractionManager`, reads registry, sends snapshot back.

---

## Phase 5: iOS Native Module

### 5.1 Embedded HTTP Server

iOS has no ContentProvider. Use a lightweight HTTP server.

```swift
// FlintRNServer.swift
import GCDWebServer

class FlintRNServer {
    private let server = GCDWebServer()
    private let port: UInt = 6099

    func start() {
        // GET /schema
        server.addHandler(forMethod: "GET", path: "/schema") { request in
            let schema = self.bridge.callJS("FlintBridge.getSchema")
            return GCDWebServerDataResponse(jsonObject: schema)
        }

        // GET /screen
        server.addHandler(forMethod: "GET", path: "/screen") { request in
            let screen = self.bridge.callJS("FlintBridge.getScreen")
            return GCDWebServerDataResponse(jsonObject: ["screen": screen])
        }

        // POST /read_screen
        server.addHandler(forMethod: "POST", path: "/read_screen") { request in
            let snapshot = self.bridge.callJS("FlintBridge.readScreen")
            return GCDWebServerDataResponse(text: snapshot)
        }

        // POST /call_tool
        server.addHandler(forMethod: "POST", path: "/call_tool") { request in
            let body = (request as? GCDWebServerDataRequest)?.jsonObject
            let result = self.bridge.callJS("FlintBridge.callTool", args: body)
            return GCDWebServerDataResponse(text: result)
        }

        // POST /invoke_action
        server.addHandler(forMethod: "POST", path: "/invoke_action") { request in
            let body = (request as? GCDWebServerDataRequest)?.jsonObject
            let result = self.bridge.callJS("FlintBridge.invokeAction", args: body)
            return GCDWebServerDataResponse(text: result)
        }

        server.start(withPort: port, bonjourName: nil)
    }
}
```

### 5.2 Hub Discovery on iOS

The Hub needs to find Flint-enabled iOS apps. Options:

**Option A: Bonjour/mDNS service advertisement.**
The iOS app advertises `_flint._tcp` on the local network. The Hub discovers it via mDNS. Standard iOS API (`NetService`/`NWBrowser`).

**Option B: Fixed port convention.**
App listens on port 6099 (configurable). Hub tries known ports. Less elegant but simpler.

**Option C: Deep link registration.**
App registers a URL scheme (`flint://`). Hub sends discovery ping via URL scheme. App responds with port info. Limited by iOS URL scheme restrictions.

**Recommendation:** Bonjour (Option A) for production. Fixed port (Option B) for dev/testing. Both can coexist.

---

## Phase 6: FlintProvider Context

The root-level context that provides the registry to all hooks.

```tsx
// FlintContext.tsx
const FlintRegistryContext = createContext<FlintRegistry | null>(null);

function FlintProvider({ children }: { children: React.ReactNode }) {
  const [registry] = useState(() => new FlintRegistry());

  useEffect(() => {
    // Register the bridge functions that native code calls
    FlintBridge.register({
      getSchema: () => generateSchema(registry),
      getScreen: () => registry.getScreen(),
      readScreen: () => renderSnapshot(registry.snapshot()),
      callTool: (name, params) => {
        registry.callTool(name, params);
        // Wait for render, then return snapshot
        return new Promise(resolve => {
          InteractionManager.runAfterInteractions(() => {
            resolve(renderSnapshot(registry.snapshot()));
          });
        });
      },
      invokeAction: (name, listId, itemIndex) => {
        registry.invokeAction(name, listId, itemIndex);
        return new Promise(resolve => {
          InteractionManager.runAfterInteractions(() => {
            resolve(renderSnapshot(registry.snapshot()));
          });
        });
      },
    });

    return () => FlintBridge.unregister();
  }, [registry]);

  return (
    <FlintRegistryContext.Provider value={registry}>
      {children}
    </FlintRegistryContext.Provider>
  );
}
```

App integration:
```tsx
// App.tsx
export default function App() {
  return (
    <FlintProvider>
      <FlintNavigationContainer>
        <RootNavigator />
      </FlintNavigationContainer>
    </FlintProvider>
  );
}
```

Two lines added to the root. Everything else is per-screen annotations.

---

## Phase 7: Unified Response (v2 Protocol)

Match the Compose SDK's v2 behavior: tool call returns screen + content + tools in one response.

```typescript
// In FlintBridge.callTool handler:
async function callTool(name: string, params: Record<string, any>): Promise<string> {
  // 1. Execute the tool action
  const tool = registry.getTool(name);
  tool.action(params);

  // 2. Wait for navigation + render
  await waitForInteractions();

  // 3. Build unified response
  const snapshot = registry.snapshot();
  const text = renderSnapshot(snapshot);

  // 4. Prepend tool result data (if any)
  // Format: _ok: true\n{snapshot text}
  return `_ok: true\n${text}`;
}
```

This gives the AI everything in one call, just like Compose SDK v1.3.0.

---

## Phase 8: Sample App

A sample React Native music app demonstrating all SDK features.

```
sample-music-app/
  App.tsx                   # FlintProvider + FlintNavigationContainer
  screens/
    HomeScreen.tsx          # useFlintTools for search, browse
    SearchResultsScreen.tsx # flintList + flintContent + flintAction
    PlayerScreen.tsx        # flintContent for now playing + flintAction for controls
    PlaylistScreen.tsx      # flintList for playlist tracks
```

**HomeScreen.tsx example:**
```tsx
export function HomeScreen({ navigation }) {
  useFlintTools([
    {
      name: 'search',
      description: 'Search for tracks',
      params: [{ name: 'query', type: 'string', description: 'Search query' }],
      target: 'SearchResults',
      action: ({ query }) => navigation.navigate('SearchResults', { query }),
    },
    {
      name: 'open_playlist',
      description: 'Open a playlist',
      params: [{ name: 'id', type: 'string', description: 'Playlist ID' }],
      target: 'Playlist',
      action: ({ id }) => navigation.navigate('Playlist', { id }),
    },
  ]);

  return (
    <View>
      <Text {...flintContent("greeting", "Welcome to MusicApp")}>
        Welcome to MusicApp
      </Text>
      <FlatList
        {...flintList("playlists", "Your playlists")}
        data={playlists}
        renderItem={({ item, index }) => (
          <FlintItem list="playlists" index={index}>
            <Text {...flintContent("name", item.name)}>{item.name}</Text>
            <Text {...flintContent("count", `${item.trackCount} tracks`)}>
              {item.trackCount} tracks
            </Text>
            <Pressable {...flintAction("open", "Open playlist", () =>
              navigation.navigate('Playlist', { id: item.id })
            )}>
              <Text>Open</Text>
            </Pressable>
          </FlintItem>
        )}
      />
    </View>
  );
}
```

**AI interaction with this screen:**
```
> musicapp.search(query: "jazz")

_ok: true
screen: SearchResults
query: jazz
results:
  [0] title: Blue Train | artist: John Coltrane
  [1] title: Kind of Blue | artist: Miles Davis
  [2] title: A Love Supreme | artist: John Coltrane
tools: play_track, add_to_playlist, go_back
```

---

## Implementation Order

| Phase | What | Depends On | Estimated Lines |
|-------|------|-----------|----------------|
| 1 | JS Runtime Core (registry, snapshot, text renderer, schema) | Nothing | ~400 |
| 2 | React Hooks (useFlintScreen, useFlintTools, annotations) | Phase 1 | ~200 |
| 3 | Navigation Integration (FlintNavigationContainer) | Phase 2 | ~50 |
| 4 | Android Native Module (ContentProvider + JS bridge) | Phase 1, 2 | ~250 |
| 5 | iOS Native Module (HTTP server + JS bridge) | Phase 1, 2 | ~300 |
| 6 | FlintProvider Context (root setup) | Phase 1-3 | ~80 |
| 7 | Unified Response (v2 protocol) | Phase 4, 5 | ~30 |
| 8 | Sample App | Phase 1-7 | ~400 |
| **Total** | | | **~1,700** |

### Suggested Build Order

1. **Phase 1 + 2 + 6** together. Pure JS. Test with console output. No native code.
2. **Phase 3.** Add navigation tracking. Still pure JS.
3. **Phase 4.** Android native module. Test with existing Hub.
4. **Phase 7.** Unified response. Verify Hub sees same format as Compose apps.
5. **Phase 8.** Sample app. End-to-end validation.
6. **Phase 5.** iOS. Can be done in parallel with Phase 4 by different developer.

---

## Testing Strategy

### Unit Tests (Jest)
- FlintRegistry: register/unregister/snapshot lifecycle
- FlintTextRenderer: output format matches Compose SDK exactly
- Schema generator: output matches `flint-manifest.json` format
- Hooks: register on mount, cleanup on unmount (React Testing Library)

### Integration Tests (on device)
- Hub discovers RN app via ContentProvider
- `get_schema` returns valid tool list
- `call_tool` navigates and returns correct screen + content
- `read_screen` returns flat text matching expected format
- `invoke_action` triggers correct handler
- Screen change clears previous screen's state
- Multiple screens with different tools register/unregister correctly

### Comparison Tests
- Same sample app implemented in both Compose and RN
- Verify identical `read_screen` output for equivalent screens
- Verify identical tool schemas
- Verify identical `call_tool` response format

---

## Open Questions

1. **Should tools be screen-scoped or global?** Compose SDK registers tools globally on the `Flint` singleton. But in RN, `useFlintTools` in a screen component naturally scopes to that screen's lifecycle. Probably screen-scoped is better (tools appear/disappear with screens). But some tools (like "go_home") might be global. Support both?

2. **Value duplication in flintContent.** `flintContent("title", track.title)` requires the value twice (once for annotation, once for rendering). Can we avoid this? A custom `FlintText` component could extract children text, but only works for simple string children. For now, explicit value is safest.

3. **FlatList virtualization.** FlatList only renders visible items. Items scrolled offscreen are unmounted. This means the registry only contains visible items. Is this a problem? Probably not. The AI sees what's on screen, same as a user. If it needs more, it scrolls.

4. **Concurrent screens.** Modals, bottom sheets, overlays. Multiple screens can be "active" simultaneously. The registry needs to handle this. Compose SDK uses `flintOverlay` for this. RN equivalent: a separate overlay registry that doesn't clear when the main screen changes.

5. **Expo vs bare RN.** The native modules (ContentProvider, HTTP server) require bare RN or Expo with dev client. Expo Go won't work. Document this clearly. The JS-only parts work everywhere.

6. **Hermes vs JSC.** Both JS engines support the APIs we need (refs, context, effects). No engine-specific concerns. Hermes is default since RN 0.70.

7. **React Native Web.** The JS runtime and hooks would work on web too (since it's all React). The native modules wouldn't. Could share the JS layer with a future Flint Web SDK. Worth considering in the architecture.
