import React from "react";
import { render, screen } from "@testing-library/react";
import { FlintItem, useFlintItemContext } from "../FlintItemContext";

function ContextReader() {
  const ctx = useFlintItemContext();
  if (!ctx) return <span>no context</span>;
  return (
    <span>
      listId={ctx.listId} index={ctx.index}
    </span>
  );
}

describe("FlintItemContext", () => {
  it("provides listId and index to children", () => {
    render(
      <FlintItem list="tasks" index={3}>
        <ContextReader />
      </FlintItem>
    );
    expect(screen.getByText("listId=tasks index=3")).toBeTruthy();
  });

  it("children can read context via useFlintItemContext", () => {
    render(
      <FlintItem list="notes" index={7}>
        <ContextReader />
      </FlintItem>
    );
    expect(screen.getByText("listId=notes index=7")).toBeTruthy();
  });

  it("returns null when used outside FlintItem", () => {
    render(<ContextReader />);
    expect(screen.getByText("no context")).toBeTruthy();
  });
});
