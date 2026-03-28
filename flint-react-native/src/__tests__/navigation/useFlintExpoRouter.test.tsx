(global as any).__DEV__ = true;

import React from "react";
import { renderHook } from "@testing-library/react";
import { FlintProvider } from "../../FlintProvider";
import { useFlintExpoRouter } from "../../navigation/useFlintExpoRouter";
import { __setPathname } from "../../__mocks__/expo-router";

function wrapper({ children }: { children: React.ReactNode }) {
  return <FlintProvider>{children}</FlintProvider>;
}

describe("useFlintExpoRouter", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
    __setPathname("/");
  });

  it("sets screen from pathname", () => {
    __setPathname("/home");
    renderHook(() => useFlintExpoRouter(), { wrapper });

    expect((globalThis as any).__flint__.getScreen()).toBe("/home");
  });

  it("clears and updates on pathname change", () => {
    __setPathname("/home");
    const { rerender } = renderHook(() => useFlintExpoRouter(), { wrapper });

    expect((globalThis as any).__flint__.getScreen()).toBe("/home");

    __setPathname("/settings");
    rerender({});

    expect((globalThis as any).__flint__.getScreen()).toBe("/settings");
  });
});
