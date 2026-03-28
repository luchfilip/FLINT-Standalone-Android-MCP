declare module "@react-navigation/native" {
  import React from "react";

  export interface NavigationContainerProps {
    ref?: any;
    onReady?: () => void;
    onStateChange?: () => void;
    children?: React.ReactNode;
    [key: string]: any;
  }

  export const NavigationContainer: React.FC<NavigationContainerProps>;

  export function useNavigationContainerRef(): {
    current: {
      getCurrentRoute: () => { name: string } | undefined;
    } | null;
  };
}
