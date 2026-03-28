import React, { useEffect } from "react";
import { Pressable, PressableProps } from "react-native";
import { useFlintRegistry } from "./FlintProvider";
import { useFlintItemContext } from "./FlintItemContext";

type FlintActionProps = PressableProps & {
  flintName: string;
  flintDescription?: string;
  children?: React.ReactNode;
};

export function FlintAction({
  flintName,
  flintDescription,
  onPress,
  children,
  ...pressableProps
}: FlintActionProps) {
  const registry = useFlintRegistry();
  const itemCtx = useFlintItemContext();

  useEffect(() => {
    return registry.registerAction({
      name: flintName,
      description: flintDescription ?? flintName,
      handler: () => {
        if (onPress) {
          onPress(null as any);
        }
      },
      listId: itemCtx?.listId,
      itemIndex: itemCtx?.index,
    });
  }, [flintName, flintDescription, onPress, itemCtx, registry]);

  return (
    <Pressable onPress={onPress} {...pressableProps}>
      {children}
    </Pressable>
  );
}
