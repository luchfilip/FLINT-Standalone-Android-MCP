export interface FlintPluginConfig {
  /** Phone IP address where Flint Hub is running */
  host: string;
  /** MCP server port (default: 8080) */
  port: number;
  /** Bearer token for authentication */
  authToken?: string;
  /** Prefix tool names with flint_ (default: true) */
  toolPrefix: boolean;
  /** Milliseconds between reconnection attempts (default: 5000) */
  reconnectIntervalMs: number;
}

export function parseConfig(raw: Record<string, unknown>): FlintPluginConfig {
  const host = raw.host;
  if (typeof host !== "string" || host.length === 0) {
    throw new Error(
      "Flint plugin: 'host' is required. Set it to your phone's IP address."
    );
  }

  return {
    host,
    port: typeof raw.port === "number" ? raw.port : 8080,
    authToken:
      typeof raw.authToken === "string" && raw.authToken.length > 0
        ? raw.authToken
        : undefined,
    toolPrefix: raw.toolPrefix !== false,
    reconnectIntervalMs:
      typeof raw.reconnectIntervalMs === "number"
        ? raw.reconnectIntervalMs
        : 5000,
  };
}
