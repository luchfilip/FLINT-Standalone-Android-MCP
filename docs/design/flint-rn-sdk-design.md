# Flint React Native SDK: Design & Implementation

Pure TypeScript library. No native modules. Debug-mode transport via CDP (Chrome DevTools Protocol). The LLM connects to Metro, evaluates JS expressions, reads screen state. Same feel as ADB.

---

## How It Works

```
LLM / CLI tool
    |  CDP WebSocket (ws://localhost:8081/...)
Metro Dev Server
    |  bridges to Hermes JS runtime
Flint Registry (pure JS)
    |  hooks register content, tools, actions
React Native UI (with Flint annotations)
```

The app annotates its UI with hooks and spread functions. These register entries in an in-memory registry. The registry exposes itself on `global.__flint__` in dev mode. An LLM or CLI tool connects to Metro's CDP WebSocket and calls `Runtime.evaluate` to read screen state or invoke tools.

No server in the app. No native code. No ADB commands. Just JS evaluation over CDP.

---

## Transport: CDP via Metro

### Discovery

Metro exposes debuggable targets at `http://localhost:8081/json`. Returns a JSON array of targets, each with a `webSocketDebuggerUrl`.

```bash
# List targets
curl http://localhost:8081/json

# Response (simplified):
[{
  "id": "page-1",
  "title": "Hermes React Native",
  "webSocketDebuggerUrl": "ws://localhost:8081/inspector/debug?device=0&page=1"
}]
```

### Connecting

```typescript
const ws = new WebSocket("ws://localhost:8081/inspector/debug?device=0&page=1");
```

### Evaluating

```typescript
// Read screen
ws.send(JSON.stringify({
  id: 1,
  method: "Runtime.evaluate",
  params: {
    expression: "global.__flint__.readScreen()",
    returnByValue: true
  }
}));

// Response:
{ id: 1, result: { result: { type: "string", value: "screen: home\nplaylists:\n  [0] name: Jazz | description: Best jazz tracks\ntools: search, open_playlist" }}}
```

### Full command set

| Expression | Returns | Purpose |
|---|---|---|
| `global.__flint__.readScreen()` | flat text snapshot | Read current screen state |
| `global.__flint__.getSchema()` | JSON tool definitions | List available tools |
| `global.__flint__.getScreen()` | screen name string | Current screen name |
| `global.__flint__.callTool("search", {query:"jazz"})` | `"ok"` | Invoke a tool |
| `global.__flint__.invokeAction("play", "results", 0)` | `"ok"` or `"not_found"` | Invoke a semantic action |

### Async after tool calls

`callTool` triggers navigation but `Runtime.evaluate` is synchronous. It can't wait for React to re-render. The pattern:

1. Evaluate `callTool("search", {query: "jazz"})`. Returns immediately with `"ok"`.
2. Wait 300-500ms (React Navigation transition + re-render).
3. Evaluate `readScreen()`. Returns the new screen state.

This is the same pattern as ADB commands. Send a tap, wait, read state. Simple. No callback plumbing.

For v2, `callTool` could return a Promise and CDP's `awaitPromise` parameter handles it. But polling works fine for debug mode.

### Cross-platform

CDP goes through Metro, not the device. Works identically on:
- Android emulator (Metro on localhost)
- iOS simulator (Metro on localhost)
- Physical Android device (Metro on localhost via `adb reverse`, already set up for dev)
- Physical iOS device (Metro on same WiFi, already set up for dev)
- Expo dev client (same Metro)
- Bare RN (same Metro)

Zero platform-specific code in the transport layer.

---

## Library Architecture

