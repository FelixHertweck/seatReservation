## 2026-08-30 - Prevent N+1 queries in UserEventResponseDTO creation
**Learning:** When bulk-loading events for a user and mapping them to DTOs in a loop, any lazy association accessed during projection (like `event.getEventLocation().getId()`) will trigger N+1 queries if not eagerly fetched.
**Action:** Use HQL `left join fetch` (e.g., `left join fetch e.event_location`) when querying events that will be subsequently mapped to DTOs to eagerly load associations and avoid triggering N+1 queries.
