## 2024-03-15 - Push Filtering to Database Instead of Streams
**Learning:** Fetching an entire collection from a database via an unbounded ORM query (like `findByUserAndEventId`) just to filter it in-memory via Java Streams (e.g. `.filter(r -> r.getStatus() != ReservationStatus.BLOCKED)`) causes unnecessary data transfer, object allocation, and O(n) memory overhead.
**Action:** Push simple filtering criteria into the HQL/JPQL query (e.g. `r.status != ?3`) to limit the result set at the database level.
