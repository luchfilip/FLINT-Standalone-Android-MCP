import {
  FlintScreenSnapshot,
  FlintElement,
  FlintListItem,
  FlintOverlay,
} from "./types";

export function renderSnapshot(
  snapshot: FlintScreenSnapshot,
  toolNames: string[]
): string {
  const lines: string[] = [];

  lines.push(`screen: ${snapshot.screen}`);

  for (const element of snapshot.content.elements) {
    renderElement(element, lines);
  }

  for (const overlay of snapshot.overlays) {
    renderOverlay(overlay, lines);
  }

  if (toolNames.length > 0) {
    lines.push(`tools: ${toolNames.join(", ")}`);
  }

  return lines.join("\n").trimEnd();
}

function renderElement(element: FlintElement, lines: string[]): void {
  switch (element.type) {
    case "content":
      lines.push(`${element.key}: ${element.value}`);
      break;
    case "list":
      lines.push(`${element.id}:`);
      for (const item of element.items) {
        renderListItem(item, lines);
      }
      break;
    case "action":
      // Action elements are not rendered in text output
      break;
  }
}

function renderListItem(item: FlintListItem, lines: string[]): void {
  const parts = Object.entries(item.content).map(
    ([key, value]) => `${key}: ${value}`
  );
  lines.push(`  [${item.index}] ${parts.join(" | ")}`);
}

function renderOverlay(overlay: FlintOverlay, lines: string[]): void {
  lines.push(`overlay(${overlay.id}):`);
  for (const [key, value] of Object.entries(overlay.content)) {
    lines.push(`  ${key}: ${value}`);
  }
  if (overlay.actions.length > 0) {
    lines.push(`  actions: ${overlay.actions.map((a) => a.name).join(", ")}`);
  }
}
