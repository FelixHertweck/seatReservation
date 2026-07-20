## 2026-07-20 - N+1 Query in Reservation Deletion Loop
**Learning:** Calling `findByIdOptional` inside a `for` loop executing reservation deletion causes a severe N+1 query problem, exponentially increasing database load and execution time when handling multiple reservations.
**Action:** Replace iterative DB reads with a single O(1) batch read using `repository.find("id in ?1", ids)` before the loop, store results in a Map, and retain the original loop logic (iterating over `ids`) to maintain transaction ordering, logging contexts, and security validations.
