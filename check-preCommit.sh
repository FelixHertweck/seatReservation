#!/bin/bash

# This script checks if the pre-commit hook is installed and runs it if it is.

# Flags to skip checks
SKIP_SPOTLESS=false
SKIP_LICENSE=false
SKIP_TESTS=false
SKIP_BACKEND_BUILD=false
SKIP_LINT=false
SKIP_FORMAT=false
SKIP_API_CLIENT=false
SKIP_FRONTEND_BUILD=false

# Parse arguments
for arg in "$@"
do
    case $arg in
        --skip-spotless)
        SKIP_SPOTLESS=true
        ;;
        --skip-license)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        ;;
        --skip-tests)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        SKIP_TESTS=true
        ;;
        --skip-backend-build)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        SKIP_TESTS=true
        SKIP_BACKEND_BUILD=true
        ;;
        --skip-lint)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        SKIP_TESTS=true
        SKIP_BACKEND_BUILD=true
        SKIP_LINT=true
        ;;
        --skip-format)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        SKIP_TESTS=true
        SKIP_BACKEND_BUILD=true
        SKIP_LINT=true
        SKIP_FORMAT=true
        ;;
        --skip-api-client)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        SKIP_TESTS=true
        SKIP_BACKEND_BUILD=true
        SKIP_LINT=true
        SKIP_FORMAT=true
        SKIP_API_CLIENT=true
        ;;
        --skip-frontend-build)
        SKIP_SPOTLESS=true
        SKIP_LICENSE=true
        SKIP_TESTS=true
        SKIP_BACKEND_BUILD=true
        SKIP_LINT=true
        SKIP_FORMAT=true
        SKIP_API_CLIENT=true
        SKIP_FRONTEND_BUILD=true
        ;;
        *)
        # Unknown option, ignore for now
        ;;
    esac
    shift # Remove argument from processing
done

# Backend check
cd "$(dirname "$0")"

if [ "$SKIP_SPOTLESS" = false ]; then
  echo "Running Spotless check..."
  ./mvnw spotless:check || {
    echo "Spotless check failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-spotless"
    exit 1
  }
fi

if [ "$SKIP_LICENSE" = false ]; then
  echo "Running License check..."
  ./mvnw license:check-file-header || {
    echo "License check failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-license"
    exit 1
  }
fi

if [ "$SKIP_TESTS" = false ]; then
  echo "Running Backend Tests..."
  ./mvnw  test || {
    echo "Tests failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-tests"
    exit 1
  }
fi

if [ "$SKIP_BACKEND_BUILD" = false ]; then
  echo "Running Backend Build..."
  ./mvnw quarkus:build || {
    echo "Build failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-backend-build"
    exit 1
  }
fi

# Frontend check
cd "$(dirname "$0")"
cd ./webapp

if [ "$SKIP_LINT" = false ]; then
  echo "Running Frontend Linting check..."
  npm run lint:check || {
    echo "Linting failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-lint"
    exit 1
  }
fi

if [ "$SKIP_FORMAT" = false ]; then
  echo "Running Frontend Formatting check..."
  npm run format:check || {
    echo "Formatting check failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-format"
    exit 1
  }
fi

# api client check
if [ "$SKIP_API_CLIENT" = false ]; then
  echo "Running API client generation check..."
  npm run generate:api
  git diff --exit-code -- api/ || {
    echo "API client generation failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-api-client"
    exit 1
  }
fi

if [ "$SKIP_FRONTEND_BUILD" = false ]; then
  echo "Running Frontend Build..."
  npm run build || {
    echo "Build failed. Please fix the issues before committing."
    echo "To skip previous checks and retry from this point, run: $(basename "$0") --skip-frontend-build"
    exit 1
  }
fi

echo "All checks passed. You can proceed with the commit."
exit 0