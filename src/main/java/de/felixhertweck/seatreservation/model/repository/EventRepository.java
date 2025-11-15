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
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {

    /**
     * Finds all events managed by a specific user.
     *
     * @param manager the manager user to search for
     * @return a list of events managed by the specified user
     */
    public List<Event> findByManager(User manager) {
        return find("manager", manager).list();
    }

    /**
     * Finds an event by its name.
     *
     * @param name the event name to search for
     * @return Optional event entity
     */
    public Optional<Event> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /**
     * Checks if a user is a supervisor for a specific event.
     *
     * @param eventId the event ID
     * @param userId the user ID
     * @return true if the user is a supervisor for the event, false otherwise
     */
    public boolean isUserSupervisor(Long eventId, Long userId) {
        if (eventId == null || userId == null) {
            return false;
        }

        return find(
                        "SELECT e FROM Event e JOIN e.supervisors s WHERE e.id = ?1 AND s.id = ?2",
                        eventId,
                        userId)
                .firstResultOptional()
                .isPresent();
    }
}
