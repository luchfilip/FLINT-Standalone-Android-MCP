(global as any).__DEV__ = true;

import React from "react";
import { renderHook } from "@testing-library/react";
import { FlintProvider, useFlintRegistry } from "../../FlintProvider";
import { useFlintList } from "../../hooks/useFlintList";
import { FlintRegistry } from "../../FlintRegistry";

describe("useFlintList", () => {
  let capturedRegistry: FlintRegistry;

  function CaptureRegistry() {
    capturedRegistry = useFlintRegistry();
    return null;
  }

  function wrapper({ children }: { children: React.ReactNode }) {
    return (
      <FlintProvider>
        <CaptureRegistry />
        {children}
      </FlintProvider>
    );
  }

  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("registers list with id and description", () => {
    renderHook(() => useFlintList("task-list", "A list of tasks"), { wrapper });

    const snapshot = capturedRegistry.snapshot();
    const list = snapshot.content.elements.find(
      (e) => e.type === "list" && e.id === "task-list"
    );
    expect(list).toBeDefined();
    expect((list as any).description).toBe("A list of tasks");
  });

  it("defaults description to id when not provided", () => {
    renderHook(() => useFlintList("my-list"), { wrapper });

    const snapshot = capturedRegistry.snapshot();
    const list = snapshot.content.elements.find(
      (e) => e.type === "list" && e.id === "my-list"
    );
    expect(list).toBeDefined();
    expect((list as any).description).toBe("my-list");
  });

  it("cleans up on unmount", () => {
    const { unmount } = renderHook(() => useFlintList("temp-list", "Temporary"), {
      wrapper,
    });

    const before = capturedRegistry.snapshot();
    expect(
      before.content.elements.some(
        (e: any) => e.type === "list" && e.id === "temp-list"
      )
    ).toBe(true);

    unmount();

    const after = capturedRegistry.snapshot();
    expect(
      after.content.elements.some(
        (e: any) => e.type === "list" && e.id === "temp-list"
      )
    ).toBe(false);
  });
});
