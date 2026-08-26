## 2025-02-28 - N+1 query and memory issues with Streams
**Learning:** Found multiple places in `OverviewService` where `allReservations.stream().filter(r -> r.getStatus() == ...).count()` is used after loading potentially all reservations. This is an N+1 and O(N) memory anti-pattern. E.g., `reservationRepository.listAll()` reads everything into memory for managers.
**Action:** Always push counting and grouping queries to the database instead of loading everything into memory and grouping.
