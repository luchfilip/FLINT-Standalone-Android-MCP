import React from "react";

export const NavigationContainer = ({ children, onReady, onStateChange, ...props }: any) => {
  React.useEffect(() => { if (onReady) onReady(); }, []);
  return <div {...props}>{children}</div>;
};
export const useNavigationContainerRef = () => ({ current: { getCurrentRoute: () => ({ name: "Home" }) } });
