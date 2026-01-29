import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "./docs/openapi.json",
  output: {
    postProcess: ["eslint", "prettier"],
    path: "./api",
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
