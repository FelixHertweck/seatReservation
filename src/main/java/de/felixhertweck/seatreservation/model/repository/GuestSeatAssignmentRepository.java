/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.GuestSeatAssignment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class GuestSeatAssignmentRepository
        implements PanacheRepositoryBase<GuestSeatAssignment, UUID> {

    public List<GuestSeatAssignment> findByEventIdWithDetails(UUID eventId) {
        return find(
                        "select g from GuestSeatAssignment g left join fetch g.seat left join fetch"
                                + " g.assignedBy where g.event.id = ?1",
                        eventId)
                .list();
    }

    public List<GuestSeatAssignment> findByEventIdAndSeatIds(UUID eventId, List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        return find("event.id = ?1 and seat.id in ?2", eventId, seatIds).list();
    }

    public boolean existsByEventIdAndSeatId(UUID eventId, UUID seatId) {
        return count("event.id = ?1 and seat.id = ?2", eventId, seatId) > 0;
    }
}
