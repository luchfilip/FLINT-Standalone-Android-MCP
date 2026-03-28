import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  server: { port: 8080 },
  resolve: {
    alias: {
      "flint-web": path.resolve(__dirname, "../../flint-web/src"),
      "flint-core": path.resolve(__dirname, "../../flint-core/src"),
    },
  },
});
