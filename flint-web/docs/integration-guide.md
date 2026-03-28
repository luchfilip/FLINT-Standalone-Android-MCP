# Flint Web SDK. Integration Guide

Flint exposes your web app's UI as structured, semantic data that an LLM can read and act on. No vision model, no coordinate guessing. The LLM sees named screens, labeled content, and callable actions.

## Local install

Both `flint-web` and `flint-core` are local packages. Add them as file dependencies.

### 1. Add dependencies

```json
{
  "dependencies": {
    "flint-web": "file:../path/to/flint-web",
    "flint-core": "file:../path/to/flint-core"
  }
}
```

Then run `npm install` or `yarn`.

### 2. Configure module aliases

Your bundler needs to resolve `flint-web` and `flint-core` to their `src/` directories.

**Vite** (`vite.config.ts`):

```ts
import path from "path";
import { defineConfig } from "vite";

export default defineConfig({
  resolve: {
    alias: {
      "flint-web": path.resolve(__dirname, "../path/to/flint-web/src"),
      "flint-core": path.resolve(__dirname, "../path/to/flint-core/src"),
    },
  },
});
```

**Webpack / CRA** (`webpack.config.js` or `craco.config.js`):

```js
const path = require("path");

module.exports = {
  resolve: {
    alias: {
      "flint-web": path.resolve(__dirname, "../path/to/flint-web/src"),
      "flint-core": path.resolve(__dirname, "../path/to/flint-core/src"),
    },
  },
};
```

Adjust the relative paths to match your folder structure.

---

## Setup

### Wrap your app in FlintProvider

`FlintProvider` creates the registry and exposes the `window.__flint__` bridge.

```tsx
import { FlintProvider } from "flint-web";

export default function App() {
  return (
    <FlintProvider>
      {/* your app */}
    </FlintProvider>
  );
}
```

### Add FlintRouter (react-router-dom)

Place `FlintRouter` inside your `BrowserRouter` or `MemoryRouter`. It tracks the current route and auto-derives a screen name from the pathname.

```tsx
import { BrowserRouter } from "react-router-dom";
import { FlintProvider, FlintRouter } from "flint-web";

export default function App() {
  return (
    <FlintProvider>
      <BrowserRouter>
        <FlintRouter>
          {/* your routes */}
        </FlintRouter>
      </BrowserRouter>
    </FlintProvider>
  );
}
```

**Auto-derived screen names:**

| Pathname | Screen name |
|----------|-------------|
| `/` | `home` |
| `/search/results` | `search_results` |
| `/settings` | `settings` |

**Custom mappings** via `screenNameMap`:

```tsx
<FlintRouter screenNameMap={{ "/": "dashboard", "/u/me": "profile" }}>
  {/* routes */}
</FlintRouter>
```

---

## Annotating screens

### useFlintScreen

Explicitly sets the screen name. This overrides FlintRouter's auto-derived name for the component where it's called.

```tsx
import { useFlintScreen } from "flint-web";

function SettingsPage() {
  useFlintScreen("settings");
  // ...
}
```

### FlintText

Wraps a `<span>`. Registers a key/value pair in the semantic layer.

```tsx
import { FlintText } from "flint-web";

<FlintText flintKey="title" className="heading">
  Shopping Cart
</FlintText>

<FlintText flintKey="itemCount">
  {`${items.length} items`}
</FlintText>
```

The LLM sees:

```
title: Shopping Cart
itemCount: 3 items
```

`FlintText` renders a normal `<span>` and accepts all span attributes.

### FlintAction

Wraps a `<button>`. Registers an invocable action.

```tsx
import { FlintAction } from "flint-web";

<FlintAction
  flintName="checkout"
  flintDescription="Proceed to checkout"
  onClick={handleCheckout}
>
  Checkout
</FlintAction>
```

The LLM sees:

```
actions: checkout (Proceed to checkout)
```

`FlintAction` renders a normal `<button>` and accepts all button attributes.

### Lists with FlintItem

For repeated data, wrap each item in `FlintItem` and use `FlintText`/`FlintAction` inside.

```tsx
import { useFlintList, FlintItem, FlintText, FlintAction } from "flint-web";

function CartPage() {
  useFlintScreen("cart");
  useFlintList("items", "Items in the shopping cart");

  return (
    <ul>
      {cartItems.map((item, index) => (
        <FlintItem list="items" index={index} key={item.id}>
          <li>
            <FlintText flintKey="name">{item.name}</FlintText>
            <FlintText flintKey="price">{`$${item.price}`}</FlintText>
            <FlintAction
              flintName="remove"
              flintDescription="Remove item from cart"
              onClick={() => removeItem(item.id)}
            >
              Remove
            </FlintAction>
          </li>
        </FlintItem>
      ))}
    </ul>
  );
}
```

The LLM sees:

```
screen: cart
items:
  [0] name: Blue Shirt | price: $29.99
  [1] name: Red Hat | price: $14.99
```

`useFlintList(id, description)` registers the list. `FlintItem` provides context so nested components know which list and index they belong to.

### useFlintTools

Registers screen-level tools for operations not tied to a specific element. Search, navigation, refresh, etc.

