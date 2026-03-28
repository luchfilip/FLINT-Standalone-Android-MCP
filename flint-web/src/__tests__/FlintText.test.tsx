(global as any).__DEV__ = true;

import "@testing-library/jest-dom";
import React from "react";
import { render, screen } from "@testing-library/react";
import { FlintProvider, FlintRegistry } from "flint-core";
import { FlintText } from "../FlintText";

// Helper to access the registry from the bridge
function getRegistry(): any {
  return (globalThis as any).__flint__;
}

function wrapper({ children }: { children: React.ReactNode }) {
  return <FlintProvider>{children}</FlintProvider>;
}

describe("FlintText", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("renders text content as a span", () => {
    render(
      <FlintText flintKey="title">Hello World</FlintText>,
      { wrapper }
    );
    expect(screen.getByText("Hello World")).toBeInTheDocument();
    expect(screen.getByText("Hello World").tagName).toBe("SPAN");
  });

  it("registers content in the flint registry", () => {
    render(
      <FlintText flintKey="heading">My Heading</FlintText>,
      { wrapper }
    );
    const output = getRegistry().readScreen();
    expect(output).toContain("heading: My Heading");
  });

  it("passes through HTML attributes", () => {
    render(
      <FlintText flintKey="info" className="text-bold" data-testid="info-text">
        Info
      </FlintText>,
      { wrapper }
    );
    const el = screen.getByTestId("info-text");
    expect(el.className).toBe("text-bold");
  });

  it("unregisters content on unmount", () => {
    const { unmount } = render(
      <FlintText flintKey="temp">Temporary</FlintText>,
      { wrapper }
    );
    const bridge = getRegistry();
    expect(bridge.readScreen()).toContain("temp: Temporary");
    unmount();
  });
});
