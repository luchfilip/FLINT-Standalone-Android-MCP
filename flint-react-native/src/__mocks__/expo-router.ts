let pathname = "/";

export const usePathname = () => pathname;

export const __setPathname = (value: string) => {
  pathname = value;
};
