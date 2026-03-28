(global as any).__DEV__ = true;

import React from "react";
import { render } from "@testing-library/react";
import { FlintProvider, useFlintRegistry } from "../FlintProvider";
import { FlintAction } from "../FlintAction";
import { FlintItem } from "../FlintItemContext";
import { FlintRegistry } from "../FlintRegistry";
import { FlintActionElement } from "../types";

function RegistryCapture({ onCapture }: { onCapture: (r: FlintRegistry) => void }) {
  const registry = useFlintRegistry();
  React.useEffect(() => { onCapture(registry); }, [registry]);
  return null;
}

describe("FlintAction", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("registers action handler", () => {
    let registry: FlintRegistry | null = null;

    render(
      <FlintProvider>
        <RegistryCapture onCapture={(r) => { registry = r; }} />
        <FlintAction flintName="do-stuff" flintDescription="Does stuff" onPress={() => {}} />
      </FlintProvider>
    );

    expect(registry).not.toBeNull();
    const snapshot = registry!.snapshot();
    const action = snapshot.content.elements.find(
      (e) => e.type === "action" && e.name === "do-stuff"
    ) as FlintActionElement | undefined;
    expect(action).toBeDefined();
    expect(action!.description).toBe("Does stuff");
  });

  it("invokes onPress when action triggered programmatically", () => {
    let registry: FlintRegistry | null = null;
    const onPress = jest.fn();

    render(
      <FlintProvider>
        <RegistryCapture onCapture={(r) => { registry = r; }} />
        <FlintAction flintName="tap-me" onPress={onPress} />
      </FlintProvider>
    );

    expect(registry).not.toBeNull();
    const invoked = registry!.invokeAction("tap-me");
    expect(invoked).toBe(true);
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it("scoped by list context when inside FlintItem", () => {
    let registry: FlintRegistry | null = null;
    const onPress = jest.fn();

    render(
      <FlintProvider>
        <RegistryCapture onCapture={(r) => { registry = r; }} />
        <FlintItem list="my-list" index={2}>
          <FlintAction flintName="item-action" onPress={onPress} />
        </FlintItem>
      </FlintProvider>
    );

    expect(registry).not.toBeNull();

    // Should NOT invoke without list scope
    const withoutScope = registry!.invokeAction("item-action");
    expect(withoutScope).toBe(false);

    // Should invoke with correct list scope
    const withScope = registry!.invokeAction("item-action", "my-list", 2);
    expect(withScope).toBe(true);
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it("cleans up on unmount", () => {
    let registry: FlintRegistry | null = null;

    const { unmount } = render(
      <FlintProvider>
        <RegistryCapture onCapture={(r) => { registry = r; }} />
        <FlintAction flintName="temp-action" onPress={() => {}} />
      </FlintProvider>
    );

    expect(registry).not.toBeNull();
    const before = registry!.snapshot();
    expect(before.content.elements.some((e) => e.type === "action" && e.name === "temp-action")).toBe(true);

    unmount();

    const after = registry!.snapshot();
    expect(after.content.elements.some((e) => e.type === "action" && e.name === "temp-action")).toBe(false);
  });
});
