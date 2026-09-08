## 2025-02-28 - Avoid repeated Java Streams on the same large collection
**Learning:** In backend Java code (e.g. OverviewService), fetching a large collection and doing multiple `.stream().filter(..).count()` passes is an O(N * passes) memory and CPU performance bottleneck.
**Action:** When multiple operations (like grouping and counting) need to be performed on a large collection, replace multiple `.stream().filter()...` passes with a single O(N) iteration (e.g. a `for` loop) using standard Java collections (like `Map.merge()`) to avoid redundant traversals and reduce CPU/memory overhead.
