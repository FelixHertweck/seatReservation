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
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class EventLocationEntranceRepository
        implements PanacheRepositoryBase<EventLocationEntrance, UUID> {
    /**
     * Finds all entrances belonging to a specific event location.
     *
     * @param eventLocation the event location to search for
     * @return a list of entrances for the specified event location
     */
    public List<EventLocationEntrance> findByEventLocation(EventLocation eventLocation) {
        return find("eventLocation", eventLocation).list();
    }

    /**
     * Finds a entrance by ID, eagerly fetching its event location and that location's manager. The
     * ownership check needs both, so fetching them up front saves two extra queries.
     *
     * @param id the entrance ID to find
     * @return the entrance including its event location and manager, if it exists
     */
    public Optional<EventLocationEntrance> findByIdWithEventLocation(UUID id) {
        return find(
                        "select e from EventLocationEntrance e"
                                + " join fetch e.eventLocation el"
                                + " join fetch el.manager"
                                + " where e.id = ?1",
                        id)
                .firstResultOptional();
    }
}
