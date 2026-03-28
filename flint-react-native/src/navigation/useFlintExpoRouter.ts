import { useEffect } from "react";
import { usePathname } from "expo-router";
import { useFlintRegistry } from "../FlintProvider";

export function useFlintExpoRouter(): void {
  const pathname = usePathname();
  const registry = useFlintRegistry();

  useEffect(() => {
    registry.clearScreenState();
    registry.setScreen(pathname);
  }, [pathname, registry]);
}
