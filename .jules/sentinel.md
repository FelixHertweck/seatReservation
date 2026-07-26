## 2026-07-26 - Predictable SecureRandom sequence in GraalVM native builds
**Vulnerability:** A static `SecureRandom` instance was used for random code generation.
**Learning:** In Quarkus GraalVM native builds, static `SecureRandom` fields cache their seed at build time, causing predictable sequences.
**Prevention:** Always use the centralized `SecurityUtils` (configured with `--initialize-at-run-time` in `pom.xml`) for random number generation (e.g., `SecurityUtils.nextInt()`) to ensure a fresh runtime seed.