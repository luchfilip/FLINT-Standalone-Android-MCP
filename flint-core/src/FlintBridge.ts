import { FlintRegistry } from "./FlintRegistry";
import { renderSnapshot } from "./FlintTextRenderer";
import { generateSchema } from "./FlintSchema";

function isProduction(): boolean {
  // React Native sets globalThis.__DEV__ = false in production
  if ((globalThis as any).__DEV__ === false) return true;
  // Node/Webpack/Vite set NODE_ENV
  try {
    if (typeof process !== "undefined" && process.env?.NODE_ENV === "production") return true;
  } catch {}
  return false;
}

export function exposeBridge(registry: FlintRegistry): () => void {
  if (isProduction()) {
    return () => {};
  }

  (globalThis as any).__flint__ = {
    readScreen(): string {
      const snapshot = registry.snapshot();
      const toolNames = registry.getToolNames();
      return renderSnapshot(snapshot, toolNames);
    },

    getScreen(): string {
      return registry.getScreen() ?? "";
    },

    getSchema(): string {
      return JSON.stringify(generateSchema(registry));
    },

    callTool(name: string, params?: Record<string, any>): string {
      const ok = registry.callTool(name, params ?? {});
      return ok ? "ok" : "error: unknown tool";
    },

    invokeAction(name: string, listId?: string, itemIndex?: number): string {
      const ok = registry.invokeAction(name, listId, itemIndex);
      return ok ? "ok" : "error: action not found";
    },
  };

  return () => {
    delete (globalThis as any).__flint__;
  };
}
