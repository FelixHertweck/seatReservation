
## 2026-07-20 - N+1 query pattern in supervisor filtering
**Learning:** Found an N+1 query bottleneck in `CheckInService.getAllEventsForSupervisor` where `isAuthorizedForEvent(User, Long)` was called per event in memory after a `findAll()`.
**Action:** Always fetch authorized events via a targeted SQL/HQL query (e.g. `SELECT DISTINCT e FROM Event e LEFT JOIN e.supervisors s WHERE e.manager = ?1 OR s = ?1`) instead of post-filtering in memory to maintain O(1) query performance.
