(global as any).__DEV__ = true;

import React from "react";
import { render, renderHook, act } from "@testing-library/react";
import { FlintProvider, useFlintRegistry } from "../FlintProvider";
import { FlintRegistry } from "../FlintRegistry";

describe("FlintProvider", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("provides registry to children", () => {
    let captured: FlintRegistry | null = null;

    function Child() {
      captured = useFlintRegistry();
      return null;
    }

    render(
      <FlintProvider>
        <Child />
      </FlintProvider>
    );

    expect(captured).toBeInstanceOf(FlintRegistry);
  });

  it("throws without provider", () => {
    const spy = jest.spyOn(console, "error").mockImplementation(() => {});

    function Bad() {
      useFlintRegistry();
      return null;
    }

    expect(() => render(<Bad />)).toThrow(
      "useFlintRegistry must be used within <FlintProvider>"
    );

    spy.mockRestore();
  });

  it("bridge exposed on mount", () => {
    render(
      <FlintProvider>
        <div />
      </FlintProvider>
    );

    expect((globalThis as any).__flint__).toBeDefined();
    expect(typeof (globalThis as any).__flint__.readScreen).toBe("function");
    expect(typeof (globalThis as any).__flint__.getScreen).toBe("function");
    expect(typeof (globalThis as any).__flint__.callTool).toBe("function");
  });

  it("bridge cleaned on unmount", () => {
    const { unmount } = render(
      <FlintProvider>
        <div />
      </FlintProvider>
    );

    expect((globalThis as any).__flint__).toBeDefined();

    unmount();

    expect((globalThis as any).__flint__).toBeUndefined();
  });
});
