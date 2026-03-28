import { useLayoutEffect } from "react";
import { useFlintRegistry } from "../FlintProvider";

/**
 * Registers a custom screen name for the current screen and maps it to the
 * React Navigation route name so that FlintNavigationContainer can resolve
 * the correct Flint name on back-navigation.
 *
 * @param name - The Flint screen name (e.g. "home", "search_results")
 * @param routeName - The React Navigation route name (e.g. "Home", "SearchResults").
 *                    Required when using FlintNavigationContainer and the route
 *                    name differs from the Flint screen name.
 */
export function useFlintScreen(name: string, routeName?: string): void {
  const registry = useFlintRegistry();

  useLayoutEffect(() => {
    registry.setScreen(name);

    // Map the React Navigation route name to the custom Flint screen name.
    // When FlintNavigationContainer calls setScreen("Home") on back-nav,
    // the registry resolves it to "home" via this mapping.
    const unregister = routeName
      ? registry.registerScreenName(routeName, name)
      : undefined;

    return () => {
      unregister?.();
      registry.clearScreen(name);
    };
  }, [name, routeName, registry]);
}
