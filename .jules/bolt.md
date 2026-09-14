## 2026-09-14 - Optimize multiple stream passes in OverviewService

**Learning:** When multiple operations (like counting different statuses) need to be performed on a large collection, using multiple `stream().filter().count()` passes incurs unnecessary O(N) traversals and stream overhead. Replacing them with a single O(N) iteration (e.g., a `for` loop) reduces CPU and memory overhead.

**Action:** Replace multiple `.stream().filter()...` passes with a single loop when aggregating multiple counts or categories from the same collection to reduce redundant traversals.