```
flint-react-native/
  src/
    index.ts                     # Public API exports
    FlintProvider.tsx             # Root context provider
    FlintRegistry.ts             # Core state store
    FlintSnapshot.ts             # Registry -> structured snapshot
    FlintTextRenderer.ts         # Snapshot -> flat text (matches Compose SDK output)
    FlintBridge.ts               # global.__flint__ exposure (dev only)
    hooks/
      useFlintScreen.ts          # Screen name registration
      useFlintTools.ts           # Tool definition hook
      useFlintContent.ts         # Content annotation (returns spread props)
      useFlintList.ts            # List annotation
      useFlintAction.ts          # Action annotation
    navigation/
      FlintNavigationContainer.tsx  # Auto screen tracking (React Navigation)
      useFlintExpoRouter.ts         # Auto screen tracking (Expo Router)
    types.ts                     # TypeScript types
  __tests__/
    FlintRegistry.test.ts
    FlintSnapshot.test.ts
    FlintTextRenderer.test.ts
    FlintBridge.test.ts
    hooks/
      useFlintScreen.test.ts
      useFlintTools.test.ts
      useFlintContent.test.ts
      useFlintList.test.ts
      useFlintAction.test.ts
```

---

## Data Model

Mirrors the Compose SDK's `FlintModels.kt` exactly.

```typescript
// types.ts

export type FlintScreenSnapshot = {
  screen: string;
  content: FlintContent;
  overlays: FlintOverlay[];
};

export type FlintContent = {
  elements: FlintElement[];
};

export type FlintElement =
  | FlintContentElement
  | FlintListElement
  | FlintActionElement;

export type FlintContentElement = {
  type: "content";
  key: string;
  value: string;
};

export type FlintListElement = {
  type: "list";
  id: string;
  description: string;
  items: FlintListItem[];
};

export type FlintActionElement = {
  type: "action";
  name: string;
  description: string;
};

export type FlintListItem = {
  index: number;
  content: Record<string, string>;
  actions: FlintAction[];
};

export type FlintAction = {
  name: string;
  description: string;
};

export type FlintOverlay = {
  id: string;
  description: string;
  content: Record<string, string>;
  actions: FlintAction[];
};

// Tool definitions
export type FlintToolDef = {
  name: string;
  description: string;
  params?: FlintToolParam[];
  action: (params: Record<string, any>) => void;
};

export type FlintToolParam = {
  name: string;
  type: "string" | "integer" | "number" | "boolean";
  description: string;
  required?: boolean; // default true
};

// Schema output (MCP-compatible)
export type FlintSchema = {
  protocol: "flint";
  version: "2.0";
  tools: FlintToolSchema[];
};

export type FlintToolSchema = {
  name: string;
  description: string;
  inputSchema: {
    type: "object";
    properties: Record<string, { type: string; description: string }>;
    required: string[];
  };
};
```

---

## Core: FlintRegistry

Central state store. All hooks write to it. The bridge reads from it.

