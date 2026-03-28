import { FlintRegistry } from "../FlintRegistry";
import { exposeBridge } from "../FlintBridge";

declare var __flint__: any;

describe("FlintBridge", () => {
  let registry: FlintRegistry;

  beforeAll(() => {
    (globalThis as any).__DEV__ = true;
  });

  beforeEach(() => {
    registry = new FlintRegistry();
    delete (globalThis as any).__flint__;
  });

  // 1. exposes readScreen on global.__flint__
  it("exposes readScreen on global.__flint__", () => {
    exposeBridge(registry);
    expect((globalThis as any).__flint__).toBeDefined();
    expect(typeof (globalThis as any).__flint__.readScreen).toBe("function");
  });

  // 2. exposes getScreen on global.__flint__
  it("exposes getScreen on global.__flint__", () => {
    exposeBridge(registry);
    expect(typeof (globalThis as any).__flint__.getScreen).toBe("function");
  });

  // 3. exposes getSchema on global.__flint__
  it("exposes getSchema on global.__flint__", () => {
    exposeBridge(registry);
    expect(typeof (globalThis as any).__flint__.getSchema).toBe("function");
  });

  // 4. exposes callTool on global.__flint__
  it("exposes callTool on global.__flint__", () => {
    exposeBridge(registry);
    expect(typeof (globalThis as any).__flint__.callTool).toBe("function");
  });

  // 5. exposes invokeAction on global.__flint__
  it("exposes invokeAction on global.__flint__", () => {
    exposeBridge(registry);
    expect(typeof (globalThis as any).__flint__.invokeAction).toBe("function");
  });

  // 6. readScreen returns string
  it("readScreen returns string", () => {
    registry.setScreen("Home");
    exposeBridge(registry);
    const result = (globalThis as any).__flint__.readScreen();
    expect(typeof result).toBe("string");
    expect(result).toContain("Home");
  });

  // 7. callTool returns "ok" for valid tool
  it('callTool returns "ok" for valid tool', () => {
    const fn = jest.fn();
    registry.registerTools([
      { name: "refresh", description: "Refresh", action: fn },
    ]);
    exposeBridge(registry);
    const result = (globalThis as any).__flint__.callTool("refresh", {});
    expect(result).toBe("ok");
    expect(fn).toHaveBeenCalled();
  });

  // 8. callTool returns "error: unknown tool" for invalid tool
  it('callTool returns "error: unknown tool" for invalid tool', () => {
    exposeBridge(registry);
    const result = (globalThis as any).__flint__.callTool("nonexistent", {});
    expect(result).toBe("error: unknown tool");
  });

  // 9. invokeAction returns "ok" for valid action
  it('invokeAction returns "ok" for valid action', () => {
    const fn = jest.fn();
    registry.registerAction({
      name: "submit",
      description: "Submit form",
      handler: fn,
    });
    exposeBridge(registry);
    const result = (globalThis as any).__flint__.invokeAction("submit");
    expect(result).toBe("ok");
    expect(fn).toHaveBeenCalled();
  });

  // 10. invokeAction returns "error: action not found" for invalid action
  it('invokeAction returns "error: action not found" for invalid action', () => {
    exposeBridge(registry);
    const result = (globalThis as any).__flint__.invokeAction("nonexistent");
    expect(result).toBe("error: action not found");
  });

  // 11. cleanup removes global.__flint__
  it("cleanup removes global.__flint__", () => {
    const cleanup = exposeBridge(registry);
    expect((globalThis as any).__flint__).toBeDefined();
    cleanup();
    expect((globalThis as any).__flint__).toBeUndefined();
  });

  // 12. does not expose in production (__DEV__ = false)
  it("does not expose in production (__DEV__ = false)", () => {
    const prev = (globalThis as any).__DEV__;
    (globalThis as any).__DEV__ = false;
    try {
      const cleanup = exposeBridge(registry);
      expect((globalThis as any).__flint__).toBeUndefined();
      cleanup(); // should be a no-op
    } finally {
      (globalThis as any).__DEV__ = prev;
    }
  });
});
