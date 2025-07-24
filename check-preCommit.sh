#!/bin/bash

# This script checks if the pre-commit hook is installed and runs it if it is.

# Backend check
cd "$(dirname "$0")"
./mvnw spotless:check || {
  echo "Spotless check failed. Please fix the issues before committing."
  exit 1
}
./mvnw license:check-file-header || {
  echo "License check failed. Please fix the issues before committing."
  exit 1
}
./mvnw  test || {
  echo "Tests failed. Please fix the issues before committing."
  exit 1
}
./mvnw quarkus:build || {
  echo "Build failed. Please fix the issues before committing."
  exit 1
}

# Frontend check
cd "$(dirname "$0")"
cd ./webapp
npm run lint:check || {
  echo "Linting failed. Please fix the issues before committing."
  exit 1
}
npm run format:check || {
  echo "Formatting check failed. Please fix the issues before committing."
  exit 1
}
# api client check
npm run generate:api
git diff --exit-code -- api/ || {
  echo "API client generation failed. Please fix the issues before committing."
  exit 1
}
npm run build || {
  echo "Build failed. Please fix the issues before committing."
  exit 1
}

echo "All checks passed. You can proceed with the commit."
exit 0