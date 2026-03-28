(global as any).__DEV__ = true;

import React from "react";
import { render, act } from "@testing-library/react";
import { MemoryRouter, Routes, Route, useNavigate } from "react-router-dom";
import { FlintProvider } from "flint-core";
import { FlintRouter } from "../FlintRouter";

function getRegistry(): any {
  return (globalThis as any).__flint__;
}

describe("FlintRouter", () => {
  afterEach(() => {
    delete (globalThis as any).__flint__;
  });

  it("sets screen name from pathname", () => {
    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/home"]}>
          <FlintRouter>
            <Routes>
              <Route path="/home" element={<div>Home</div>} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );
    expect(getRegistry().getScreen()).toBe("home");
  });

  it("sets screen to 'home' for root path", () => {
    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/"]}>
          <FlintRouter>
            <Routes>
              <Route path="/" element={<div>Root</div>} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );
    expect(getRegistry().getScreen()).toBe("home");
  });

  it("converts nested paths to underscored screen names", () => {
    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/search/results"]}>
          <FlintRouter>
            <Routes>
              <Route path="/search/results" element={<div>Results</div>} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );
    expect(getRegistry().getScreen()).toBe("search_results");
  });

  it("updates screen on navigation", () => {
    function NavButton() {
      const navigate = useNavigate();
      return <button onClick={() => navigate("/about")}>Go</button>;
    }

    const { getByText } = render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/home"]}>
          <FlintRouter>
            <NavButton />
            <Routes>
              <Route path="/home" element={<div>Home</div>} />
              <Route path="/about" element={<div>About</div>} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );

    expect(getRegistry().getScreen()).toBe("home");

    act(() => {
      getByText("Go").click();
    });

    expect(getRegistry().getScreen()).toBe("about");
  });

  it("respects custom screenNameMap", () => {
    render(
      <FlintProvider>
        <MemoryRouter initialEntries={["/dashboard"]}>
          <FlintRouter screenNameMap={{ "/dashboard": "main_dashboard" }}>
            <Routes>
              <Route path="/dashboard" element={<div>Dashboard</div>} />
            </Routes>
          </FlintRouter>
        </MemoryRouter>
      </FlintProvider>
    );
    expect(getRegistry().getScreen()).toBe("main_dashboard");
  });
});
