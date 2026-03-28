import React, { createContext, useContext, useState, useEffect } from "react";
import { FlintRegistry } from "./FlintRegistry";
import { exposeBridge } from "./FlintBridge";

const FlintRegistryContext = createContext<FlintRegistry | null>(null);

export function useFlintRegistry(): FlintRegistry {
  const registry = useContext(FlintRegistryContext);
  if (!registry) {
    throw new Error("useFlintRegistry must be used within <FlintProvider>");
  }
  return registry;
}

export function FlintProvider({ children }: { children: React.ReactNode }) {
  const [registry] = useState(() => new FlintRegistry());

  useEffect(() => {
    const cleanup = exposeBridge(registry);
    return cleanup;
  }, [registry]);

  return (
    <FlintRegistryContext.Provider value={registry}>
      {children}
    </FlintRegistryContext.Provider>
  );
}
