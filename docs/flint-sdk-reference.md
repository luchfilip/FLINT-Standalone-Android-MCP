# Flint SDK Reference

Flint gives LLMs structured access to app UI. Instead of parsing screenshots or DOM trees, the LLM reads named screens, labeled content, typed tools, and invocable actions. Works on web (React) and mobile (React Native).

## Packages

| Package | Platform | Wraps |
|---------|----------|-------|
| `flint-core` | Shared | Registry, hooks, bridge, renderer, schema. No platform imports. |
| `flint-web` | React DOM | `<span>`, `<button>`, React Router v6. Re-exports flint-core. |
| `flint-react-native` | React Native | `<Text>`, `<Pressable>`, React Navigation. Re-exports flint-core. |

Import from `flint-web` or `flint-react-native`. Never import `flint-core` directly in app code.

---

## Bridge API

In dev mode, Flint exposes `globalThis.__flint__` (aliased as `window.__flint__` on web). This is the LLM's interface.

### Methods

| Method | Returns | Use |
|--------|---------|-----|
| `readScreen()` | `string` | Full semantic snapshot of the current screen |
| `getScreen()` | `string` | Current screen name only |
| `getSchema()` | `string` (JSON) | MCP-compatible tool definitions with typed params |
| `callTool(name, params?)` | `"ok"` or `"error: unknown tool"` | Execute a screen-level tool |
| `invokeAction(name, listId?, itemIndex?)` | `"ok"` or `"error: action not found"` | Execute an inline action |

### readScreen() output format

```
screen: <screen_name>
<key>: <value>
<key>: <value>
<listId>:
  [0] <key>: <value> | <key>: <value> | actions: <action>, <action>
  [1] <key>: <value> | <key>: <value> | actions: <action>, <action>
actions: <standalone_action>
overlay(<overlayId>):
  <key>: <value>
  actions: <action>, <action>
tools: <tool>, <tool>
```

Real example:

```
screen: home
heading: Sample Store
cartSummary: 0 item(s) in cart
products:
  [0] name: Wireless Headphones | price: $79.99 | category: Electronics | actions: view, add_to_cart
  [1] name: Running Shoes | price: $129.99 | category: Sports | actions: view, add_to_cart
tools: view_product, go_to_cart
```

### getSchema() output format

MCP-compatible JSON. Each tool has `name`, `description`, and `inputSchema` with typed properties.

```json
{
  "protocol": "flint",
  "version": "2.0",
  "tools": [
    {
      "name": "view_product",
      "description": "View product details",
      "inputSchema": {
        "type": "object",
        "properties": {
          "id": { "type": "string", "description": "Product ID" }
        },
        "required": ["id"]
      }
    }
  ]
}
```

---

## Tools vs Actions

**Tools** are screen-level operations registered with `useFlintTools`. They appear on the `tools:` line. Call them with `callTool(name, params)`. They accept typed parameters.

**Actions** are element-level operations registered with `FlintAction` components. They appear inline on list items or as standalone `actions:` lines. Call them with `invokeAction(name)` for standalone, or `invokeAction(name, listId, itemIndex)` for list item actions. They take no parameters.

### When to use which

Read `readScreen()` first. Look at:
- `tools:` line for callable tools with params (check `getSchema()` for param types)
- `actions:` on list items for per-item operations (pass `listId` and `itemIndex`)
- standalone `actions:` for screen-level buttons

### Calling sequence

```
1. readScreen()          → understand what's on screen
2. getSchema()           → get tool param types (if needed)
3. callTool(name, {})    → execute a tool (navigates, fetches, etc.)
4. readScreen()          → verify the result
```

For list item actions:
```
1. readScreen()                              → find the item index
2. invokeAction("remove", "cart_items", 0)   → act on item [0]
3. readScreen()                              → verify
```

---

## Transport

### Web (React)

Access `window.__flint__` directly.

**Browser console:**
```js
window.__flint__.readScreen()
window.__flint__.callTool("search", { query: "shoes" })
```

**Playwright/Puppeteer:**
```js
const screen = await page.evaluate(() => window.__flint__.readScreen());
await page.evaluate(() => window.__flint__.callTool("add_to_cart", {}));
```

### React Native

Access via Chrome DevTools Protocol over Metro's WebSocket.

**Discover target:**
```
GET http://localhost:8081/json
→ [{ "webSocketDebuggerUrl": "ws://localhost:8081/inspector/debug?device=<id>&page=1" }]
```

**Evaluate:**
```json
{
  "id": 1,
  "method": "Runtime.evaluate",
  "params": {
    "expression": "globalThis.__flint__.readScreen()",
    "returnByValue": true
  }
}
```

**CLI shortcut** (included in flint-react-native):
```bash
npx flint-cdp read
npx flint-cdp schema
npx flint-cdp call search --params '{"query":"shoes"}'
npx flint-cdp action remove cart_items 0
```

