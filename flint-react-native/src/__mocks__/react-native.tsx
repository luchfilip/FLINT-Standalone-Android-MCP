import React from "react";
export const Text = ({ children, ...props }: any) => <span {...props}>{children}</span>;
export const View = ({ children, ...props }: any) => <div {...props}>{children}</div>;
export const Pressable = ({ children, onPress, ...props }: any) => <div onClick={onPress} {...props}>{children}</div>;
