## 2024-05-15 - [Predictable Codes in Native Builds]
**Vulnerability:** `CodeGenerator` instantiated a static `SecureRandom` instance without being configured for runtime initialization.
**Learning:** In GraalVM native images (Quarkus), static fields are initialized at build time by default. A static `SecureRandom` caches its initial state/seed during the build, leading to predictable random number sequences across application restarts.
**Prevention:** Always use the centralized `SecurityUtils` (which is configured in `pom.xml` via `--initialize-at-run-time=...`) for random number generation in Quarkus applications to ensure a fresh, unpredictable seed at runtime.
