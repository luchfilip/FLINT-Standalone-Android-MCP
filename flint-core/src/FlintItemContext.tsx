import React, { createContext, useContext } from "react";

type FlintItemContextValue = { listId: string; index: number };

const FlintItemCtx = createContext<FlintItemContextValue | null>(null);

export function useFlintItemContext(): FlintItemContextValue | null {
  return useContext(FlintItemCtx);
}

export function FlintItem({
  list,
  index,
  children,
}: {
  list: string;
  index: number;
  children: React.ReactNode;
}) {
  return (
    <FlintItemCtx.Provider value={{ listId: list, index }}>
      {children}
    </FlintItemCtx.Provider>
  );
}
