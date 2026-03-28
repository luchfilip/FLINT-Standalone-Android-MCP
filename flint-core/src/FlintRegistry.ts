import {
  FlintAction,
  FlintElement,
  FlintOverlay,
  FlintScreenSnapshot,
  FlintToolDef,
} from "./types";

type ContentEntry = {
  key: string;
  value: string;
  listId?: string;
  itemIndex?: number;
  screen?: string;
};

type ActionEntry = {
  name: string;
  description: string;
  handler: () => void;
  listId?: string;
  itemIndex?: number;
  screen?: string;
};

type ListEntry = {
  id: string;
  description: string;
  screen?: string;
};

type OverlayEntry = {
  id: string;
  description: string;
  content: Map<string, string>;
  actions: FlintAction[];
  screen?: string;
};

type StoredTool = FlintToolDef & { _screen?: string };

export class FlintRegistry {
  private _screen: string | null = null;
  private _screenListeners: Set<() => void> = new Set();
  // Maps React Navigation route names to useFlintScreen custom names.
  // e.g., "Home" -> "home", "SearchResults" -> "search_results"
  private _routeNameMap: Map<string, string> = new Map();
  private _tools: Map<string, StoredTool> = new Map();
  private _content: Map<string, ContentEntry> = new Map();
  private _lists: Map<string, ListEntry> = new Map();
  private _actions: Map<string, ActionEntry> = new Map();
  private _overlays: Map<string, OverlayEntry> = new Map();
  private _idCounter = 0;

  private nextId(): string {
    return `_${this._idCounter++}`;
  }

  setScreen(name: string): void {
    // If a useFlintScreen registered a custom name for this route, use it.
    const resolved = this._routeNameMap.get(name) ?? name;
    if (this._screen === resolved) return;
    this._screen = resolved;
    for (const listener of this._screenListeners) {
      listener();
    }
  }

  clearScreen(name: string): void {
    if (this._screen === name) {
      this._screen = null;
    }
  }

  onScreenChange(listener: () => void): () => void {
    this._screenListeners.add(listener);
    return () => {
      this._screenListeners.delete(listener);
    };
  }

  registerScreenName(routeName: string, customName: string): () => void {
    this._routeNameMap.set(routeName, customName);
    return () => {
      this._routeNameMap.delete(routeName);
    };
  }

  getScreen(): string | null {
    return this._screen;
  }

  registerTools(tools: FlintToolDef[]): () => void {
    const screen = this._screen ?? undefined;
    for (const tool of tools) {
      this._tools.set(tool.name, { ...tool, _screen: screen });
    }
    return () => {
      for (const tool of tools) {
        this._tools.delete(tool.name);
      }
    };
  }

  getTools(): FlintToolDef[] {
    return Array.from(this._tools.values())
      .filter(t => !t._screen || t._screen === this._screen);
  }

  getToolNames(): string[] {
    return Array.from(this._tools.entries())
      .filter(([_, t]) => !t._screen || t._screen === this._screen)
      .map(([name]) => name);
  }

  callTool(name: string, params: Record<string, any>): boolean {
    const tool = this._tools.get(name);
    if (!tool) return false;
    tool.action(params);
    return true;
  }

  registerContent(entry: ContentEntry): () => void {
    const id = this.nextId();
    const tagged = { ...entry, screen: this._screen ?? undefined };
    this._content.set(id, tagged);
    return () => {
      this._content.delete(id);
    };
  }

  registerList(entry: ListEntry): () => void {
    const id = this.nextId();
    const tagged = { ...entry, screen: this._screen ?? undefined };
    this._lists.set(entry.id, tagged);
    return () => {
      this._lists.delete(entry.id);
    };
  }

  registerAction(entry: ActionEntry): () => void {
    const id = this.nextId();
    const tagged = { ...entry, screen: this._screen ?? undefined };
    this._actions.set(id, tagged);
    return () => {
      this._actions.delete(id);
    };
  }

  registerOverlay(entry: OverlayEntry): () => void {
    const tagged = { ...entry, screen: this._screen ?? undefined };
    this._overlays.set(entry.id, tagged);
    return () => {
      this._overlays.delete(entry.id);
    };
  }

  invokeAction(name: string, listId?: string, itemIndex?: number): boolean {
    for (const entry of this._actions.values()) {
      if (
        entry.name === name &&
        entry.listId === listId &&
        entry.itemIndex === itemIndex
      ) {
        entry.handler();
        return true;
      }
    }
    return false;
  }

  snapshot(): FlintScreenSnapshot {
    const elements: FlintElement[] = [];

    const isCurrentScreen = (screen?: string) =>
      screen === undefined || screen === null || screen === this._screen;

    // Standalone content (no listId)
    for (const entry of this._content.values()) {
      if (!isCurrentScreen(entry.screen)) continue;
      if (!entry.listId) {
        elements.push({ type: "content", key: entry.key, value: entry.value });
      }
    }

    // Lists with their items
    for (const list of this._lists.values()) {
      if (!isCurrentScreen(list.screen)) continue;

      const itemMap = new Map<
        number,
        { content: Record<string, string>; actions: FlintAction[] }
      >();

      // Gather content for this list
      for (const entry of this._content.values()) {
        if (!isCurrentScreen(entry.screen)) continue;
        if (entry.listId === list.id && entry.itemIndex !== undefined) {
          if (!itemMap.has(entry.itemIndex)) {
            itemMap.set(entry.itemIndex, { content: {}, actions: [] });
          }
          itemMap.get(entry.itemIndex)!.content[entry.key] = entry.value;
        }
      }

      // Gather actions for this list
      for (const entry of this._actions.values()) {
        if (!isCurrentScreen(entry.screen)) continue;
        if (entry.listId === list.id && entry.itemIndex !== undefined) {
          if (!itemMap.has(entry.itemIndex)) {
            itemMap.set(entry.itemIndex, { content: {}, actions: [] });
          }
          itemMap.get(entry.itemIndex)!.actions.push({
            name: entry.name,
            description: entry.description,
          });
        }
      }

      // Sort items by index
      const sortedIndices = Array.from(itemMap.keys()).sort((a, b) => a - b);
      const items = sortedIndices.map((index) => ({
        index,
        content: itemMap.get(index)!.content,
        actions: itemMap.get(index)!.actions,
      }));

      elements.push({
        type: "list",
        id: list.id,
        description: list.description,
        items,
      });
    }

    // Standalone actions (no listId)
    for (const entry of this._actions.values()) {
      if (!isCurrentScreen(entry.screen)) continue;
      if (!entry.listId) {
        elements.push({
          type: "action",
          name: entry.name,
          description: entry.description,
        });
      }
    }

    // Overlays
    const overlays: FlintOverlay[] = [];
    for (const entry of this._overlays.values()) {
      if (!isCurrentScreen(entry.screen)) continue;
      const content: Record<string, string> = {};
      for (const [k, v] of entry.content) {
        content[k] = v;
      }
      overlays.push({
        id: entry.id,
        description: entry.description,
        content,
        actions: entry.actions,
      });
    }

    return {
      screen: this._screen ?? "",
      content: { elements },
      overlays,
    };
  }

  clearScreenState(): void {
    this._screen = null;
    this._content.clear();
    this._lists.clear();
    this._actions.clear();
    this._overlays.clear();
  }

  clearAll(): void {
    this.clearScreenState();
    this._tools.clear();
  }
}