```typescript
// FlintRegistry.ts

import {
  FlintToolDef,
  FlintContentElement,
  FlintListElement,
  FlintActionElement,
  FlintListItem,
  FlintAction,
  FlintOverlay,
  FlintScreenSnapshot,
  FlintContent,
  FlintElement,
} from "./types";

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

type OverlayEntry = {
  id: string;
  description: string;
  content: Map<string, string>;
  actions: FlintAction[];
};

export class FlintRegistry {
  private _screen: string | null = null;
  private _tools: Map<string, FlintToolDef> = new Map();
  private _content: Map<string, ContentEntry> = new Map();
  private _lists: Map<string, ListEntry> = new Map();
  private _actions: Map<string, ActionEntry> = new Map();
  private _overlays: Map<string, OverlayEntry> = new Map();
  private _idCounter = 0;

  // -- Screen --

  setScreen(name: string): void {
    this._screen = name;
  }

  clearScreen(name: string): void {
    if (this._screen === name) {
      this._screen = null;
      this.clearScreenState();
    }
  }

  getScreen(): string | null {
    return this._screen;
  }

  // -- Tools --

  registerTools(tools: FlintToolDef[]): () => void {
    for (const tool of tools) {
      this._tools.set(tool.name, tool);
    }
    return () => {
      for (const tool of tools) {
        this._tools.delete(tool.name);
      }
    };
  }

  getTools(): FlintToolDef[] {
    return Array.from(this._tools.values());
  }

  getToolNames(): string[] {
    return Array.from(this._tools.keys());
  }

  callTool(name: string, params: Record<string, any>): boolean {
    const tool = this._tools.get(name);
    if (!tool) return false;
    tool.action(params);
    return true;
  }

  // -- Content --

  registerContent(entry: ContentEntry): () => void {
    const id = `content_${this._idCounter++}`;
    this._content.set(id, entry);
    return () => { this._content.delete(id); };
  }

  // -- Lists --

  registerList(entry: ListEntry): () => void {
    this._lists.set(entry.id, entry);
    return () => { this._lists.delete(entry.id); };
  }

  // -- Actions --

  registerAction(entry: ActionEntry): () => void {
    const id = `action_${this._idCounter++}`;
    this._actions.set(id, entry);
    return () => { this._actions.delete(id); };
  }

  invokeAction(name: string, listId?: string, itemIndex?: number): boolean {
    for (const entry of this._actions.values()) {
      if (entry.name !== name) continue;
      if (listId != null && entry.listId !== listId) continue;
      if (itemIndex != null && entry.itemIndex !== itemIndex) continue;
      entry.handler();
      return true;
    }
    return false;
  }

  // -- Overlays --

  registerOverlay(entry: OverlayEntry): () => void {
    this._overlays.set(entry.id, entry);
    return () => { this._overlays.delete(entry.id); };
  }

  // -- Snapshot --

  snapshot(): FlintScreenSnapshot {
    const screen = this._screen ?? "unknown";
    const elements: FlintElement[] = [];

    // Top-level content (not in any list)
    for (const entry of this._content.values()) {
      if (!entry.listId) {
        elements.push({ type: "content", key: entry.key, value: entry.value });
      }
    }

    // Top-level actions (not in any list)
    for (const entry of this._actions.values()) {
      if (!entry.listId) {
        elements.push({ type: "action", name: entry.name, description: entry.description });
      }
    }

    // Lists with their items
    for (const list of this._lists.values()) {
      const itemMap = new Map<number, { content: Record<string, string>; actions: FlintAction[] }>();

      for (const entry of this._content.values()) {
        if (entry.listId !== list.id) continue;
        const idx = entry.itemIndex ?? 0;
        if (!itemMap.has(idx)) itemMap.set(idx, { content: {}, actions: [] });
        itemMap.get(idx)!.content[entry.key] = entry.value;
      }

      for (const entry of this._actions.values()) {
        if (entry.listId !== list.id) continue;
        const idx = entry.itemIndex ?? 0;
        if (!itemMap.has(idx)) itemMap.set(idx, { content: {}, actions: [] });
        itemMap.get(idx)!.actions.push({ name: entry.name, description: entry.description });
      }

      const items: FlintListItem[] = Array.from(itemMap.entries())
        .sort(([a], [b]) => a - b)
        .map(([index, data]) => ({ index, content: data.content, actions: data.actions }));

      elements.push({ type: "list", id: list.id, description: list.description, items });
    }

    // Overlays
    const overlays: FlintOverlay[] = Array.from(this._overlays.values()).map((o) => ({
      id: o.id,
      description: o.description,
      content: Object.fromEntries(o.content),
      actions: o.actions,
    }));

    return { screen, content: { elements }, overlays };
  }

  // -- Clear --

  clearScreenState(): void {
    this._content.clear();
    this._lists.clear();
    this._actions.clear();
    this._overlays.clear();
    this._idCounter = 0;
  }

  clearAll(): void {
    this._screen = null;
    this._tools.clear();
    this.clearScreenState();
  }
}
```

---

## FlintTextRenderer

Direct port of `FlintTextRenderer.kt`. Produces identical output.

```typescript
// FlintTextRenderer.ts

import { FlintScreenSnapshot } from "./types";

export function renderSnapshot(snapshot: FlintScreenSnapshot, toolNames: string[]): string {
  const lines: string[] = [];
  lines.push(`screen: ${snapshot.screen}`);

  for (const el of snapshot.content.elements) {
    switch (el.type) {
      case "content":
        lines.push(`${el.key}: ${el.value}`);
        break;
      case "list":
        lines.push(`${el.id}:`);
        for (const item of el.items) {
          const parts = Object.entries(item.content)
            .map(([k, v]) => `${k}: ${v}`)
            .join(" | ");
          lines.push(`  [${item.index}] ${parts}`);
        }
        break;
      case "action":
        // Actions covered by tools list
        break;
    }
  }

  for (const overlay of snapshot.overlays) {
    lines.push(`overlay(${overlay.id}):`);
    for (const [key, value] of Object.entries(overlay.content)) {
      lines.push(`  ${key}: ${value}`);
    }
    if (overlay.actions.length > 0) {
      lines.push(`  actions: ${overlay.actions.map((a) => a.name).join(", ")}`);
    }
  }

  if (toolNames.length > 0) {
    lines.push(`tools: ${toolNames.join(", ")}`);
  }

  return lines.join("\n");
}
```

