import { FlatCompat } from "@eslint/eslintrc";
import { globalIgnores } from "eslint/config";

const compat = new FlatCompat({
  baseDirectory: import.meta.dirname,
});

const eslintConfig = [
  ...compat.config({
    extends: ["next"],
    settings: {
      next: {
        rootDir: "./",
      },
    },
  }),
  globalIgnores([".next/*", "node_modules/*", "public/*", "out/*", "api/*"]),
];

export default eslintConfig;
