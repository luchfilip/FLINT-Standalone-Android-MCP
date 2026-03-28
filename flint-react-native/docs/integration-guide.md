# Flint React Native SDK. Integration Guide

Flint exposes your app's UI as structured, semantic data that an LLM can read and act on. No vision model, no coordinate guessing. The LLM sees named screens, labeled content, and callable actions.

## Local install

The package isn't published yet. Link it from disk.

### 1. Add the dependency

Point your `package.json` at the local path:

```json
{
  "dependencies": {
    "flint-react-native": "file:../path/to/flint-react-native"
  }
}
```

Then run `npm install` or `yarn`.

### 2. Fix Metro resolution

Metro will bundle a second copy of React from flint's `node_modules`. This causes the "Invalid hook call" crash. Add this to your `metro.config.js`:

```js
const path = require('path');
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');

const flintPath = path.resolve(__dirname, '../path/to/flint-react-native');
const appModules = path.resolve(__dirname, 'node_modules');

const config = {
  watchFolders: [flintPath],
  resolver: {
    nodeModulesPaths: [appModules],
    extraNodeModules: {
      react: path.resolve(appModules, 'react'),
      'react-native': path.resolve(appModules, 'react-native'),
      '@react-navigation/native': path.resolve(appModules, '@react-navigation/native'),
    },
    blockList: [
      new RegExp(path.resolve(flintPath, 'node_modules', 'react') + '/.*'),
      new RegExp(path.resolve(flintPath, 'node_modules', 'react-native') + '/.*'),
    ],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
```

Adjust the relative path to match your folder structure.

### 3. Clear Metro cache after linking

```bash
npx react-native start --reset-cache
```

Do this every time you change the flint source.

---

## Setup

### Wrap your app in FlintProvider

`FlintProvider` creates the registry and exposes the `globalThis.__flint__` bridge in dev mode.

```tsx
import { FlintProvider } from 'flint-react-native';

export default function App() {
  return (
    <FlintProvider>
      {/* your app */}
    </FlintProvider>
  );
}
```

### Add screen tracking (React Navigation)

If you use React Navigation, replace `NavigationContainer` with `FlintNavigationContainer`. It tracks the active route automatically.

```tsx
import { FlintProvider } from 'flint-react-native';
import { FlintNavigationContainer } from 'flint-react-native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <FlintProvider>
      <FlintNavigationContainer>
        <Stack.Navigator>
          <Stack.Screen name="Home" component={HomeScreen} />
          <Stack.Screen name="Details" component={DetailsScreen} />
        </Stack.Navigator>
      </FlintNavigationContainer>
    </FlintProvider>
  );
}
```

### Add screen tracking (Expo Router)

```tsx
import { useFlintExpoRouter } from 'flint-react-native/expo-router';

export default function Layout() {
  useFlintExpoRouter();
  return <Slot />;
}
```

---

## Annotating screens

### useFlintScreen

Names the current screen. Call it at the top of every screen component.

```tsx
import { useFlintScreen } from 'flint-react-native';

export function HomeScreen() {
  useFlintScreen('home', 'Home');
  // ...
}
```

The first argument is the Flint screen name (what the LLM sees). The second is the React Navigation route name. Pass both when they differ so back-navigation resolves correctly.

If your route name matches the Flint name, the second argument is optional:

```tsx
useFlintScreen('settings'); // route is also "settings"
```

---

## Exposing content

### FlintText

Drop-in replacement for `Text`. Registers a key/value pair in the semantic layer.

```tsx
import { FlintText } from 'flint-react-native';

<FlintText flintKey="title" style={styles.title}>
  Shopping Cart
</FlintText>

<FlintText flintKey="itemCount" style={styles.subtitle}>
  {`${items.length} items`}
</FlintText>
```

The LLM sees:

```
title: Shopping Cart
itemCount: 3 items
```

`FlintText` renders a normal `Text` component. It accepts all `TextProps`.

### Lists with FlintItem

For repeated data (FlatList, map, etc.), wrap each item in `FlintItem` and use `FlintText` inside.

```tsx
import { useFlintList, FlintItem, FlintText, FlintAction } from 'flint-react-native';

export function CartScreen() {
  useFlintScreen('cart', 'Cart');
  useFlintList('items', 'Items in the shopping cart');

  return (
    <FlatList
      data={cartItems}
      renderItem={({ item, index }) => (
        <FlintItem list="items" index={index}>
          <FlintText flintKey="name">{item.name}</FlintText>
          <FlintText flintKey="price">{`$${item.price}`}</FlintText>
          <FlintAction
            flintName="remove"
            flintDescription="Remove item from cart"
            onPress={() => removeItem(item.id)}
          >
            <Text>Remove</Text>
          </FlintAction>
        </FlintItem>
      )}
    />
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

`useFlintList(id, description)` registers the list. `FlintItem` provides context so nested `FlintText` and `FlintAction` components know which list and index they belong to.

---

## Exposing actions

### FlintAction (inline)

A pressable element that registers an invocable action. Works standalone or inside `FlintItem`.

```tsx
<FlintAction
  flintName="checkout"
  flintDescription="Proceed to checkout"
  onPress={handleCheckout}
