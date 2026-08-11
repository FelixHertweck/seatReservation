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
package de.felixhertweck.seatreservation.reservation.dto;

import java.time.Instant;
import java.util.UUID;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Data transfer object representing a reservation response for user views.
 *
 * @param id unique identifier of the reservation
 * @param userId identifier of the user who owns the reservation
 * @param eventId identifier of the reserved event
 * @param seatId identifier of the reserved seat
 * @param reservationDateTime timestamp when the reservation was created
 * @param checkInToken token used for check-in verification
 */
@RegisterForReflection
public record UserReservationResponseDTO(
        UUID id,
        UUID userId,
        UUID eventId,
        UUID seatId,
        Instant reservationDateTime,
        String checkInToken) {

    /**
     * Constructs a {@link UserReservationResponseDTO} from a {@link Reservation} entity.
     *
     * @param reservation the reservation entity
     */
    public UserReservationResponseDTO(Reservation reservation) {
        this(
                reservation.id,
                reservation.getUser().id,
                reservation.getEvent().id,
                reservation.getSeat() != null ? reservation.getSeat().getId() : null,
                reservation.getReservationDate(),
                reservation.getCheckInToken() != null
                        ? reservation.getCheckInToken().getToken()
                        : null);
    }
}