**Output example (identical to Compose SDK):**
```
screen: search_results
query: jazz
results:
  [0] title: Blue Train | artist: John Coltrane | duration: 10:42
  [1] title: Kind of Blue | artist: Miles Davis | duration: 9:22
tools: go_back
```

---

## Schema Generator

Produces MCP-compatible tool schema. Same format as Compose SDK's `liveSchema()`.

```typescript
// FlintSchema.ts

import { FlintRegistry } from "./FlintRegistry";
import { FlintSchema } from "./types";

export function generateSchema(registry: FlintRegistry): FlintSchema {
  return {
    protocol: "flint",
    version: "2.0",
    tools: registry.getTools().map((tool) => ({
      name: tool.name,
      description: tool.description,
      inputSchema: {
        type: "object" as const,
        properties: Object.fromEntries(
          (tool.params ?? []).map((p) => [
            p.name,
            { type: p.type, description: p.description },
          ])
        ),
        required: (tool.params ?? [])
          .filter((p) => p.required !== false)
          .map((p) => p.name),
      },
    })),
  };
}
```

---

## FlintBridge (CDP interface)

Exposes the registry on `global.__flint__` in dev mode. This is the surface the LLM touches.

```typescript
// FlintBridge.ts

import { FlintRegistry } from "./FlintRegistry";
import { renderSnapshot } from "./FlintTextRenderer";
import { generateSchema } from "./FlintSchema";

export function exposeBridge(registry: FlintRegistry): () => void {
  if (!__DEV__) return () => {};

  const bridge = {
    readScreen(): string {
      const snapshot = registry.snapshot();
      const toolNames = registry.getToolNames();
      return renderSnapshot(snapshot, toolNames);
    },

    getScreen(): string {
      return registry.getScreen() ?? "";
    },

    getSchema(): string {
      return JSON.stringify(generateSchema(registry));
    },

    callTool(name: string, params?: Record<string, any>): string {
      const ok = registry.callTool(name, params ?? {});
      return ok ? "ok" : "error: unknown tool";
    },

    invokeAction(name: string, listId?: string, itemIndex?: number): string {
      const ok = registry.invokeAction(name, listId, itemIndex);
      return ok ? "ok" : "error: action not found";
    },
  };

  (globalThis as any).__flint__ = bridge;

  return () => {
    delete (globalThis as any).__flint__;
  };
}
```

Every function returns a string. No objects, no Promises, no complexity. `Runtime.evaluate` gets a string back every time.

---

## React Hooks

### useFlintScreen

```typescript
// hooks/useFlintScreen.ts

import { useEffect } from "react";
import { useFlintRegistry } from "../FlintProvider";

export function useFlintScreen(name: string): void {
  const registry = useFlintRegistry();

  useEffect(() => {
    registry.setScreen(name);
    return () => registry.clearScreen(name);
  }, [name, registry]);
}
```

### useFlintTools

```typescript
// hooks/useFlintTools.ts

import { useEffect, useRef } from "react";
import { useFlintRegistry } from "../FlintProvider";
import { FlintToolDef } from "../types";

export function useFlintTools(tools: FlintToolDef[]): void {
  const registry = useFlintRegistry();
  const toolsRef = useRef(tools);
  toolsRef.current = tools;

  useEffect(() => {
    const cleanup = registry.registerTools(toolsRef.current);
    return cleanup;
  }, [registry]);
}
```

### flintContent (spread function)