>
  <Text>Checkout</Text>
</FlintAction>
```

Standalone actions appear in the screen output:

```
actions: checkout (Proceed to checkout)
```

Inside a list item, they appear on the item:

```
items:
  [0] name: Blue Shirt | price: $29.99
      actions: remove (Remove item from cart)
```

### useFlintTools (screen-level tools)

For operations that aren't tied to a specific UI element. Search, navigation, refresh, etc.

```tsx
import { useFlintTools } from 'flint-react-native';

export function HomeScreen({ navigation }) {
  useFlintScreen('home', 'Home');

  useFlintTools([
    {
      name: 'search',
      description: 'Search products by name',
      params: [
        { name: 'query', type: 'string', description: 'Search query', required: true },
      ],
      action: (params) => {
        navigation.navigate('SearchResults', { query: params.query });
      },
    },
    {
      name: 'refresh',
      description: 'Reload the product list',
      action: () => fetchProducts(),
    },
  ]);

  // ...
}
```

The LLM sees these in the `tools:` line and can call them with typed parameters.

---

## Screen scoping

Content and tools are automatically scoped to the active screen. When using a stack navigator, both the previous and current screen stay mounted. Flint filters the output so the LLM only sees data from the focused screen.

This happens automatically. No extra code needed.

---

## Reading the screen (CDP)

In dev mode, Flint exposes `globalThis.__flint__` with these methods:

| Method | Returns | Description |
|--------|---------|-------------|
| `readScreen()` | `string` | Human-readable snapshot of the current screen |
| `getScreen()` | `string` | Current screen name |
| `getSchema()` | `string` (JSON) | MCP-compatible tool schema |
| `callTool(name, params)` | `"ok"` or `"error: ..."` | Execute a tool by name |
| `invokeAction(name, listId?, itemIndex?)` | `"ok"` or `"error: ..."` | Invoke an action |

### CLI tool

The package includes `flint-cdp`, a CLI that calls these methods over Chrome DevTools Protocol.

```bash
# Read current screen
npx flint-cdp read

# Get tool schema
npx flint-cdp schema

# Call a tool
npx flint-cdp call search --params '{"query":"shoes"}'

# Invoke an action on a list item
npx flint-cdp action remove items 0

# Options
npx flint-cdp read --port 8081    # Metro port (default: 8081)
npx flint-cdp call search --delay 1000  # Wait ms before auto-read (default: 500)
```

### Direct CDP evaluation

Connect to `ws://localhost:8081/inspector/debug?device=<id>&page=1` and send:

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

Discover the device ID at `http://localhost:8081/json`.

---

## Full example

```tsx
import React from 'react';
import { View, FlatList, Text, StyleSheet } from 'react-native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import {
  FlintProvider,
  FlintNavigationContainer,
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintItem,
  FlintText,
  FlintAction,
} from 'flint-react-native';

const Stack = createNativeStackNavigator();

function TodoScreen({ navigation }) {
  useFlintScreen('todos', 'Todos');
  useFlintList('tasks', 'List of tasks');

  useFlintTools([
    {
      name: 'add_task',
      description: 'Add a new task',
      params: [
        { name: 'title', type: 'string', description: 'Task title', required: true },
      ],
      action: (params) => addTask(params.title),
    },
  ]);

  return (
    <FlatList
      data={tasks}
      renderItem={({ item, index }) => (
        <FlintItem list="tasks" index={index}>
          <FlintText flintKey="title">{item.title}</FlintText>
          <FlintText flintKey="status">{item.done ? 'done' : 'pending'}</FlintText>
          <FlintAction
            flintName="toggle"
            flintDescription="Mark task as done or pending"
            onPress={() => toggleTask(item.id)}
          >
            <Text>{item.done ? 'Undo' : 'Done'}</Text>
          </FlintAction>
        </FlintItem>
      )}
    />
  );
}

export default function App() {
  return (
    <FlintProvider>
      <FlintNavigationContainer>
        <Stack.Navigator>
          <Stack.Screen name="Todos" component={TodoScreen} />
        </Stack.Navigator>
      </FlintNavigationContainer>
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
| `FlintNavigationContainer` | Same as `NavigationContainer` | Auto-tracks React Navigation screen changes. |
| `FlintText` | `flintKey: string`, `children: string`, + all `TextProps` | Registers a key/value pair. Renders a `Text`. |
| `FlintItem` | `list: string`, `index: number`, `children` | Context provider for list items. |
| `FlintAction` | `flintName: string`, `flintDescription?: string`, `onPress`, + all `PressableProps` | Registers an invocable action. Renders a `Pressable`. |

### Hooks

| Hook | Signature | Description |
|------|-----------|-------------|
| `useFlintScreen` | `(name: string, routeName?: string) => void` | Sets the active screen name. |
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
  type: 'string' | 'integer' | 'number' | 'boolean';
  description: string;
  required?: boolean;
};
```

---

## Requirements

- React >= 18.0.0
- React Native >= 0.72.0
- `@react-navigation/native` >= 6.0.0 (optional, for `FlintNavigationContainer`)
- `expo-router` >= 3.0.0 (optional, for `useFlintExpoRouter`)
