## 2025-09-11 - Optimize Backend Stream Aggregation
**Learning:** Repeated stream iteration on large Java Collections in Quarkus Panache services creates redundant O(N) traversals and adds unneeded memory overhead. Grouping with Java Streams is also slower compared to one loop with standard HashMap combinations, where multiple items could be grouped and aggregated simultaneously during a single iteration block.
**Action:** Avoid multiple Java Streams on large lists for aggregation and counts, and replace them with standard for-loops and HashMap aggregations.