Important: use a single WebSocket connection for multi-step flows. Creating a new connection after navigation may hit a stale CDP target.

---

## Developer Integration

### Web setup (React + React Router)

```tsx
import { FlintProvider, FlintRouter } from "flint-web";
import { BrowserRouter } from "react-router-dom";

function App() {
  return (
    <FlintProvider>
      <BrowserRouter>
        <FlintRouter>
          <Routes>{/* ... */}</Routes>
        </FlintRouter>
      </BrowserRouter>
    </FlintProvider>
  );
}
```

`FlintRouter` auto-derives screen names from the URL pathname. `/search/results` becomes `search_results`. `/` becomes `home`. Override with `screenNameMap`:

```tsx
<FlintRouter screenNameMap={{ "/dashboard": "main_dash", "/": "landing" }}>
```

### React Native setup (React Navigation)

```tsx
import { FlintProvider, FlintNavigationContainer } from "flint-react-native";

function App() {
  return (
    <FlintProvider>
      <FlintNavigationContainer>
        <Stack.Navigator>{/* ... */}</Stack.Navigator>
      </FlintNavigationContainer>
    </FlintProvider>
  );
}
```

### Annotating a screen

```tsx
// Web: import from "flint-web"
// RN:  import from "flint-react-native"
import {
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintText,
  FlintAction,
  FlintItem,
} from "flint-web";

function CartScreen() {
  // Name the screen. Second arg is the React Navigation route name (RN only).
  useFlintScreen("cart");

  // Register a list container
  useFlintList("items", "Items in the cart");

  // Register screen-level tools
  useFlintTools([
    {
      name: "go_back",
      description: "Return to store",
      action: () => navigate("/"),
    },
    {
      name: "checkout",
      description: "Proceed to checkout",
      params: [
        { name: "coupon", type: "string", description: "Coupon code", required: false },
      ],
      action: (params) => startCheckout(params.coupon),
    },
  ]);

  return (
    <div>
      {/* Key/value content */}
      <FlintText flintKey="heading">Shopping Cart</FlintText>
      <FlintText flintKey="total">{`$${total}`}</FlintText>

      {/* List with per-item actions */}
      {items.map((item, i) => (
        <FlintItem list="items" index={i} key={item.id}>
          <FlintText flintKey="name">{item.name}</FlintText>
          <FlintText flintKey="price">{`$${item.price}`}</FlintText>
          <FlintAction
            flintName="remove"
            flintDescription="Remove from cart"
            onClick={() => removeItem(item.id)}  // web: onClick, RN: onPress
          >
            Remove
          </FlintAction>
        </FlintItem>
      ))}
    </div>
  );
}
```

LLM sees:

```
screen: cart
heading: Shopping Cart
total: $94.98
items:
  [0] name: Wireless Headphones | price: $79.99 | actions: remove
  [1] name: Coffee Maker | price: $14.99 | actions: remove
tools: go_back, checkout
```

---

## API Quick Reference

### Hooks

| Hook | Signature | Purpose |
|------|-----------|---------|
| `useFlintScreen` | `(name: string, routeName?: string)` | Name the active screen |
| `useFlintTools` | `(tools: FlintToolDef[])` | Register callable tools |
| `useFlintList` | `(id: string, description?: string)` | Register a list container |

### Components

| Component | Key Props | Renders |
|-----------|-----------|---------|
| `FlintProvider` | `children` | Root context. Wrap your app. |
| `FlintText` | `flintKey`, `children: string` | Web: `<span>`. RN: `<Text>`. |
| `FlintAction` | `flintName`, `flintDescription?`, `onClick`/`onPress` | Web: `<button>`. RN: `<Pressable>`. |
| `FlintItem` | `list: string`, `index: number` | Context for list items. |
| `FlintRouter` | `children`, `screenNameMap?` | Web only. React Router v6 screen tracking. |
| `FlintNavigationContainer` | Same as `NavigationContainer` | RN only. React Navigation screen tracking. |

### FlintToolDef

```ts
{
  name: string;
  description: string;
  params?: { name: string; type: "string"|"integer"|"number"|"boolean"; description: string; required?: boolean }[];
  action: (params: Record<string, any>) => void;
}
```

---

## Local Install

Both packages are local (not published to npm).

```json
{
  "dependencies": {
    "flint-web": "file:../../flint-web"
  }
}
```

For Vite, add resolve aliases to avoid duplicate React:

```ts
// vite.config.ts
resolve: {
  alias: {
    "flint-web": path.resolve(__dirname, "../../flint-web/src"),
    "flint-core": path.resolve(__dirname, "../../flint-core/src"),
  },
}
```

For React Native with Metro, add `watchFolders`, `extraNodeModules`, and `blockList` to prevent bundling a second React. See `sample/musicapp-rn/metro.config.js` for the exact config.
