## 2026-08-31 - Optimized Reservation Collection in OverviewService
**Learning:** Found a performance bottleneck where multiple O(N) operations were executed on a large list of reservations (`allReservations`). The code iteratively used `.stream().filter(...).count()` and `.stream().filter(...).collect(Collectors.groupingBy(...))` multiple times, causing repeated full traversals of the list.
**Action:** Replaced the multiple stream operations with a single O(N) iteration over the list, combining the aggregations (counts and hash maps) within one pass to significantly reduce overhead.
