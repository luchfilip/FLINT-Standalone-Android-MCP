# Cross-Platform Research: Flint SDK for React Native & Web

Research conducted March 2026. Investigates how to bring Flint's context-efficient, annotation-based AI interaction model to React Native and web apps.

## Core Insight

Flint's power comes from the developer declaring what's on each screen. The AI gets a tiny structured response, not a DOM dump or accessibility tree. One response: screen name, content fields, available actions. This must be preserved across platforms.

All outside-in tools (Playwright MCP, Stagehand, Browser-Use, Computer Use) are token-hungry because they scrape/screenshot. The inside-out approach where the app exposes only what matters is fundamentally more efficient.

---

## Competitive Landscape

### Outside-In Tools (No App Cooperation)

**Playwright MCP (Microsoft).** Official MCP server using accessibility tree snapshots. ~114K tokens per session. The CLI variant uses ~27K tokens (4x reduction). Works on any website. No app changes needed. But generic, no app-defined tools.

**Playwright CLI (Microsoft, 2026).** Leaner alternative to Playwright MCP. Single CLI command per step instead of full MCP tool schemas. ~27K tokens per session. Still outside-in.

**Stagehand v3 (Browserbase).** AI web agent SDK. Three primitives: `act()`, `extract()`, `observe()`. Uses CDP directly (removed Playwright dependency in v3). ~$0.002-0.01 per action. Auto-caching skips repeated LLM calls. Closest to Flint philosophy but works without app cooperation (AI figures out UI).

**Browser-Use.** Python/TypeScript agent framework. Full agent loop with screenshots + DOM. ~$0.02-0.08 per 5 steps. ~72-78% task completion on WebVoyager benchmark. Maximum flexibility, minimum efficiency.

**Anthropic Computer Use.** Screenshot + coordinate tapping. Works on any app on any platform. Massive token cost per interaction. ~466-499 token system prompt overhead + screenshot vision pricing per step. Validates why structured approaches exist.

**Maestro MCP.** 14 tools including `inspect_view_hierarchy` (CSV format). Uses accessibility-based interaction. Works for React Native since RN exposes accessibility info. Outside-in. Closest mobile competitor but lacks app-defined tools.

**Mobile MCP (mobile-next).** Platform-agnostic MCP server for iOS/Android. Hybrid accessibility + screenshot approach. Works on emulators, simulators, real devices.

### Inside-Out Tools (App Cooperation Required)

**Flint SDK (ours).** Android/Compose. Developer annotates screens, content, actions. AI gets flat text response. ~50-200 tokens per screen read. One round trip. The benchmark.

**Google WebMCP (Chrome 146 Canary, Feb 2026).** W3C standard by Google + Microsoft. Websites expose structured tools via `navigator.modelContext`. Two APIs: declarative (annotate HTML forms) and imperative (JS-based). Claims **89% token savings** over screenshot methods. Validates the Flint thesis at browser level.

### Comparison Matrix

| Tool | Tokens/Interaction | Dev Effort | Structured Data | Round Trips | App Cooperation |
|------|-------------------|------------|-----------------|-------------|-----------------|
| Flint (Compose) | ~50-200 | Medium | Yes, developer-curated | 1 | Yes |
| WebMCP (Google) | Low (89% savings) | Low | Yes, developer-curated | 1 | Yes |
| Playwright CLI | ~27K/session | None | Accessibility tree | 1 | No |
| Playwright MCP | ~114K/session | None | Accessibility tree | 1-2 | No |
| Stagehand v3 | $0.002-0.01/action | None | Hybrid DOM+AI | 1-2 | No |
| Maestro MCP | Medium (CSV tree) | None | View hierarchy | 1-2 | No |
| Browser-Use | $0.02-0.08/5 steps | None | Screenshots+DOM | 3-5+ | No |
| Computer Use | High (vision) | None | Pixels | 2+ | No |

---

## React Native Findings

### Accessibility Tree

RN exposes accessibility through standard props: `accessibilityLabel`, `accessibilityRole`, `accessibilityState`, `accessibilityValue`, `testID`. These map to native platform APIs (Android `AccessibilityNodeInfo`, iOS `UIAccessibility`).

**Critical difference from Compose:** RN's accessibility props are a fixed, closed set. Compose has extensible `SemanticsPropertyKey<T>` that Flint piggybacks on. RN has no equivalent. You cannot add `flintContent` as an accessibility property that flows through the native tree.

**Solution:** Build a parallel JS-side registry. Components register with the registry via hooks/refs. The registry IS the semantic tree. No native tree walking needed.

### Proposed Developer API

```tsx
// Screen tracking (automatic via wrapper, zero per-screen code)
<FlintNavigationContainer>
  {/* existing navigation tree */}
</FlintNavigationContainer>

// OR explicit per screen
useFlintScreen("search_results");

// Tool registration
useFlintTools([
  { name: "play_track", description: "Play a track",
    params: [{ name: "id", type: "string" }],
    action: (p) => playTrack(p.id) },
]);

// Content annotations (spread props, 1 per element)
<Text {...flintContent("title")}>{track.title}</Text>
<FlatList {...flintList("results")} renderItem={({item, index}) => (
  <View {...flintItem(index)}>
    <Text {...flintContent("title")}>{item.title}</Text>
    <Pressable {...flintAction("play")} onPress={() => play(item)}>
      <Text>Play</Text>
    </Pressable>
  </View>
)} />
```

### Communication Channel

