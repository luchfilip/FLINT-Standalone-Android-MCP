import type { FlintClient } from "./flint-client.js";

export interface HealthStatus {
  connected: boolean;
  serverUrl: string;
  health?: Record<string, string>;
  toolCount?: number;
}

export async function checkFlintStatus(
  client: FlintClient,
  toolCount: number
): Promise<HealthStatus> {
  const status: HealthStatus = {
    connected: client.connected,
    serverUrl: client.serverUrl,
    toolCount,
  };

  if (client.connected) {
    const health = await client.checkHealth();
    if (health.ok && health.info) {
      status.health = health.info;
    }
  }

  return status;
}

export function formatHealthStatus(status: HealthStatus): string {
  const lines: string[] = [];

  if (status.connected) {
    lines.push(`Connected to Flint Hub at ${status.serverUrl}`);
    lines.push(`Tools available: ${status.toolCount ?? "unknown"}`);
    if (status.health) {
      lines.push(`Server status: ${status.health.status ?? "unknown"}`);
    }
  } else {
    lines.push(`Disconnected from Flint Hub (${status.serverUrl})`);
    lines.push("Run /flint to check status or restart the plugin.");
  }

  return lines.join("\n");
}
