import React, { useRef } from "react";
import {
  NavigationContainer,
  NavigationContainerProps,
  useNavigationContainerRef,
} from "@react-navigation/native";
import { useFlintRegistry } from "../FlintProvider";

export function FlintNavigationContainer({
  children,
  ...props
}: NavigationContainerProps & { children: React.ReactNode }) {
  const navigationRef = useNavigationContainerRef();
  const routeNameRef = useRef<string | undefined>();
  const registry = useFlintRegistry();

  return (
    <NavigationContainer
      ref={navigationRef}
      onReady={() => {
        const name = navigationRef.current?.getCurrentRoute()?.name;
        if (name) {
          routeNameRef.current = name;
          registry.setScreen(name);
        }
      }}
      onStateChange={() => {
        const name = navigationRef.current?.getCurrentRoute()?.name;
        if (name && name !== routeNameRef.current) {
          registry.setScreen(name);
          routeNameRef.current = name;
        }
      }}
      {...props}
    >
      {children}
    </NavigationContainer>
  );
}