- **Android:** Native module wrapping a ContentProvider. Same authority pattern (`content://<package>.flint`). Hub discovers it automatically. Zero Hub changes.
- **iOS:** Embedded HTTP server (GCDWebServer or Swift equivalent). Hub connects over local network.
- **Dev mode:** HTTP server over WiFi (like existing `FlintNetworkServer`).

### Advantages Over Compose

- Screen tracking can be fully automatic (React Navigation global listener)
- Tool definitions more concise in JS (no KSP needed)
- Hot reload friendly
- Works on both Android and iOS from single codebase

### Challenges

- No extensible semantic tree (solved by JS registry)
- Text content extraction needs explicit value: `flintContent("title", track.title)`
- iOS channel adds ~300 lines of native code
- No compile-time validation (runtime registration)
- Cross-architecture support (Fabric TurboModules vs old Bridge)

### Estimated SDK Size

~1,200 lines total (vs ~800 for Compose SDK):
- JS runtime (registry, hooks, snapshot, text renderer): ~500-700 lines
- Android native module (ContentProvider bridge): ~200 lines
- iOS native module (HTTP server bridge): ~300 lines
- TypeScript types: ~100 lines

### Existing RN Ecosystem

- **react-native-mcp (ohah):** 49 tools, Fiber tree access via Babel preset. Developer tooling, not production AI.
- **react-native-ai-devtools:** CDP connection to Metro. Developer-focused.
- **Detox (Wix):** Gray-box E2E testing via `testID`. Proves testID is reliable for element identification.
- **Callstack Agent Skills:** RN best practices for AI coding agents. Not runtime interaction.
- No direct competitor for inside-out, annotation-based AI interaction in RN.

---

## Web Findings

### Annotation Layer

**Data attributes are the universal solution.** `data-flint-content="title"`, `data-flint-list="results"`, etc. Work with every framework. No build step. Zero runtime cost until queried. Queryable via `querySelectorAll('[data-flint-content]')` in under 1ms on 1000-node pages.

### Proposed Developer API

**React:**
```jsx
useFlintScreen("search_results");
useFlintTools([{ name: "go_back", action: () => navigate(-1) }]);

<ul data-flint-list="results">
  <li data-flint-item="0">
    <span data-flint-content="title">Blue Train</span>
    <button data-flint-action="play">Play</button>
  </li>
</ul>
```

**Vue:**
```vue
<span v-flint-content="'title'">{{ track.title }}</span>
<ul v-flint-list="'results'">
  <li v-for="(t, i) in tracks" v-flint-item="i">...</li>
</ul>
```

**Vanilla:**
```html
<span data-flint-content="title">Blue Train</span>
<script>Flint.screen("search"); Flint.tool("go_back", { action: () => history.back() });</script>
```

### Communication (The Hard Part)

Browser can't host a server. The SDK connects outbound via WebSocket to a local MCP server. Inverted vs Android (Hub calls app) but architecturally clean. MCP server sends "read_screen" over WebSocket, SDK responds with flat text snapshot.

### Tree Walking

No reflection hacks needed. `querySelectorAll('[data-flint-content]')` is fast and built for this. Shadow DOM requires `element.shadowRoot` traversal for open roots. `FlintTextRenderer` output format is identical to Android.

### Architecture Options

| Architecture | Best For | Token Efficiency | Production? |
|---|---|---|---|
| In-App SDK + WebSocket | First-party apps | Best (developer-curated) | Yes |
| Browser Extension + MCP | Testing/debugging | Good (reads annotations) | No (requires install) |
| Playwright/CDP + annotations | CI/CD testing | Good (page.evaluate reads data-*) | No (external process) |
| Build on WebMCP | Future-proof | Best (browser-native) | Yes (if standard ships) |

### Web-Specific Challenges

- Cross-origin iframes (same-origin only without Playwright)
- CSP policies (in-app SDK avoids this since it's bundled)
- Shadow DOM (open roots traversable, closed roots inaccessible)
- WebSocket to localhost may be blocked by corporate proxies
- Browser tab must be open and active

### WebMCP (Google) Consideration

WebMCP is a W3C standard doing exactly what Flint Web would do. If it gains traction (Google + Microsoft backing), Flint Web could be a developer-friendly layer on top rather than a competing protocol. Worth monitoring Chrome 146+ adoption.

---

## Strategic Recommendations

1. **React Native SDK first.** No inside-out competition exists. ContentProvider compatibility means zero Hub changes on Android. Clear market gap.

2. **Web SDK second.** Monitor WebMCP adoption. If it ships broadly, build Flint Web as a DX layer on top. If not, ship standalone in-app SDK with data attributes + WebSocket.

3. **Same response format everywhere.** The flat text format is the killer feature. One format across Compose, RN, and Web. AI doesn't care what platform it's talking to.

4. **Level 0 fallback on every platform.** Android: AccessibilityService. iOS: VoiceOver tree. Web: Playwright accessibility snapshot. Always works, just less efficient.

---

## Key URLs

- [Playwright MCP](https://github.com/microsoft/playwright-mcp)
- [Stagehand](https://github.com/browserbase/stagehand)
- [Browser-Use](https://github.com/browser-use/browser-use)
- [Chrome DevTools MCP](https://github.com/ChromeDevTools/chrome-devtools-mcp)
- [WebMCP W3C Standard](https://webmcp.link/)
- [WebMCP Chrome Blog](https://developer.chrome.com/blog/webmcp-epp)
- [Maestro MCP](https://docs.maestro.dev/get-started/maestro-mcp)
- [Mobile MCP](https://github.com/mobile-next/mobile-mcp)
- [react-native-mcp](https://github.com/nicepkg/react-native-mcp)
