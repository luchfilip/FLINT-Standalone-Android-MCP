// Core
export { FlintProvider } from "./FlintProvider";
export { FlintRegistry } from "./FlintRegistry";

// Hooks
export { useFlintScreen } from "./hooks/useFlintScreen";
export { useFlintTools } from "./hooks/useFlintTools";
export { useFlintList } from "./hooks/useFlintList";

// Components
export { FlintText } from "./FlintText";
export { FlintItem } from "./FlintItemContext";
export { FlintAction } from "./FlintAction";

// Navigation
export { FlintNavigationContainer } from "./navigation/FlintNavigationContainer";
// useFlintExpoRouter is available via "flint-react-native/expo-router" to avoid
// pulling in expo-router as a hard dependency for non-Expo projects.

// Types
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
  FlintAction as FlintActionType,
  FlintOverlay,
  FlintToolSchema,
} from "./types";
