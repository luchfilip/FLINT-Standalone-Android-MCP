(global as any).__DEV__ = true;

import React from "react";
import { render, screen, act } from "@testing-library/react";
import { MemoryRouter, Routes, Route, useNavigate } from "react-router-dom";
import {
  FlintProvider,
  FlintRouter,
  FlintText,
  FlintAction,
  FlintItem,
  useFlintScreen,
  useFlintTools,
  useFlintList,
} from "../index";

function getBridge(): any {
  return (globalThis as any).__flint__;
}

function HomeScreen() {
  useFlintScreen("home");
  useFlintList("items", "Shopping items");
  useFlintTools([
    {
      name: "search",
      description: "Search items",
      params: [{ name: "query", type: "string" as const, description: "Search query", required: true }],
      action: () => {},
    },
  ]);

  return (
    <div>
      <FlintText flintKey="heading">My Store</FlintText>
      <FlintItem list="items" index={0}>
        <FlintText flintKey="name">Widget A</FlintText>
        <FlintText flintKey="price">$10</FlintText>
        <FlintAction flintName="buy" flintDescription="Buy this item" onClick={() => {}}>
          Buy
        </FlintAction>
      </FlintItem>
      <FlintItem list="items" index={1}>
        <FlintText flintKey="name">Widget B</FlintText>
        <FlintText flintKey="price">$20</FlintText>
        <FlintAction flintName="buy" flintDescription="Buy this item" onClick={() => {}}>
          Buy
        </FlintAction>
      </FlintItem>
      <FlintAction flintName="checkout" flintDescription="Go to checkout" onClick={() => {}}>
        Checkout
      </FlintAction>
    </div>
  );
}

function AboutScreen() {
  useFlintScreen("about");
  return (
    <div>
      <FlintText flintKey="title">About Us</FlintText>
    </div>
  );
}

describe("flint-web integration", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("produces correct readScreen output for a full page", () => {
    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/home"]}>
          <FlintRouter>
            <Routes>
              <Route path="/home" element={<HomeScreen />} />
              <Route path="/about" element={<AboutScreen />} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );

    const output = getBridge().readScreen();
    expect(output).toContain("screen: home");
    expect(output).toContain("heading: My Store");
    expect(output).toContain("items:");
    expect(output).toContain("name: Widget A");
    expect(output).toContain("price: $10");
    expect(output).toContain("name: Widget B");
    expect(output).toContain("price: $20");
    expect(output).toContain("tools: search");
  });

  it("schema includes tool definitions", () => {
    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/home"]}>
          <FlintRouter>
            <Routes>
              <Route path="/home" element={<HomeScreen />} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );

    const schema = JSON.parse(getBridge().getSchema());
    expect(schema.protocol).toBe("flint");
    expect(schema.tools).toHaveLength(1);
    expect(schema.tools[0].name).toBe("search");
    expect(schema.tools[0].inputSchema.properties.query).toBeDefined();
  });

  it("actions are invocable through the bridge", () => {
    const checkoutHandler = jest.fn();

    function HomeWithHandler() {
      useFlintScreen("home");
      return (
        <FlintAction flintName="checkout" flintDescription="Go to checkout" onClick={checkoutHandler}>
          Checkout
        </FlintAction>
      );
    }

    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/home"]}>
          <FlintRouter>
            <Routes>
              <Route path="/home" element={<HomeWithHandler />} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );

    const result = getBridge().invokeAction("checkout");
    expect(result).toBe("ok");
    expect(checkoutHandler).toHaveBeenCalled();
  });
});
