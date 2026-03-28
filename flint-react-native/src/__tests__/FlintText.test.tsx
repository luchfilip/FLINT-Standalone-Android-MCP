(global as any).__DEV__ = true;

import React from "react";
import { render, screen } from "@testing-library/react";
import { FlintProvider, useFlintRegistry } from "../FlintProvider";
import { FlintItem } from "../FlintItemContext";
import { FlintText } from "../FlintText";
import { FlintRegistry } from "../FlintRegistry";

describe("FlintText", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  function captureRegistry(): { ref: FlintRegistry | null } {
    const holder = { ref: null as FlintRegistry | null };
    function Capture() {
      holder.ref = useFlintRegistry();
      return null;
    }
    return holder;
  }

  it("registers content with key and value", () => {
    let registry: FlintRegistry | null = null;

    function Grab() {
      registry = useFlintRegistry();
      return null;
    }

    render(
      <FlintProvider>
        <Grab />
        <FlintText flintKey="title">Hello World</FlintText>
      </FlintProvider>
    );

    expect(registry).not.toBeNull();
    const snap = registry!.snapshot();
    const content = snap.content.elements.find(
      (e) => e.type === "content" && e.key === "title"
    );
    expect(content).toBeDefined();
    expect(content).toMatchObject({ type: "content", key: "title", value: "Hello World" });
  });

  it("registers with list context when inside FlintItem", () => {
    let registry: FlintRegistry | null = null;

    function Grab() {
      registry = useFlintRegistry();
      return null;
    }

    render(
      <FlintProvider>
        <Grab />
        <FlintItem list="tasks" index={2}>
          <FlintText flintKey="name">Task Name</FlintText>
        </FlintItem>
      </FlintProvider>
    );

    expect(registry).not.toBeNull();
    const snap = registry!.snapshot();
    // Content inside a list item should NOT appear as standalone content
    const standalone = snap.content.elements.find(
      (e) => e.type === "content" && e.key === "name"
    );
    expect(standalone).toBeUndefined();
  });

  it("cleans up on unmount", () => {
    let registry: FlintRegistry | null = null;

    function Grab() {
      registry = useFlintRegistry();
      return null;
    }

    const { unmount } = render(
      <FlintProvider>
        <Grab />
        <FlintText flintKey="temp">Temporary</FlintText>
      </FlintProvider>
    );

    expect(registry).not.toBeNull();
    let snap = registry!.snapshot();
    expect(
      snap.content.elements.some(
        (e) => e.type === "content" && e.key === "temp"
      )
    ).toBe(true);

    unmount();

    snap = registry!.snapshot();
    expect(
      snap.content.elements.some(
        (e) => e.type === "content" && e.key === "temp"
      )
    ).toBe(false);
  });

  it("renders text children", () => {
    render(
      <FlintProvider>
        <FlintText flintKey="greeting">Hello Flint</FlintText>
      </FlintProvider>
    );

    expect(screen.getByText("Hello Flint")).toBeTruthy();
  });
});
