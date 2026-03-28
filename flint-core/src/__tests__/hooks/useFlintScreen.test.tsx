(global as any).__DEV__ = true;

import React from "react";
import { renderHook, act } from "@testing-library/react";
import { FlintProvider } from "../../FlintProvider";
import { useFlintScreen } from "../../hooks/useFlintScreen";

function wrapper({ children }: { children: React.ReactNode }) {
  return <FlintProvider>{children}</FlintProvider>;
}

describe("useFlintScreen", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("sets screen name on mount", () => {
    renderHook(() => useFlintScreen("Home"), { wrapper });

    expect((globalThis as any).__flint__.getScreen()).toBe("Home");
  });

  it("clears screen on unmount", () => {
    const { unmount } = renderHook(() => useFlintScreen("Home"), { wrapper });

    // Save reference before unmount deletes __flint__ from globalThis.
    const bridge = (globalThis as any).__flint__;
    expect(bridge.getScreen()).toBe("Home");

    unmount();

    // Hook cleanup calls clearScreen before provider cleanup deletes the bridge.
    // Bridge getScreen returns "" when registry screen is null.
    expect(bridge.getScreen()).toBe("");
  });

  it("updates when name prop changes", () => {
    const { rerender } = renderHook(
      ({ name }) => useFlintScreen(name),
      { wrapper, initialProps: { name: "Home" } }
    );

    expect((globalThis as any).__flint__.getScreen()).toBe("Home");

    rerender({ name: "Settings" });

    expect((globalThis as any).__flint__.getScreen()).toBe("Settings");
  });

  it("registers route name mapping when routeName is provided", () => {
    renderHook(() => useFlintScreen("home", "Home"), { wrapper });

    const bridge = (globalThis as any).__flint__;
    expect(bridge.getScreen()).toBe("home");
  });
});
