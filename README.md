# Flint SDK

Give an LLM structured control of any app. Android, React Native, or web.

Flint is an SDK that lets app developers annotate their code so AI agents can interact with the app directly: call named tools, read structured screen data, invoke actions by name. No screenshot parsing, no coordinate guessing, no DOM scraping. The AI gets a flat text snapshot of what's on screen, calls tools with typed parameters, and acts on elements by name.

Works on three platforms with three transport layers, all producing the same output format.

## Three ways to connect

### 1. ADB direct (Android, recommended)

Best for AI agents building or testing Android apps. No Hub app, no MCP server, no extra infrastructure. Just `adb shell content read` commands that return clean JSON. The app's ContentProvider handles everything.

```bash
adb shell content read --uri content://com.example.myapp.flint/read_screen
```

Returns structured JSON. Call tools, read screens, invoke actions. All via ADB.

See [docs/adb-mode.md](docs/adb-mode.md) for the full command reference.

### 2. SDK local server (React Native and Web)

For React Native apps, Flint exposes `globalThis.__flint__` through Metro's CDP (Chrome DevTools Protocol). For web apps, the same interface lives on `window.__flint__`. AI agents evaluate JS expressions to read screens and call tools.

**React Native:**
```bash
npx flint-cdp read          # read current screen
npx flint-cdp call search --params '{"query":"jazz"}'
```

**Web (browser console, Playwright, Puppeteer):**
```js
window.__flint__.readScreen()
window.__flint__.callTool("search", { query: "jazz" })
```

See [docs/react-native-agent-guide.md](docs/react-native-agent-guide.md) and [docs/web-agent-guide.md](docs/web-agent-guide.md).

### 3. MCP Hub (Android, production/wireless)

The Flint Hub is an Android app that runs an MCP server on the device. Exposes 25+ built-in device tools (tap, swipe, SMS, calls, notifications, etc.) plus any Flint SDK app tools. MCP clients connect over WiFi. No laptop, no USB, no ADB.

This is the right choice for production device automation, multi-app workflows, or when you need generic device control beyond a single app.

See [docs/mcp-hub-guide.md](docs/mcp-hub-guide.md).

## What the AI sees

Same output regardless of platform or transport:

```
screen: home
heading: Sample Store
cartSummary: 0 item(s) in cart
products:
  [0] name: Wireless Headphones | price: $79.99 | actions: view, add_to_cart
  [1] name: Running Shoes | price: $129.99 | actions: view, add_to_cart
tools: view_product, go_to_cart
```

One format. One protocol. The AI doesn't know or care whether it's talking to a Compose app via ADB, a React Native app via CDP, or a web app via the browser console.

## Why this exists

LLMs can reason about UIs but they can't touch a phone or interact with an app natively. Existing tools (Playwright MCP, Computer Use, mobile-mcp) work by scraping accessibility trees or screenshots. They're generic but expensive: ~27K-114K tokens per session, lots of round trips, brittle coordinate-based interaction.

Flint takes the opposite approach. The app declares what's on each screen. The AI gets a tiny structured response (~50-200 tokens), calls named tools, and invokes actions by name. One round trip per interaction.

## Use cases

**QA and testing.** An AI agent builds your app, installs it via ADB, queries the Flint ContentProvider to verify screen content and test workflows. No mocking, no Espresso, no Appium. Tests run on real devices against real UIs.

**Device automation.** The MCP Hub gives any AI assistant full device control: send messages, manage contacts, adjust settings, launch apps. Users and agents automate multi-app workflows through natural language.

**AI-native apps.** Apps that integrate the SDK become directly accessible to any AI assistant. Named tools appear in the assistant's tool list. The app speaks the AI's language.

## Quick start

### Android (Kotlin/Compose)

**1. Add the SDK:**

```kotlin
// build.gradle.kts (app module)
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.flintsdk:runtime:1.0.0")
    ksp("com.flintsdk:compiler:1.0.0")
}
```