```typescript
// hooks/useFlintContent.ts

import { useEffect } from "react";
import { useFlintRegistry } from "../FlintProvider";

export function useFlintContent(
  key: string,
  value: string,
  listId?: string,
  itemIndex?: number
): void {
  const registry = useFlintRegistry();

  useEffect(() => {
    const cleanup = registry.registerContent({ key, value, listId, itemIndex });
    return cleanup;
  }, [key, value, listId, itemIndex, registry]);
}
```

But hooks can't be called conditionally and must be at the top of a component. For inline annotation, provide a component:

```typescript
// FlintText.tsx

import React, { useEffect } from "react";
import { Text, TextProps } from "react-native";
import { useFlintRegistry } from "./FlintProvider";
import { useFlintItemContext } from "./FlintItemContext";

type FlintTextProps = TextProps & {
  flintKey: string;
  children: string;
};

export function FlintText({ flintKey, children, ...textProps }: FlintTextProps) {
  const registry = useFlintRegistry();
  const itemCtx = useFlintItemContext();

  useEffect(() => {
    return registry.registerContent({
      key: flintKey,
      value: children,
      listId: itemCtx?.listId,
      itemIndex: itemCtx?.index,
    });
  }, [flintKey, children, itemCtx, registry]);

  return <Text {...textProps}>{children}</Text>;
}
```

### FlintItem (wrapper component)

Provides list/item context to children. The one wrapper in the API.

```typescript
// FlintItemContext.tsx

import React, { createContext, useContext } from "react";

type FlintItemContextValue = { listId: string; index: number };

const FlintItemCtx = createContext<FlintItemContextValue | null>(null);

export function useFlintItemContext(): FlintItemContextValue | null {
  return useContext(FlintItemCtx);
}

export function FlintItem({
  list,
  index,
  children,
}: {
  list: string;
  index: number;
  children: React.ReactNode;
}) {
  return (
    <FlintItemCtx.Provider value={{ listId: list, index }}>
      {children}
    </FlintItemCtx.Provider>
  );
}
```

### useFlintList

```typescript
// hooks/useFlintList.ts

import { useEffect } from "react";
import { useFlintRegistry } from "../FlintProvider";

export function useFlintList(id: string, description?: string): void {
  const registry = useFlintRegistry();

  useEffect(() => {
    const cleanup = registry.registerList({ id, description: description ?? id });
    return cleanup;
  }, [id, description, registry]);
}
```

### FlintAction

```typescript
// FlintAction.tsx

import React, { useEffect } from "react";
import { Pressable, PressableProps } from "react-native";
import { useFlintRegistry } from "./FlintProvider";
import { useFlintItemContext } from "./FlintItemContext";

type FlintActionProps = PressableProps & {
  flintName: string;
  flintDescription?: string;
};

export function FlintAction({
  flintName,
  flintDescription,
  onPress,
  children,
  ...pressableProps
}: FlintActionProps) {
  const registry = useFlintRegistry();
  const itemCtx = useFlintItemContext();

  useEffect(() => {
    return registry.registerAction({
      name: flintName,
      description: flintDescription ?? flintName,
      handler: () => {
        if (onPress) {
          // Simulate a press event
          onPress(null as any);
        }
      },
      listId: itemCtx?.listId,
      itemIndex: itemCtx?.index,
    });
  }, [flintName, flintDescription, onPress, itemCtx, registry]);

  return (
    <Pressable onPress={onPress} {...pressableProps}>
      {children}
    </Pressable>
  );
}
```

---

## FlintProvider

Root context. Initializes the registry and the CDP bridge.

```typescript
// FlintProvider.tsx

import React, { createContext, useContext, useState, useEffect } from "react";
import { FlintRegistry } from "./FlintRegistry";
import { exposeBridge } from "./FlintBridge";

const FlintRegistryContext = createContext<FlintRegistry | null>(null);

export function useFlintRegistry(): FlintRegistry {
  const registry = useContext(FlintRegistryContext);
  if (!registry) {
    throw new Error("useFlintRegistry must be used within <FlintProvider>");
  }
  return registry;
}

export function FlintProvider({ children }: { children: React.ReactNode }) {
  const [registry] = useState(() => new FlintRegistry());

  useEffect(() => {
    const cleanup = exposeBridge(registry);
    return cleanup;
  }, [registry]);

  return (
    <FlintRegistryContext.Provider value={registry}>
      {children}
    </FlintRegistryContext.Provider>
  );
}
```

