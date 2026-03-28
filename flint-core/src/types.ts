export type FlintScreenSnapshot = {
  screen: string;
  content: FlintContent;
  overlays: FlintOverlay[];
};

export type FlintContent = {
  elements: FlintElement[];
};

export type FlintElement =
  | FlintContentElement
  | FlintListElement
  | FlintActionElement;

export type FlintContentElement = {
  type: "content";
  key: string;
  value: string;
};

export type FlintListElement = {
  type: "list";
  id: string;
  description: string;
  items: FlintListItem[];
};

export type FlintActionElement = {
  type: "action";
  name: string;
  description: string;
};

export type FlintListItem = {
  index: number;
  content: Record<string, string>;
  actions: FlintAction[];
};

export type FlintAction = {
  name: string;
  description: string;
};

export type FlintOverlay = {
  id: string;
  description: string;
  content: Record<string, string>;
  actions: FlintAction[];
};

export type FlintToolDef = {
  name: string;
  description: string;
  params?: FlintToolParam[];
  action: (params: Record<string, any>) => void;
};

export type FlintToolParam = {
  name: string;
  type: "string" | "integer" | "number" | "boolean";
  description: string;
  required?: boolean;
};

export type FlintSchema = {
  protocol: "flint";
  version: "2.0";
  tools: FlintToolSchema[];
};

export type FlintToolSchema = {
  name: string;
  description: string;
  inputSchema: {
    type: "object";
    properties: Record<string, { type: string; description: string }>;
    required: string[];
  };
};
