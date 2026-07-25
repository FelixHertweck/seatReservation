## 2026-07-25 - N+1 Query in Reservation Deletion
**Learning:** Performing database deletions and associated allowance recovery within a loop triggers individual SQL queries per item, creating an N+1 query issue.
**Action:** Use a single batch delete query (e.g., `delete("id in ?1", ids)`) and pre-group entity-related properties (e.g., reservations by user and event) to perform batched updates for relational tables like user allowances.