```tsx
import { useFlintTools } from "flint-web";

function HomePage() {
  useFlintScreen("home");

  useFlintTools([
    {
      name: "search",
      description: "Search products by name",
      params: [
        { name: "query", type: "string", description: "Search query", required: true },
      ],
      action: (params) => navigate(`/search?q=${params.query}`),
    },
    {
      name: "refresh",
      description: "Reload the product list",
      action: () => fetchProducts(),
    },
  ]);

  // ...
}
```

---

## Reading from the browser

Flint exposes `window.__flint__` with these methods:

| Method | Returns | Description |
|--------|---------|-------------|
| `readScreen()` | `string` | Human-readable snapshot of the current screen |
| `getSchema()` | `string` (JSON) | MCP-compatible tool schema |
| `callTool(name, params)` | `"ok"` or `"error: ..."` | Execute a tool by name |
| `invokeAction(name, listId?, itemIndex?)` | `"ok"` or `"error: ..."` | Invoke an action |

### Dev console

```js
// Read the current screen
window.__flint__.readScreen()

// Get the MCP tool schema
window.__flint__.getSchema()

// Call a tool
window.__flint__.callTool("search", { query: "shoes" })

// Invoke a standalone action
window.__flint__.invokeAction("checkout")

// Invoke an action on a list item
window.__flint__.invokeAction("remove", "items", 0)
```

### Playwright

```ts
const screen = await page.evaluate(() => window.__flint__.readScreen());
const schema = await page.evaluate(() => window.__flint__.getSchema());
await page.evaluate(() => window.__flint__.callTool("search", { query: "shoes" }));
```

---

## Full example

```tsx
import React, { useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import {
  FlintProvider,
  FlintRouter,
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintItem,
  FlintText,
  FlintAction,
} from "flint-web";

function TodoPage() {
  useFlintScreen("todos");
  useFlintList("tasks", "List of tasks");

  const [tasks, setTasks] = useState([
    { id: 1, title: "Buy groceries", done: false },
    { id: 2, title: "Walk the dog", done: true },
    { id: 3, title: "Write tests", done: false },
  ]);

  useFlintTools([
    {
      name: "add_task",
      description: "Add a new task",
      params: [
        { name: "title", type: "string", description: "Task title", required: true },
      ],
      action: (params) => {
        setTasks((prev) => [...prev, { id: Date.now(), title: params.title, done: false }]);
      },
    },
  ]);

  const toggle = (id: number) =>
    setTasks((prev) => prev.map((t) => (t.id === id ? { ...t, done: !t.done } : t)));

  return (
    <ul>
      {tasks.map((task, index) => (
        <FlintItem list="tasks" index={index} key={task.id}>
          <li>
            <FlintText flintKey="title">{task.title}</FlintText>
            <FlintText flintKey="status">{task.done ? "done" : "pending"}</FlintText>
            <FlintAction
              flintName="toggle"
              flintDescription="Mark task as done or pending"
              onClick={() => toggle(task.id)}
            >
              {task.done ? "Undo" : "Done"}
            </FlintAction>
          </li>
        </FlintItem>
      ))}
    </ul>
  );
}

export default function App() {
  return (
    <FlintProvider>
      <BrowserRouter>
        <FlintRouter>
          <Routes>
            <Route path="/" element={<TodoPage />} />
          </Routes>
        </FlintRouter>
      </BrowserRouter>
    </FlintProvider>
  );
}
```

LLM output:

```
screen: todos
tasks:
  [0] title: Buy groceries | status: pending
  [1] title: Walk the dog | status: done
  [2] title: Write tests | status: pending
tools: add_task
```

---

## API reference

### Components

| Component | Props | Description |
|-----------|-------|-------------|
| `FlintProvider` | `children` | Root provider. Wrap your app in this. |
| `FlintRouter` | `children`, `screenNameMap?: Record<string, string>` | Tracks react-router location. Auto-derives screen names from pathname. |
| `FlintText` | `flintKey: string`, `children: string`, + all `HTMLSpanElement` attrs | Registers a key/value pair. Renders a `<span>`. |
| `FlintItem` | `list: string`, `index: number`, `children` | Context provider for list items. |
| `FlintAction` | `flintName: string`, `flintDescription?: string`, `onClick`, + all `HTMLButtonElement` attrs | Registers an invocable action. Renders a `<button>`. |

### Hooks

| Hook | Signature | Description |
|------|-----------|-------------|
| `useFlintScreen` | `(name: string, routeName?: string) => void` | Sets the active screen name. Overrides FlintRouter. |
| `useFlintTools` | `(tools: FlintToolDef[]) => void` | Registers screen-level tools. |
| `useFlintList` | `(id: string, description?: string) => void` | Registers a list container. |

### Types

```ts
type FlintToolDef = {
  name: string;
  description: string;
  params?: FlintToolParam[];
  action: (params: Record<string, any>) => void;
};

type FlintToolParam = {
  name: string;
  type: "string" | "integer" | "number" | "boolean";
  description: string;
  required?: boolean;
};
```

---

## Requirements

- React >= 18
- `react-router-dom` >= 6 (optional, for `FlintRouter`)
