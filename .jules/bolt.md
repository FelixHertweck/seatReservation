## 2025-02-12 - N+1 Query Optimization in CheckInService
**Learning:** Calling `findById...` inside a loop iterating over user-provided IDs scales poorly (O(N) queries) and delays request processing unnecessarily. When using HQL positional parameters with an IN clause in Panache/Hibernate, `find("id in (?1)", collection)` requires parentheses.
**Action:** Replace looped exact-match queries with a single batch fetch using an HQL `IN (?1)` clause. Collect the results into a `Map` for O(1) lookup during processing to maintain existing error handling and ordering semantics.
