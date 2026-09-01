## 2026-09-01 - Consolidating Java Streams
**Learning:** When multiple operations (like grouping and counting) need to be performed on a large collection in Quarkus/Java, replacing multiple `.stream().filter()...` passes with a single O(N) iteration using standard Java collections (like `HashMap` and `getOrDefault`) significantly reduces redundant traversals and CPU/memory overhead.
**Action:** Look for multiple `.stream()` aggregations on the same collection and refactor them into a single pass using standard loops and maps.
