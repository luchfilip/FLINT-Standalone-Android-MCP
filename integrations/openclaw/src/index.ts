import { FlintClient } from "./flint-client.js";
import { ToolBridge } from "./tool-bridge.js";
import { parseConfig } from "./config.js";
import { checkFlintStatus, formatHealthStatus } from "./health.js";

/**
 * OpenClaw plugin API surface. OpenClaw passes this to register().
 * Types are intentionally loose since OpenClaw doesn't ship official type defs.
 */
interface OpenClawPluginApi {
  pluginConfig: Record<string, unknown>;
  registerTool(def: {
    name: string;
    description: string;
    inputSchema: Record<string, unknown>;
    execute: (args: Record<string, unknown>) => Promise<unknown>;
  }): void;
  registerCommand(def: {
    name: string;
    description: string;
    execute: () => Promise<string>;
  }): void;
  registerService(def: {
    name: string;
    start: () => Promise<void>;
    stop: () => Promise<void>;
  }): void;
  log(level: string, message: string): void;
}

let client: FlintClient | null = null;
let bridge: ToolBridge | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let toolCount = 0;

export function register(api: OpenClawPluginApi): void {
  const config = parseConfig(api.pluginConfig);
  client = new FlintClient(config);
  bridge = new ToolBridge(client, config);

  // /flint status command
  api.registerCommand({
    name: "flint",
    description: "Show Flint Hub connection status",
    execute: async () => {
      const status = await checkFlintStatus(client!, toolCount);
      return formatHealthStatus(status);
    },
  });

  // Background service for connection lifecycle
  api.registerService({
    name: "flint-hub",
    start: async () => {
      await connectAndDiscover(api, config.reconnectIntervalMs);
    },
    stop: async () => {
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
      bridge?.clear();
      toolCount = 0;
      await client?.disconnect();
      api.log("info", "Flint Hub disconnected");
    },
  });
}

async function connectAndDiscover(
  api: OpenClawPluginApi,
  reconnectMs: number
): Promise<void> {
  try {
    await client!.connect();
    api.log("info", `Connected to Flint Hub at ${client!.serverUrl}`);

    const tools = await bridge!.discover();
    toolCount = tools.length;
    for (const tool of tools) {
      api.registerTool(tool);
    }
    api.log("info", `Registered ${tools.length} Flint tools`);

    // Listen for new Flint-enabled apps
    client!.onToolsChanged(async () => {
      api.log("info", "Flint tool list changed, re-discovering...");
      const newTools = await bridge!.refresh();
      for (const tool of newTools) {
        api.registerTool(tool);
      }
      toolCount += newTools.length;
      api.log("info", `Registered ${newTools.length} new Flint tools`);
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    api.log("warn", `Flint Hub connection failed: ${message}`);
    api.log("info", `Retrying in ${reconnectMs / 1000}s...`);
    scheduleReconnect(api, reconnectMs);
  }
}

function scheduleReconnect(
  api: OpenClawPluginApi,
  reconnectMs: number
): void {
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connectAndDiscover(api, reconnectMs);
  }, reconnectMs);
}
