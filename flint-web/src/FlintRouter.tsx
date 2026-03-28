import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useFlintRegistry } from "flint-core";

type FlintRouterProps = {
  children: React.ReactNode;
  screenNameMap?: Record<string, string>;
};

export function FlintRouter({ children, screenNameMap }: FlintRouterProps) {
  const location = useLocation();
  const registry = useFlintRegistry();

  useEffect(() => {
    const pathname = location.pathname;

    // Check custom map first
    if (screenNameMap && screenNameMap[pathname]) {
      registry.setScreen(screenNameMap[pathname]);
      return;
    }

    // Derive screen name: /search/results → search_results, / → home
    const screen =
      pathname === "/" ? "home" : pathname.slice(1).replace(/\//g, "_");

    registry.setScreen(screen);
  }, [location.pathname, screenNameMap, registry]);

  return <>{children}</>;
}
