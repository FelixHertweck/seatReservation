## 2026-09-04 - Reduce redundant stream traversals
**Learning:** When multiple operations (like grouping and counting) need to be performed on a large collection, it's significantly more efficient to replace multiple `.stream().filter()...` passes with a single O(N) iteration using standard Java collections (like `Map.merge()`).
**Action:** In the future, look for repeated streams processing the same collection in services, especially for dashboard statistics, and consolidate them into a single loop.
