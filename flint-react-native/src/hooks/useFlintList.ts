import { useEffect } from "react";
import { useFlintRegistry } from "../FlintProvider";

export function useFlintList(id: string, description?: string): void {
  const registry = useFlintRegistry();
  useEffect(() => {
    const cleanup = registry.registerList({ id, description: description ?? id });
    return cleanup;
  }, [id, description, registry]);
}
