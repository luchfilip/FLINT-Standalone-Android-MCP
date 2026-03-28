import React, { useEffect } from "react";
import { useFlintRegistry, useFlintItemContext } from "flint-core";

type FlintActionProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  flintName: string;
  flintDescription?: string;
  children?: React.ReactNode;
};

export function FlintAction({
  flintName,
  flintDescription,
  onClick,
  children,
  ...buttonProps
}: FlintActionProps) {
  const registry = useFlintRegistry();
  const itemCtx = useFlintItemContext();

  useEffect(() => {
    return registry.registerAction({
      name: flintName,
      description: flintDescription ?? flintName,
      handler: () => {
        if (onClick) {
          onClick(null as any);
        }
      },
      listId: itemCtx?.listId,
      itemIndex: itemCtx?.index,
    });
  }, [flintName, flintDescription, onClick, itemCtx, registry]);

  return (
    <button onClick={onClick} {...buttonProps}>
      {children}
    </button>
  );
}