---

## Navigation Integration

### React Navigation

```typescript
// navigation/FlintNavigationContainer.tsx

import React, { useRef } from "react";
import {
  NavigationContainer,
  NavigationContainerProps,
  useNavigationContainerRef,
} from "@react-navigation/native";
import { useFlintRegistry } from "../FlintProvider";

export function FlintNavigationContainer({
  children,
  ...props
}: NavigationContainerProps & { children: React.ReactNode }) {
  const navigationRef = useNavigationContainerRef();
  const routeNameRef = useRef<string | undefined>();
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

### Expo Router

```typescript
// navigation/useFlintExpoRouter.ts

import { useEffect } from "react";
import { usePathname } from "expo-router";
import { useFlintRegistry } from "../FlintProvider";

export function useFlintExpoRouter(): void {
  const pathname = usePathname();
  const registry = useFlintRegistry();

  useEffect(() => {
    registry.clearScreenState();
    registry.setScreen(pathname);
  }, [pathname, registry]);
}
```

---

## Developer Integration

### App setup (2 lines)

```tsx
// App.tsx
import { FlintProvider, FlintNavigationContainer } from "flint-react-native";

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

### Screen with tools and content

```tsx
// screens/HomeScreen.tsx
import { useFlintTools, useFlintList, FlintText, FlintItem, FlintAction } from "flint-react-native";

export function HomeScreen({ navigation }) {
  useFlintTools([
    {
      name: "search",
      description: "Search for tracks",
      params: [{ name: "query", type: "string", description: "Search query" }],
      action: ({ query }) => navigation.navigate("SearchResults", { query }),
    },
  ]);

  useFlintList("playlists", "Featured playlists");

  return (
    <FlatList
      data={playlists}
      renderItem={({ item, index }) => (
        <FlintItem list="playlists" index={index}>
          <FlintText flintKey="name" style={styles.title}>{item.name}</FlintText>
          <FlintText flintKey="count" style={styles.subtitle}>
            {`${item.trackCount} tracks`}
          </FlintText>
          <FlintAction
            flintName="open"
            flintDescription="Open playlist"
            onPress={() => navigation.navigate("Playlist", { id: item.id })}
          >
            <Text>Open</Text>
          </FlintAction>
        </FlintItem>
      )}
    />
  );
}
```

### Compose SDK comparison (same screen)

```kotlin
// Compose SDK equivalent
Flint.screen("home")
Flint.tools {
    tool("search", "Search for tracks") {
        param("query", "string", "Search query")
        action { params -> onSearch(params["query"] as String); null }
    }
}

LazyColumn(modifier = Modifier.flintList("playlists", "Featured playlists")) {
    itemsIndexed(playlists) { index, playlist ->
        Card(modifier = Modifier.flintItem(index).flintAction("open", "Open playlist")) {
            Text(playlist.name, modifier = Modifier.flintContent("name"))
            Text("${playlist.trackCount} tracks", modifier = Modifier.flintContent("count"))
        }
    }
}
```

### Boilerplate comparison

| Aspect | Compose SDK | RN SDK |
|---|---|---|
| App setup | `Flint.init(this)` (1 line) | `<FlintProvider>` + `<FlintNavigationContainer>` (2 wrappers) |
| Screen tracking | `Flint.screen("home")` (1 line) | Automatic via nav wrapper (0 lines) |
| Tool definition | `Flint.tools { tool(...) }` | `useFlintTools([...])` |
| Content annotation | `Modifier.flintContent("key")` | `<FlintText flintKey="key">` |
| List annotation | `Modifier.flintList("id")` | `useFlintList("id")` |
| Item annotation | `Modifier.flintItem(index)` | `<FlintItem list="id" index={n}>` |
| Action annotation | `Modifier.flintAction("name")` | `<FlintAction flintName="name">` |

Roughly equal code. RN uses components where Compose uses modifiers. Neither is more verbose.

---

## LLM Interaction

### From Claude Code or any LLM agent

The LLM needs a thin CLI wrapper or MCP tool that:

1. Discovers the Metro CDP WebSocket URL
2. Sends `Runtime.evaluate` with a Flint expression
3. Returns the result string

