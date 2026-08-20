import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "/",
  build: {
    outDir: "dist",
    sourcemap: false,
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/react") || id.includes("node_modules/react-dom")) {
            return "react";
          }
          if (id.includes("node_modules/recharts") || id.includes("node_modules/d3-")) {
            return "charts";
          }
          if (
            id.includes("node_modules/jspdf") ||
            id.includes("node_modules/pdfjs-dist") ||
            id.includes("node_modules/html2canvas") ||
            id.includes("node_modules/dompurify")
          ) {
            return "pdf";
          }
        }
      }
    }
  },
  server: {
    host: "127.0.0.1",
    port: 4173
  }
});
