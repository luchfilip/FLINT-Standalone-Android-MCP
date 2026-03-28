(global as any).__DEV__ = true;

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { FlintProvider } from "flint-core";
import { FlintAction } from "../FlintAction";

function getRegistry(): any {
  return (globalThis as any).__flint__;
}

function wrapper({ children }: { children: React.ReactNode }) {
  return <FlintProvider>{children}</FlintProvider>;
}

describe("FlintAction", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("renders as a button", () => {
    render(
      <FlintAction flintName="submit" onClick={() => {}}>
        Submit
      </FlintAction>,
      { wrapper }
    );
    const btn = screen.getByText("Submit");
    expect(btn.tagName).toBe("BUTTON");
  });

  it("registers action in the flint registry", () => {
    render(
      <FlintAction flintName="save" flintDescription="Save changes" onClick={() => {}}>
        Save
      </FlintAction>,
      { wrapper }
    );
    const result = getRegistry().invokeAction("save");
    expect(result).toBe("ok");
  });

  it("invokes onClick when pressed", () => {
    const handler = jest.fn();
    render(
      <FlintAction flintName="click_me" onClick={handler}>
        Click
      </FlintAction>,
      { wrapper }
    );
    fireEvent.click(screen.getByText("Click"));
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("action is invocable through the registry", () => {
    const handler = jest.fn();
    render(
      <FlintAction flintName="do_thing" flintDescription="Does the thing" onClick={handler}>
        Do
      </FlintAction>,
      { wrapper }
    );
    const result = getRegistry().invokeAction("do_thing");
    expect(result).toBe("ok");
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
