import React, { useEffect } from "react";
import { Text, TextProps } from "react-native";
import { useFlintRegistry } from "./FlintProvider";
import { useFlintItemContext } from "./FlintItemContext";

type FlintTextProps = TextProps & {
  flintKey: string;
  children: string;
};

export function FlintText({ flintKey, children, ...textProps }: FlintTextProps) {
  const registry = useFlintRegistry();
  const itemCtx = useFlintItemContext();

  useEffect(() => {
    return registry.registerContent({
      key: flintKey,
      value: children,
      listId: itemCtx?.listId,
      itemIndex: itemCtx?.index,
    });
  }, [flintKey, children, itemCtx, registry]);

  return <Text {...textProps}>{children}</Text>;
}
