import { FlintRegistry } from "../FlintRegistry";
import { FlintElement, FlintToolDef } from "../types";

describe("FlintRegistry", () => {
  let registry: FlintRegistry;

  beforeEach(() => {
    registry = new FlintRegistry();
  });

  // 1. registers and retrieves screen name
  it("registers and retrieves screen name", () => {
    registry.setScreen("Home");
    expect(registry.getScreen()).toBe("Home");
  });

  // 2. clears screen only if name matches
  it("clears screen only if name matches", () => {
    registry.setScreen("Home");
    registry.clearScreen("Settings");
    expect(registry.getScreen()).toBe("Home");

    registry.clearScreen("Home");
    expect(registry.getScreen()).toBeNull();
  });

  // 3. registers and retrieves tools
  it("registers and retrieves tools", () => {
    const tool: FlintToolDef = {
      name: "refresh",
      description: "Refresh data",
      action: jest.fn(),
    };
    registry.registerTools([tool]);
    expect(registry.getToolNames()).toEqual(["refresh"]);
    expect(registry.getTools()).toEqual([tool]);
  });

  // 4. callTool returns false for unknown tool
  it("callTool returns false for unknown tool", () => {
    expect(registry.callTool("nonexistent", {})).toBe(false);
  });

  // 5. callTool invokes action with params
  it("callTool invokes action with params", () => {
    const action = jest.fn();
    const tool: FlintToolDef = {
      name: "submit",
      description: "Submit form",
      action,
    };
    registry.registerTools([tool]);

    const result = registry.callTool("submit", { value: "hello" });
    expect(result).toBe(true);
    expect(action).toHaveBeenCalledWith({ value: "hello" });
  });

  // 6. registerContent returns cleanup function
  it("registerContent returns cleanup function", () => {
    const cleanup = registry.registerContent({ key: "title", value: "Hello" });
    expect(typeof cleanup).toBe("function");
  });

  // 7. cleanup function removes content
  it("cleanup function removes content", () => {
    registry.setScreen("Home");
    const cleanup = registry.registerContent({ key: "title", value: "Hello" });
    cleanup();
    const snap = registry.snapshot();
    const contentElements = snap.content.elements.filter(
      (e: FlintElement) => e.type === "content" && e.key === "title"
    );
    expect(contentElements).toHaveLength(0);
  });

  // 8. registerList + registerContent builds correct snapshot
  it("registerList + registerContent builds correct snapshot", () => {
    registry.setScreen("Home");
    registry.registerList({ id: "users", description: "User list" });
    registry.registerContent({
      key: "name",
      value: "Alice",
      listId: "users",
      itemIndex: 0,
    });

    const snap = registry.snapshot();
    const listEl = snap.content.elements.find((e: FlintElement) => e.type === "list");
    expect(listEl).toBeDefined();
    expect(listEl!.type).toBe("list");
    if (listEl && listEl.type === "list") {
      expect(listEl.items[0].content).toEqual({ name: "Alice" });
    }
  });

  // 9. list items sorted by index
  it("list items sorted by index", () => {
    registry.setScreen("Home");
    registry.registerList({ id: "items", description: "Item list" });
    registry.registerContent({
      key: "name",
      value: "Second",
      listId: "items",
      itemIndex: 2,
    });
    registry.registerContent({
      key: "name",
      value: "First",
      listId: "items",
      itemIndex: 0,
    });

    const snap = registry.snapshot();
    const listEl = snap.content.elements.find((e: FlintElement) => e.type === "list");
    if (listEl && listEl.type === "list") {
      expect(listEl.items[0].index).toBe(0);
      expect(listEl.items[1].index).toBe(2);
    }
  });

  // 10. actions with listId/itemIndex grouped correctly
  it("actions with listId/itemIndex grouped correctly", () => {
    registry.setScreen("Home");
    registry.registerList({ id: "items", description: "Item list" });
    registry.registerAction({
      name: "delete",
      description: "Delete item",
      handler: jest.fn(),
      listId: "items",
      itemIndex: 0,
    });

    const snap = registry.snapshot();
    const listEl = snap.content.elements.find((e: FlintElement) => e.type === "list");
    if (listEl && listEl.type === "list") {
      expect(listEl.items[0].actions).toEqual([
        { name: "delete", description: "Delete item" },
      ]);
    }
  });

  // 11. invokeAction matches by name only
  it("invokeAction matches by name only", () => {
    const handler = jest.fn();
    registry.registerAction({
      name: "save",
      description: "Save data",
      handler,
    });

    const result = registry.invokeAction("save");
    expect(result).toBe(true);
    expect(handler).toHaveBeenCalled();
  });

  // 12. invokeAction matches by name + listId + itemIndex
  it("invokeAction matches by name + listId + itemIndex", () => {
    const handler = jest.fn();
    registry.registerAction({
      name: "delete",
      description: "Delete item",
      handler,
      listId: "items",
      itemIndex: 1,
    });

    const result = registry.invokeAction("delete", "items", 1);
    expect(result).toBe(true);
    expect(handler).toHaveBeenCalled();
  });

  // 13. invokeAction returns false when not found
  it("invokeAction returns false when not found", () => {
    expect(registry.invokeAction("nonexistent")).toBe(false);
  });

  // 14. clearScreenState preserves tools
  it("clearScreenState preserves tools", () => {
    const tool: FlintToolDef = {
      name: "refresh",
      description: "Refresh",
      action: jest.fn(),
    };
    registry.registerTools([tool]);
    registry.setScreen("Home");
    registry.registerContent({ key: "title", value: "Hello" });

    registry.clearScreenState();

    expect(registry.getToolNames()).toEqual(["refresh"]);
    expect(registry.getScreen()).toBeNull();
  });

  // 15. clearAll removes everything
  it("clearAll removes everything", () => {
    registry.registerTools([
      { name: "t", description: "t", action: jest.fn() },
    ]);
    registry.setScreen("Home");
    registry.registerContent({ key: "k", value: "v" });

    registry.clearAll();

    expect(registry.getToolNames()).toEqual([]);
    expect(registry.getScreen()).toBeNull();
    const snap = registry.snapshot();
    expect(snap.content.elements).toEqual([]);
  });

  // 16. snapshot returns correct structure for empty state
  it("snapshot returns correct structure for empty state", () => {
    registry.setScreen("Empty");
    const snap = registry.snapshot();
    expect(snap).toEqual({
      screen: "Empty",
      content: { elements: [] },
      overlays: [],
    });
  });

  // 17. snapshot returns correct structure for complex state
  it("snapshot returns correct structure for complex state", () => {
    registry.setScreen("Dashboard");

    registry.registerContent({ key: "header", value: "Welcome" });
    registry.registerList({ id: "tasks", description: "Task list" });
    registry.registerContent({
      key: "title",
      value: "Task 1",
      listId: "tasks",
      itemIndex: 0,
    });
    registry.registerAction({
      name: "complete",
      description: "Mark complete",
      handler: jest.fn(),
      listId: "tasks",
      itemIndex: 0,
    });
    registry.registerAction({
      name: "refresh",
      description: "Refresh all",
      handler: jest.fn(),
    });
    registry.registerOverlay({
      id: "modal",
      description: "Confirm dialog",
      content: new Map([["message", "Are you sure?"]]),
      actions: [{ name: "confirm", description: "Confirm action" }],
    });

    const snap = registry.snapshot();

    expect(snap.screen).toBe("Dashboard");
    expect(snap.content.elements).toHaveLength(3);

    const content = snap.content.elements.find(
      (e: FlintElement) => e.type === "content"
    );
    expect(content).toEqual({ type: "content", key: "header", value: "Welcome" });

    const list = snap.content.elements.find((e: FlintElement) => e.type === "list");
    expect(list).toBeDefined();
    if (list?.type === "list") {
      expect(list.id).toBe("tasks");
      expect(list.items).toHaveLength(1);
      expect(list.items[0].content).toEqual({ title: "Task 1" });
      expect(list.items[0].actions).toEqual([
        { name: "complete", description: "Mark complete" },
      ]);
    }

    const action = snap.content.elements.find((e: FlintElement) => e.type === "action");
    expect(action).toEqual({
      type: "action",
      name: "refresh",
      description: "Refresh all",
    });

    expect(snap.overlays).toHaveLength(1);
    expect(snap.overlays[0]).toEqual({
      id: "modal",
      description: "Confirm dialog",
      content: { message: "Are you sure?" },
      actions: [{ name: "confirm", description: "Confirm action" }],
    });
  });

  // 18. snapshot only includes content from current screen
  it("snapshot only includes content from current screen", () => {
    registry.setScreen("home");
    registry.registerContent({ key: "title", value: "Home" });
    registry.setScreen("search");
    registry.registerContent({ key: "query", value: "jazz" });

    const snap = registry.snapshot();
    const contentElements = snap.content.elements.filter(
      (e: FlintElement) => e.type === "content"
    );
    expect(contentElements).toHaveLength(1);
    expect(contentElements[0]).toEqual({ type: "content", key: "query", value: "jazz" });
  });

  // 19. snapshot only includes lists from current screen
  it("snapshot only includes lists from current screen", () => {
    registry.setScreen("home");
    registry.registerList({ id: "playlists", description: "User playlists" });
    registry.registerContent({
      key: "name",
      value: "Chill",
      listId: "playlists",
      itemIndex: 0,
    });
    registry.setScreen("search");
    registry.registerList({ id: "results", description: "Search results" });
    registry.registerContent({
      key: "name",
      value: "Coltrane",
      listId: "results",
      itemIndex: 0,
    });

    const snap = registry.snapshot();
    const listElements = snap.content.elements.filter(
      (e: FlintElement) => e.type === "list"
    );
    expect(listElements).toHaveLength(1);
    if (listElements[0].type === "list") {
      expect(listElements[0].id).toBe("results");
    }
  });

  // 20. snapshot only includes tools from current screen
  it("snapshot only includes tools from current screen", () => {
    registry.setScreen("home");
    registry.registerTools([
      { name: "refresh", description: "Refresh home", action: jest.fn() },
    ]);
    registry.setScreen("search");
    registry.registerTools([
      { name: "search", description: "Run search", action: jest.fn() },
    ]);

    expect(registry.getToolNames()).toEqual(["search"]);
    expect(registry.getTools()).toHaveLength(1);
    expect(registry.getTools()[0].name).toBe("search");
  });

  // 21. entries with no screen tag are always included
  it("entries with no screen tag are always included", () => {
    // Register without setting a screen
    registry.registerContent({ key: "global", value: "always visible" });
    registry.registerTools([
      { name: "globalTool", description: "Global", action: jest.fn() },
    ]);

    // Now set a screen
    registry.setScreen("home");

    const snap = registry.snapshot();
    const contentElements = snap.content.elements.filter(
      (e: FlintElement) => e.type === "content"
    );
    expect(contentElements).toHaveLength(1);
    expect(contentElements[0]).toEqual({
      type: "content",
      key: "global",
      value: "always visible",
    });
    expect(registry.getToolNames()).toEqual(["globalTool"]);
  });

  // 22. callTool works across screens
  it("callTool works across screens", () => {
    const action = jest.fn();
    registry.setScreen("home");
    registry.registerTools([
      { name: "refresh", description: "Refresh", action },
    ]);
    registry.setScreen("search");

    const result = registry.callTool("refresh", { force: true });
    expect(result).toBe(true);
    expect(action).toHaveBeenCalledWith({ force: true });
  });

  // 23. invokeAction works across screens
  it("invokeAction works across screens", () => {
    const handler = jest.fn();
    registry.setScreen("home");
    registry.registerAction({
      name: "navigate",
      description: "Go somewhere",
      handler,
    });
    registry.setScreen("search");

    const result = registry.invokeAction("navigate");
    expect(result).toBe(true);
    expect(handler).toHaveBeenCalled();
  });

  // 24. registerScreenName maps route names to custom names
  it("registerScreenName maps route names to custom names", () => {
    registry.registerScreenName("Home", "home");
    registry.registerScreenName("SearchResults", "search_results");

    // setScreen with a route name resolves to the custom name
    registry.setScreen("Home");
    expect(registry.getScreen()).toBe("home");

    registry.setScreen("SearchResults");
    expect(registry.getScreen()).toBe("search_results");

    // Unknown route names pass through unchanged
    registry.setScreen("Unknown");
    expect(registry.getScreen()).toBe("Unknown");
  });

  // 25. registerScreenName cleanup removes the mapping
  it("registerScreenName cleanup removes the mapping", () => {
    const unregister = registry.registerScreenName("Home", "home");
    registry.setScreen("Home");
    expect(registry.getScreen()).toBe("home");

    unregister();
    registry.setScreen("Home");
    expect(registry.getScreen()).toBe("Home");
  });
});
