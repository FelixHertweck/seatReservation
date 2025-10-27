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
package de.felixhertweck.seatreservation.email.event;

import java.util.List;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;

/** Event fired when reservations are updated/deleted and confirmation email should be sent. */
public class ReservationUpdateEvent {
    private final User user;
    private final List<Reservation> deletedReservations;
    private final List<Reservation> activeReservations;
    private final String additionalMailAddress;

    public ReservationUpdateEvent(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations,
            String additionalMailAddress) {
        this.user = user;
        this.deletedReservations = deletedReservations;
        this.activeReservations = activeReservations;
        this.additionalMailAddress = additionalMailAddress;
    }

    public ReservationUpdateEvent(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations) {
        this(user, deletedReservations, activeReservations, null);
    }

    public User getUser() {
        return user;
    }

    public List<Reservation> getDeletedReservations() {
        return deletedReservations;
    }

    public List<Reservation> getActiveReservations() {
        return activeReservations;
    }

    public String getAdditionalMailAddress() {
        return additionalMailAddress;
    }
}
