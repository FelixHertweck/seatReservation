## 2026-09-13 - Avoid Multiple Passes and Grouping in Java Streams
**Learning:** In Quarkus Panache, performing multiple stream passes (`.filter().count()`) or using `Collectors.groupingBy()` on large collections fetched into memory (like `allReservations`) is inefficient and causes unnecessary O(N) traversals and object allocations.
**Action:** Replace multiple `.stream().filter()...` passes and `Collectors.groupingBy()` with a single O(N) iteration (e.g., a `for` loop) using standard Java collections (like `Map.merge()`) to avoid redundant traversals and reduce CPU/memory overhead.