```bash
# Conceptual CLI usage
flint read           # -> screen: home\nplaylists:\n  [0] name: Jazz...
flint schema         # -> { "protocol": "flint", "version": "2.0", "tools": [...] }
flint call search --query jazz    # -> ok
flint read           # -> screen: search_results\nresults:\n  [0] title: Blue Train...
```

### As an MCP server

A small Node.js MCP server wraps the CDP calls:

```typescript
// flint-mcp-server (separate package, ~100 lines)

server.tool("flint_read_screen", "Read the current screen state", {}, async () => {
  const result = await cdp.evaluate("global.__flint__.readScreen()");
  return { content: [{ type: "text", text: result }] };
});

server.tool("flint_call_tool", "Call a registered tool", { name: "string", params: "object" }, async ({ name, params }) => {
  await cdp.evaluate(`global.__flint__.callTool("${name}", ${JSON.stringify(params)})`);
  await sleep(400);
  const screen = await cdp.evaluate("global.__flint__.readScreen()");
  return { content: [{ type: "text", text: screen }] };
});

server.tool("flint_get_schema", "Get available tools", {}, async () => {
  const schema = await cdp.evaluate("global.__flint__.getSchema()");
  return { content: [{ type: "text", text: schema }] };
});
```

The LLM sees three MCP tools. Calls them like any other tool. Gets flat text back.

---

## Test Strategy (TDD)

### Unit Tests (Jest, no device needed)

Every module tested in isolation. No React Native runtime required for core logic.

#### FlintRegistry tests

```
- registers and retrieves screen name
- clears screen only if name matches
- registers and retrieves tools
- callTool returns false for unknown tool
- callTool invokes action with params
- registerContent returns cleanup function
- cleanup function removes content
- registerList + registerContent builds correct snapshot
- list items sorted by index
- actions with listId/itemIndex grouped correctly
- invokeAction matches by name only
- invokeAction matches by name + listId + itemIndex
- invokeAction returns false when not found
- clearScreenState preserves tools
- clearAll removes everything
- snapshot returns correct structure for empty state
- snapshot returns correct structure for complex state
```

#### FlintTextRenderer tests

```
- renders screen name
- renders content elements
- renders list with items
- renders item content joined with pipe separator
- renders overlays with content and actions
- renders tool names
- matches Compose SDK output exactly (golden tests)
- handles empty snapshot
- handles snapshot with no tools
```

#### FlintSchema tests

```
- generates correct protocol and version
- generates tool with no params
- generates tool with required params
- generates tool with optional params
- generates multiple tools
```

#### FlintBridge tests

```
- exposes readScreen on global.__flint__
- exposes getScreen on global.__flint__
- exposes getSchema on global.__flint__
- exposes callTool on global.__flint__
- exposes invokeAction on global.__flint__
- readScreen returns string
- callTool returns "ok" for valid tool
- callTool returns "error: unknown tool" for invalid tool
- invokeAction returns "ok" for valid action
- invokeAction returns "error: action not found" for invalid action
- cleanup removes global.__flint__
- does not expose in production (__DEV__ = false)
```

### Component Tests (React Testing Library)

```
- FlintProvider provides registry to children
- useFlintScreen sets screen name on mount
- useFlintScreen clears screen on unmount
- useFlintTools registers tools on mount
- useFlintTools cleans up on unmount
- FlintText registers content with key and value
- FlintText registers with list context when inside FlintItem
- FlintText cleans up on unmount
- FlintItem provides listId and index to children
- FlintAction registers action handler
- FlintAction invokes onPress when action triggered
- useFlintList registers list entry
- screen change via navigation clears previous screen state
```

### Golden Tests (output parity with Compose SDK)

Hardcoded snapshot inputs, verified against Compose SDK's `FlintTextRenderer` output:

