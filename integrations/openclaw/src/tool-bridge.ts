import type { FlintClient, FlintTool } from "./flint-client.js";
import type { FlintPluginConfig } from "./config.js";

export interface OpenClawToolDef {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
  execute: (args: Record<string, unknown>) => Promise<unknown>;
}

export class ToolBridge {
  private registeredTools = new Set<string>();

  constructor(
    private client: FlintClient,
    private config: FlintPluginConfig
  ) {}

  /** Convert Flint tool names like device.tap to flint_device_tap */
  private mapName(flintName: string): string {
    const normalized = flintName.replace(/\./g, "_");
    return this.config.toolPrefix ? `flint_${normalized}` : normalized;
  }

  /** Build OpenClaw tool definitions from Flint Hub tools */
  async discover(): Promise<OpenClawToolDef[]> {
    const flintTools = await this.client.listTools();
    const defs: OpenClawToolDef[] = [];

    for (const tool of flintTools) {
      const name = this.mapName(tool.name);
      if (this.registeredTools.has(name)) continue;
      this.registeredTools.add(name);
      defs.push(this.buildToolDef(tool, name));
    }

    return defs;
  }

  private buildToolDef(tool: FlintTool, mappedName: string): OpenClawToolDef {
    return {
      name: mappedName,
      description: `[Phone/Flint] ${tool.description}`,
      inputSchema: tool.inputSchema,
      execute: async (args: Record<string, unknown>) => {
        const result = await this.client.callTool(tool.name, args);

        // Flatten single text results for cleaner LLM output
        if (
          result.content.length === 1 &&
          result.content[0].type === "text"
        ) {
          return result.content[0].text;
        }

        return result.content;
      },
    };
  }

  /** Re-discover tools after a list_changed notification */
  async refresh(): Promise<OpenClawToolDef[]> {
    return this.discover();
  }

  clear(): void {
    this.registeredTools.clear();
  }
}
