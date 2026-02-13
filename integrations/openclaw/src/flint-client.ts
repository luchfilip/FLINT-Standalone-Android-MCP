import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { SSEClientTransport } from "@modelcontextprotocol/sdk/client/sse.js";
import { ToolListChangedNotificationSchema } from "@modelcontextprotocol/sdk/types.js";
import type { FlintPluginConfig } from "./config.js";

export interface FlintTool {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
}

export interface FlintToolResult {
  content: Array<{
    type: string;
    text?: string;
    data?: string;
    mimeType?: string;
  }>;
  isError?: boolean;
}

export class FlintClient {
  private client: Client | null = null;
  private transport: SSEClientTransport | null = null;
  private _connected = false;

  constructor(private config: FlintPluginConfig) {}

  get connected(): boolean {
    return this._connected;
  }

  get serverUrl(): string {
    return `http://${this.config.host}:${this.config.port}`;
  }

  async connect(): Promise<void> {
    const url = new URL("/sse", this.serverUrl);

    const headers: Record<string, string> = {};
    if (this.config.authToken) {
      headers["Authorization"] = `Bearer ${this.config.authToken}`;
    }

    this.transport = new SSEClientTransport(url, {
      requestInit: { headers },
    });

    this.client = new Client({
      name: "openclaw-flint",
      version: "0.1.0",
    });

    await this.client.connect(this.transport);
    this._connected = true;
  }

  async disconnect(): Promise<void> {
    this._connected = false;
    if (this.client) {
      await this.client.close();
      this.client = null;
      this.transport = null;
    }
  }

  async listTools(): Promise<FlintTool[]> {
    if (!this.client) throw new Error("Not connected to Flint Hub");
    const result = await this.client.listTools();
    return result.tools.map((t) => ({
      name: t.name,
      description: t.description ?? "",
      inputSchema: t.inputSchema as Record<string, unknown>,
    }));
  }

  async callTool(
    name: string,
    args: Record<string, unknown>
  ): Promise<FlintToolResult> {
    if (!this.client) throw new Error("Not connected to Flint Hub");
    const result = await this.client.callTool({ name, arguments: args });
    return {
      content: (result.content as FlintToolResult["content"]) ?? [],
      isError: result.isError === true,
    };
  }

  onToolsChanged(callback: () => void): void {
    if (!this.client) throw new Error("Not connected to Flint Hub");
    this.client.setNotificationHandler(
      ToolListChangedNotificationSchema,
      async () => {
        callback();
      }
    );
  }

  async checkHealth(): Promise<{ ok: boolean; info?: Record<string, string> }> {
    try {
      const resp = await globalThis.fetch(`${this.serverUrl}/health`);
      if (!resp.ok) return { ok: false };
      const data = (await resp.json()) as Record<string, string>;
      return { ok: data.status === "ok", info: data };
    } catch {
      return { ok: false };
    }
  }
}
