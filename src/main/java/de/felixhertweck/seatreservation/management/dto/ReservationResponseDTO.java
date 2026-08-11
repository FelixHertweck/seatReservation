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
package de.felixhertweck.seatreservation.management.dto;

import java.time.Instant;
import java.util.UUID;

import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Data transfer object representing a reservation response for manager views.
 *
 * @param id unique identifier of the reservation
 * @param user user details for the reservation holder
 * @param eventId identifier of the event
 * @param seatId identifier of the reserved seat
 * @param reservationDateTime timestamp when the reservation was created
 * @param status static status of the reservation
 * @param liveStatus live status of the reservation
 */
@RegisterForReflection
public record ReservationResponseDTO(
        UUID id,
        UserDTO user,
        UUID eventId,
        UUID seatId,
        Instant reservationDateTime,
        ReservationStatus status,
        ReservationLiveStatus liveStatus) {

    /**
     * Constructs a {@link ReservationResponseDTO} from a {@link Reservation} entity.
     *
     * @param reservation the reservation entity
     */
    public ReservationResponseDTO(Reservation reservation) {
        this(
                reservation.id,
                new UserDTO(reservation.getUser()),
                reservation.getEvent().getId(),
                reservation.getSeat() != null ? reservation.getSeat().getId() : null,
                reservation.getReservationDate(),
                reservation.getStatus(),
                reservation.getLiveStatus());
    }
}
