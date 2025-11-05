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
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SeatRepository implements PanacheRepository<Seat> {

    private static final Logger LOG = Logger.getLogger(SeatRepository.class);

    /**
     * Finds all seats for a specific event location.
     *
     * @param eventLocation the event location to search for
     * @return a list of seats for the specified event location
     */
    public List<Seat> findByEventLocation(EventLocation eventLocation) {
        LOG.debugf("Finding seats by event location ID: %d", eventLocation.id);
        List<Seat> seats = find("location", eventLocation).list();
        LOG.debugf("Found %d seats for event location ID: %d", seats.size(), eventLocation.id);
        return seats;
    }
}
