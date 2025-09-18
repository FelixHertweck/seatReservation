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

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SeatRepository implements PanacheRepository<Seat> {

    private static final Logger LOG = Logger.getLogger(SeatRepository.class);

    /**
     * Finds all seats for a given event location
     *
     * @param eventLocation the event location to find seats for
     * @return list of seats for the given event location
     */
    public List<Seat> findByEventLocation(EventLocation eventLocation) {
        LOG.debugf("Finding seats by event location ID: %d", eventLocation.id);
        List<Seat> seats = find("location", eventLocation).list();
        LOG.debugf("Found %d seats for event location ID: %d", seats.size(), eventLocation.id);
        return seats;
    }

    /**
     * Finds a seat by ID with location eagerly loaded to avoid LazyInitializationException
     *
     * @param id the ID of the seat to find
     * @return an Optional containing the found seat, or empty if not found
     */
    public Optional<Seat> findByIdWithLocation(Long id) {
        LOG.debugf("Finding seat by ID with location: %d", id);
        return find(
                        "SELECT s FROM Seat s JOIN FETCH s.location WHERE s.id = :id",
                        Parameters.with("id", id))
                .firstResultOptional();
    }

    /** Finds all seats for a manager's locations with location eagerly loaded */
    public List<Seat> findAllForManagerWithLocation(User manager) {
        LOG.debugf(
                "Finding all seats for manager: %s (ID: %d)",
                manager.getUsername(), manager.getId());
        return find(
                        "SELECT s FROM Seat s JOIN FETCH s.location l WHERE l.manager = :manager",
                        Parameters.with("manager", manager))
                .list();
    }

    /**
     * Finds all seats with location eagerly loaded (for admins)
     *
     * @return list of all seats with locations
     */
    public List<Seat> findAllWithLocation() {
        LOG.debugf("Finding all seats with location loaded");
        return find("SELECT s FROM Seat s JOIN FETCH s.location").list();
    }
}
