## 2026-07-25 - Bulk Deletion N+1 Optimization
**Learning:** In Quarkus Panache, doing `eventLocationRepository.findByIdOptional(id)` in a loop during bulk deletion creates an N+1 select issue.
**Action:** Always prefer batch fetching using `repository.find("from EventLocation e left join fetch e.manager where e.id in ?1", ids)` before iterating over the result set to validate and delete entities, avoiding N queries while preserving JPA lifecycle cascades.