**2. Initialize with ADB mode:**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Flint.init(this, adbMode = BuildConfig.DEBUG)
    }
}
```

**3. Query via ADB:**

```bash
./gradlew :app:installDebug
adb shell content read --uri content://com.example.myapp.flint/get_schema
adb shell content read --uri content://com.example.myapp.flint/read_screen
```

### React Native

**1. Install:**

```json
{ "dependencies": { "flint-react-native": "file:../../flint-react-native" } }
```

**2. Wrap your app:**

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

**3. Query via CDP:**

```bash
npx flint-cdp read
npx flint-cdp schema
npx flint-cdp call search --params '{"query":"jazz"}'
```

### Web (React)

**1. Install:**

```json
{ "dependencies": { "flint-web": "file:../../flint-web" } }
```

**2. Wrap your app:**

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

**3. Query via browser console or Playwright:**

```js
window.__flint__.readScreen()
window.__flint__.getSchema()
window.__flint__.callTool("view_product", { id: "1" })
```

## Annotating a screen

Same concepts across all platforms. Name the screen, register tools, label content, define actions.

```tsx
// React Native / Web (import from "flint-react-native" or "flint-web")
import { useFlintScreen, useFlintTools, useFlintList, FlintText, FlintItem, FlintAction } from "flint-web";

function CartScreen() {
  useFlintScreen("cart");
  useFlintList("items", "Items in the cart");
  useFlintTools([
    { name: "checkout", description: "Proceed to checkout", action: () => startCheckout() },
  ]);

  return (
    <div>
      <FlintText flintKey="total">{`$${total}`}</FlintText>
      {items.map((item, i) => (
        <FlintItem list="items" index={i} key={item.id}>
          <FlintText flintKey="name">{item.name}</FlintText>
          <FlintAction flintName="remove" flintDescription="Remove from cart" onClick={() => removeItem(item.id)}>
            Remove
          </FlintAction>
        </FlintItem>
      ))}
    </div>
  );
}
```

AI sees:

```
screen: cart
total: $94.98
items:
  [0] name: Wireless Headphones | actions: remove
  [1] name: Coffee Maker | actions: remove
tools: checkout
```

For Kotlin/Compose annotation, see the [full spec](docs/flint-full-spec-v1.md).

## Progressive integration

Apps don't need full annotation. Each level adds more capability:

| Level | What you add | What the AI gets |
|-------|-------------|-----------------|
| 0 | Nothing (every app) | Accessibility tree + screenshots (slow, unstructured) |
| 1 | `@FlintTool` + `Flint.screen()` | Named tools, screen tracking |
| 2 | `flintContent("title")` | Structured key/value data instead of raw text |
| 3 | `flintAction("play")` | Named actions that survive layout changes |
| 4 | Full annotation | Complete structured representation |

## Modules

| Module | What it does |
|--------|-------------|
| `sdk/annotations` | Kotlin annotations: `@FlintTool`, `@FlintScreen`, `@FlintAction`, `@FlintSemanticTree` |
| `sdk/compiler` | KSP processor that generates `flint-manifest.json` at build time |
| `sdk/runtime` | Android library: ContentProvider, action invoker, tree walker, data models |
| `hub/app` | Android app with embedded MCP server, 25+ built-in tools, Flint app discovery |
| `flint-core` | Shared TypeScript library: registry, hooks, bridge, schema, renderer |
| `flint-web` | Web SDK: `<span>`, `<button>`, React Router v6 screen tracking |
| `flint-react-native` | React Native SDK: `<Text>`, `<Pressable>`, React Navigation screen tracking, CDP CLI |
| `integrations/openclaw` | OpenClaw plugin: exposes Flint Hub tools as native agent tools |
| `sample/musicapp` | Kotlin/Compose sample with full Flint annotation |
| `sample/webapp` | React sample store app |
| `sample/musicapp-rn` | React Native sample music app |

## Architecture

```
AI Agent
  |
  |-- ADB shell ---------> Android App (ContentProvider)     [direct, recommended]
  |-- CDP WebSocket -----> React Native App (Metro runtime)  [dev mode]
  |-- JS evaluate -------> Web App (browser runtime)         [dev mode]
  |-- HTTP/SSE ----------> Flint Hub (MCP server on device)  [production, wireless]
                              |
                              |-- Accessibility Service (any app)
                              |-- ContentProvider (Flint SDK apps)
                              |-- System APIs (SMS, calls, etc.)
```

## Docs

| Doc | For |
|-----|-----|
| [ADB Mode](docs/adb-mode.md) | AI agents testing Android apps via ADB |
| [React Native Agent Guide](docs/react-native-agent-guide.md) | AI agents testing RN apps via CDP |
| [Web Agent Guide](docs/web-agent-guide.md) | AI agents testing web apps via browser/Playwright |
| [MCP Hub Guide](docs/mcp-hub-guide.md) | Production device automation via MCP |
| [SDK Reference](docs/flint-sdk-reference.md) | Developer API for all platforms (hooks, components, types) |
| [Full Spec](docs/flint-full-spec-v1.md) | Complete protocol spec (Android SDK, manifest, ContentProvider, security) |

## License

Apache License 2.0. See [LICENSE](LICENSE).
