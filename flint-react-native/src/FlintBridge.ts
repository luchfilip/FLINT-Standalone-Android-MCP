import { FlintRegistry } from "./FlintRegistry";
import { renderSnapshot } from "./FlintTextRenderer";
import { generateSchema } from "./FlintSchema";

export function exposeBridge(registry: FlintRegistry): () => void {
  if (!(globalThis as any).__DEV__) {
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