```typescript
test("home screen output matches Compose SDK", () => {
  const snapshot: FlintScreenSnapshot = {
    screen: "home",
    content: {
      elements: [
        {
          type: "list",
          id: "playlists",
          description: "Featured playlists",
          items: [
            { index: 0, content: { name: "Jazz Essentials", description: "Best jazz tracks" }, actions: [{ name: "select", description: "Open playlist" }] },
            { index: 1, content: { name: "Rock Classics", description: "Classic rock hits" }, actions: [{ name: "select", description: "Open playlist" }] },
          ],
        },
      ],
    },
    overlays: [],
  };

  const output = renderSnapshot(snapshot, ["search", "open_playlist"]);

  expect(output).toBe(
    "screen: home\n" +
    "playlists:\n" +
    "  [0] name: Jazz Essentials | description: Best jazz tracks\n" +
    "  [1] name: Rock Classics | description: Classic rock hits\n" +
    "tools: search, open_playlist"
  );
});
```

### Integration Tests (on device/emulator)

```
- FlintProvider + FlintNavigationContainer renders without error
- global.__flint__.readScreen() returns valid text from running app
- global.__flint__.getSchema() returns valid JSON
- global.__flint__.callTool() triggers navigation
- readScreen after callTool shows new screen (with delay)
- screen tracking updates on navigation
- list items visible in snapshot
- actions invocable via invokeAction
```

### CDP Integration Tests (end-to-end)

```
- connects to Metro CDP WebSocket
- discovers Hermes target
- Runtime.evaluate returns readScreen output
- Runtime.evaluate returns getSchema output
- Runtime.evaluate callTool + readScreen flow works
```

---

## Build Order

### Phase 1: Core + Tests (pure TypeScript, no RN runtime)

1. Write `types.ts`
2. TDD `FlintRegistry` (write tests first, then implementation)
3. TDD `FlintTextRenderer` (golden tests against Compose SDK output)
4. TDD `FlintSchema` generator
5. TDD `FlintBridge`

All testable with Jest. No React, no RN, no device.

### Phase 2: Hooks + Components (React Testing Library)

1. `FlintProvider` + `useFlintRegistry`
2. TDD `useFlintScreen`
3. TDD `useFlintTools`
4. TDD `FlintText`
5. TDD `FlintItem` + `FlintItemContext`
6. TDD `FlintAction`
7. TDD `useFlintList`

### Phase 3: Navigation Integration

1. `FlintNavigationContainer` (React Navigation)
2. `useFlintExpoRouter` (Expo Router)

### Phase 4: Sample App + CDP Validation

1. Create basic RN sample app (3-4 screens)
2. Verify `global.__flint__` works via Metro debugger
3. Build thin CDP CLI tool
4. End-to-end test: CLI reads screen, calls tool, reads new screen

---

## Package Structure

```json
{
  "name": "flint-react-native",
  "version": "0.1.0",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "peerDependencies": {
    "react": ">=18.0.0",
    "react-native": ">=0.72.0"
  },
  "optionalPeerDependencies": {
    "@react-navigation/native": ">=6.0.0",
    "expo-router": ">=3.0.0"
  },
  "devDependencies": {
    "jest": "^29.0.0",
    "@testing-library/react-native": "^12.0.0",
    "typescript": "^5.0.0"
  }
}
```

### Public API (index.ts)

```typescript
// Core
export { FlintProvider } from "./FlintProvider";
export { FlintRegistry } from "./FlintRegistry";

// Hooks
export { useFlintScreen } from "./hooks/useFlintScreen";
export { useFlintTools } from "./hooks/useFlintTools";
export { useFlintList } from "./hooks/useFlintList";

// Components
export { FlintText } from "./FlintText";
export { FlintItem } from "./FlintItemContext";
export { FlintAction } from "./FlintAction";

// Navigation
export { FlintNavigationContainer } from "./navigation/FlintNavigationContainer";
export { useFlintExpoRouter } from "./navigation/useFlintExpoRouter";

// Types
export type {
  FlintToolDef,
  FlintToolParam,
  FlintScreenSnapshot,
  FlintSchema,
} from "./types";
```

---

## What the LLM sees

Same output regardless of whether the app is Compose or React Native:

```
screen: home
playlists:
  [0] name: Jazz Essentials | description: Best jazz tracks
  [1] name: Rock Classics | description: Classic rock hits
tools: search, open_playlist
```

The AI doesn't know or care what framework rendered it. One format. One protocol. Every platform.
