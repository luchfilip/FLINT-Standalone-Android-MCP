// Re-export everything from flint-core
export {
  FlintProvider,
  FlintRegistry,
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintItem,
  useFlintRegistry,
  useFlintItemContext,
} from "flint-core";

// Re-export types from flint-core
export type {
  FlintToolDef,
  FlintToolParam,
  FlintScreenSnapshot,
  FlintSchema,
  FlintContent,
  FlintElement,
  FlintContentElement,
  FlintListElement,
  FlintActionElement,
  FlintListItem,
  FlintActionType,
  FlintOverlay,
  FlintToolSchema,
} from "flint-core";

// Web-specific components
export { FlintText } from "./FlintText";
export { FlintAction } from "./FlintAction";
export { FlintRouter } from "./FlintRouter";
