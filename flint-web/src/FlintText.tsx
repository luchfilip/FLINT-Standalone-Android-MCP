import React, { useEffect } from "react";
import { useFlintRegistry, useFlintItemContext } from "flint-core";

type FlintTextProps = React.HTMLAttributes<HTMLSpanElement> & {
  flintKey: string;
  children: string;
};

export function FlintText({ flintKey, children, ...spanProps }: FlintTextProps) {
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

  return <span {...spanProps}>{children}</span>;
}
