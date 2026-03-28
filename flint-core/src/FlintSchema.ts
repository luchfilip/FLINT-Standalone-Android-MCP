import { FlintRegistry } from "./FlintRegistry";
import { FlintSchema } from "./types";

export function generateSchema(registry: FlintRegistry): FlintSchema {
  return {
    protocol: "flint",
    version: "2.0",
    tools: registry.getTools().map((tool) => ({
      name: tool.name,
      description: tool.description,
      inputSchema: {
        type: "object" as const,
        properties: Object.fromEntries(
          (tool.params ?? []).map((p) => [
            p.name,
            { type: p.type, description: p.description },
          ])
        ),
        required: (tool.params ?? [])
          .filter((p) => p.required !== false)
          .map((p) => p.name),
      },
    })),
  };
}
