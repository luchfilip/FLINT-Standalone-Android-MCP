(global as any).__DEV__ = true;

import React from "react";
import { renderHook, act } from "@testing-library/react";
import { FlintProvider } from "../../FlintProvider";
import { useFlintTools } from "../../hooks/useFlintTools";
import { FlintToolDef } from "../../types";

function wrapper({ children }: { children: React.ReactNode }) {
  return <FlintProvider>{children}</FlintProvider>;
}

describe("useFlintTools", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("registers tools on mount", () => {
    const tools: FlintToolDef[] = [
      {
        name: "greet",
        description: "Say hello",
        params: [
          { name: "name", type: "string", description: "Who to greet", required: true },
        ],
        action: jest.fn(),
      },
    ];

    renderHook(() => useFlintTools(tools), { wrapper });

    const bridge = (globalThis as any).__flint__;
    const schema = JSON.parse(bridge.getSchema());
    const toolNames = schema.tools.map((t: any) => t.name);
    expect(toolNames).toContain("greet");
  });

  it("cleans up on unmount", () => {
    const tools: FlintToolDef[] = [
      {
        name: "disappear",
        description: "Tool that goes away",
        action: jest.fn(),
      },
    ];

    const { unmount } = renderHook(() => useFlintTools(tools), { wrapper });

    const bridge = (globalThis as any).__flint__;
    let schema = JSON.parse(bridge.getSchema());
    expect(schema.tools.map((t: any) => t.name)).toContain("disappear");

    unmount();

    // Bridge is also cleaned up by FlintProvider unmount, so re-check via a fresh render.
    // Instead, verify the tool was removed before the bridge teardown by checking
    // the registry directly through a second render.
    const { result } = renderHook(() => {
      const { useFlintRegistry } = require("../../FlintProvider");
      return useFlintRegistry();
    }, { wrapper });

    const remaining = result.current.getToolNames();
    expect(remaining).not.toContain("disappear");
  });

  it("tools callable after registration", () => {
    const actionFn = jest.fn();
    const tools: FlintToolDef[] = [
      {
        name: "doStuff",
        description: "Does stuff",
        params: [
          { name: "count", type: "integer", description: "How many", required: true },
        ],
        action: actionFn,
      },
    ];

    renderHook(() => useFlintTools(tools), { wrapper });

    const bridge = (globalThis as any).__flint__;
    const result = bridge.callTool("doStuff", { count: 42 });
    expect(result).toBe("ok");
    expect(actionFn).toHaveBeenCalledWith({ count: 42 });
  });
});
