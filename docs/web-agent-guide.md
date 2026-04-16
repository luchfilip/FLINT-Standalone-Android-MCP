# Flint Web: Agent Guide

How to interact with a Flint-enabled web app from an AI agent, test runner, or browser console. The app exposes `window.__flint__` in dev mode. No server, no MCP, no build step.

## Prerequisites

- Web app running in a browser (dev server, e.g., `npm run dev`)
- App wraps its root in `<FlintProvider>` and `<FlintRouter>`
- Uses `flint-web` package

## Quick start (browser console)

Open the app in a browser, open DevTools console:

```js
window.__flint__.readScreen()    // flat text snapshot
window.__flint__.getSchema()     // JSON tool definitions
window.__flint__.getScreen()     // current screen name
window.__flint__.callTool("view_product", { id: "1" })
window.__flint__.invokeAction("add_to_cart")
window.__flint__.invokeAction("remove", "cart_items", 0)  // list item action
```

Every method returns a plain string. No promises, no async.

## Quick start (Playwright)

```js
const screen = await page.evaluate(() => window.__flint__.readScreen());
const schema = await page.evaluate(() => window.__flint__.getSchema());

await page.evaluate(() => window.__flint__.callTool("view_product", { id: "1" }));
await page.waitForTimeout(300);
const newScreen = await page.evaluate(() => window.__flint__.readScreen());
```

## Quick start (Puppeteer)

```js
const screen = await page.evaluate(() => window.__flint__.readScreen());

await page.evaluate(() => window.__flint__.callTool("add_to_cart", {}));
await page.waitForTimeout(300);
const result = await page.evaluate(() => window.__flint__.readScreen());
```

## Commands

### readScreen()

Returns a flat text snapshot. Screen name, content fields, lists with items and actions, available tools.

```js
window.__flint__.readScreen()
```

Output:
```
screen: home
heading: Sample Store
cartSummary: 0 item(s) in cart
products:
  [0] name: Wireless Headphones | price: $79.99 | category: Electronics | actions: view, add_to_cart
  [1] name: Running Shoes | price: $129.99 | category: Sports | actions: view, add_to_cart
tools: view_product, go_to_cart
```

### getSchema()

Returns MCP-compatible JSON string with tool definitions and typed parameters.

```js
JSON.parse(window.__flint__.getSchema())
```

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

### getScreen()

Returns the current screen name only. `FlintRouter` auto-derives names from the URL pathname: `/search/results` becomes `search_results`, `/` becomes `home`.

```js
window.__flint__.getScreen()  // "home"
```

### callTool(name, params?)

Invokes a registered tool. Usually triggers navigation.

```js
window.__flint__.callTool("view_product", { id: "1" })  // "ok"
window.__flint__.callTool("go_to_cart")                  // "ok"
window.__flint__.callTool("nonexistent")                 // "error: unknown tool"
```

### invokeAction(name, listId?, itemIndex?)

Triggers a semantic action from the current screen.

```js
// Screen-level action
window.__flint__.invokeAction("checkout")

// List item action (remove item at index 0 from cart_items list)
window.__flint__.invokeAction("remove", "cart_items", 0)
```

## Typical agent workflow

```js
// 1. Discover what the app can do
const schema = JSON.parse(window.__flint__.getSchema());
console.log(schema.tools.map(t => t.name));

// 2. Read current screen
console.log(window.__flint__.readScreen());

// 3. Navigate using a tool
window.__flint__.callTool("view_product", { id: "1" });

// 4. Wait for React Router transition
await new Promise(r => setTimeout(r, 300));

// 5. Read new screen
console.log(window.__flint__.readScreen());

// 6. Take an action
window.__flint__.invokeAction("add_to_cart");

// 7. Navigate to cart
window.__flint__.callTool("go_to_cart");
await new Promise(r => setTimeout(r, 300));

// 8. Verify cart contents
console.log(window.__flint__.readScreen());
```

## Playwright test example

```js
import { test, expect } from "@playwright/test";

test("add product to cart", async ({ page }) => {
  await page.goto("http://localhost:5173");

  // Read home screen
  const home = await page.evaluate(() => window.__flint__.readScreen());
  expect(home).toContain("screen: home");
  expect(home).toContain("products:");

  // View a product
  await page.evaluate(() => window.__flint__.callTool("view_product", { id: "1" }));
  await page.waitForTimeout(300);

  const product = await page.evaluate(() => window.__flint__.readScreen());
  expect(product).toContain("screen: product");

  // Add to cart
  await page.evaluate(() => window.__flint__.invokeAction("add_to_cart"));

  // Go to cart and verify
  await page.evaluate(() => window.__flint__.callTool("go_to_cart"));
  await page.waitForTimeout(300);

  const cart = await page.evaluate(() => window.__flint__.readScreen());
  expect(cart).toContain("screen: cart");
  expect(cart).toContain("Wireless Headphones");
});
```

## Tools vs Actions

**Tools** are screen-level operations. They appear on the `tools:` line in `readScreen()` output. Called with `callTool(name, params)`. Accept typed parameters (check `getSchema()` for types).

**Actions** are element-level operations. They appear inline on list items or as standalone `actions:` lines. Called with `invokeAction(name)` for standalone, or `invokeAction(name, listId, itemIndex)` for list items. Take no parameters.

### Decision flow

1. Read `readScreen()`.
2. See `tools:` line? Use `callTool` (check `getSchema()` for params).
3. See `actions:` on a list item? Use `invokeAction(name, listId, itemIndex)`.
4. See standalone `actions:`? Use `invokeAction(name)`.

## Screen name derivation

`FlintRouter` converts URL pathnames to screen names:

| URL | Screen name |
|-----|-------------|
| `/` | `home` |
| `/products` | `products` |
| `/products/123` | `products_123` |
| `/search/results` | `search_results` |

Override with `screenNameMap`:
```tsx
<FlintRouter screenNameMap={{ "/dashboard": "main_dash", "/": "landing" }}>
```

## Error responses

```
"ok"                        # success
"error: unknown tool"       # callTool with invalid name
"error: action not found"   # invokeAction with invalid name/target
""                          # getScreen when no screen is active
```

## Notes

- `window.__flint__` is only exposed in dev mode. It does not exist in production builds.
- All methods are synchronous. No promises.
- After `callTool`, wait 200-300ms for React Router to transition before reading the new screen.
- Works in any browser. No extensions or plugins needed.
