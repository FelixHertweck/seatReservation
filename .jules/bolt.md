## 2025-02-12 - N+1 Query Optimization in CheckInService
**Learning:** Calling `findById...` inside a loop iterating over user-provided IDs scales poorly (O(N) queries) and delays request processing unnecessarily.
**Action:** Replace looped exact-match queries with a single batch fetch using an HQL `IN` clause. Collect the results into a `Map` for O(1) lookup during processing to maintain existing error handling and ordering semantics.
