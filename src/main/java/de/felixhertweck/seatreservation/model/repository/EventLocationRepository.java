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

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class EventLocationRepository implements PanacheRepositoryBase<EventLocation, UUID> {
    /**
     * Finds all event locations a user manages - either directly (as the location's creator or a
     * user it was explicitly shared with) or indirectly, by managing an event held at that
     * location.
     *
     * @param manager the manager user to search for
     * @return a list of event locations managed by the specified user
     */
    public List<EventLocation> findByManager(User manager) {
        return find(
                        "SELECT DISTINCT el FROM EventLocation el WHERE el.createdBy = ?1 OR ?1"
                                + " MEMBER OF el.managers OR EXISTS (SELECT 1 FROM Event ev WHERE"
                                + " ev.event_location = el AND ?1 MEMBER OF ev.managers)",
                        manager)
                .list();
    }

    /**
     * Checks whether a user manages a given event location - either directly (as the location's
     * creator or a user it was explicitly shared with) or indirectly, by managing an event held at
     * that location.
     *
     * @param locationId the event location ID to check
     * @param userId the user ID to check
     * @return true if the user manages the location, false otherwise
     */
    public boolean isUserManager(UUID locationId, UUID userId) {
        if (locationId == null || userId == null) {
            return false;
        }
        return find(
                        "SELECT el FROM EventLocation el WHERE el.id = ?1 AND (el.createdBy.id ="
                                + " ?2 OR EXISTS (SELECT 1 FROM EventLocation el2 JOIN"
                                + " el2.managers m WHERE el2 = el AND m.id = ?2) OR EXISTS (SELECT"
                                + " 1 FROM Event ev JOIN ev.managers m WHERE ev.event_location = el"
                                + " AND m.id = ?2))",
                        locationId,
                        userId)
                .firstResultOptional()
                .isPresent();
    }

    /**
     * Finds event locations by their IDs, eagerly fetching each location's creator.
     *
     * @param ids collection of event location IDs
     * @return list of matching event locations with creator pre-fetched
     */
    public List<EventLocation> findByIdsWithManager(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return find("from EventLocation el left join fetch el.createdBy where el.id in ?1", ids)
                .list();
    }
}
