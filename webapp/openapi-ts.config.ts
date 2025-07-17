import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "docs/openapi.json",
  output: {
    format: "prettier",
    lint: "eslint",
    path: "./src/api",
  },
  plugins: [
    "@hey-api/schemas",
    {
      dates: true,
      name: "@hey-api/transformers",
    },
    {
      enums: "javascript",
      name: "@hey-api/typescript",
    },
    {
      name: "@hey-api/sdk",
      transformer: true,
    },
    "@tanstack/react-query",
  ],
});
