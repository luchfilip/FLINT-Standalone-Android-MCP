import { useEffect, useRef } from "react";
import { useFlintRegistry } from "../FlintProvider";
import { FlintToolDef } from "../types";

export function useFlintTools(tools: FlintToolDef[]): void {
  const registry = useFlintRegistry();
  const toolsRef = useRef(tools);
  toolsRef.current = tools;

  useEffect(() => {
    const cleanup = registry.registerTools(toolsRef.current);
    return cleanup;
  }, [registry]);
}
