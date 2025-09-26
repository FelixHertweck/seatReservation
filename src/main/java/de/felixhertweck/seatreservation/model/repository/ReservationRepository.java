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

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class ReservationRepository implements PanacheRepository<Reservation> {
    /*
     * This method returns all reservations for a given user that are not blocked.
     */
    public List<Reservation> findByUser(User user) {
        return find("user = ?1 and status != ?2", user, ReservationStatus.BLOCKED).list();
    }

    public List<Reservation> findByEventId(Long eventId) {
        return find("event.id", eventId).list();
    }

    public List<Reservation> findByUserAndEvent(
            User user, de.felixhertweck.seatreservation.model.entity.Event event) {
        return find("user = ?1 and event = ?2", user, event).list();
    }

    public void persistAll(List<Reservation> newReservations) {
        newReservations.forEach(this::persist);
    }
}
