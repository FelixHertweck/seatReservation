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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import de.felixhertweck.seatreservation.common.dto.EventLocationWithStatusDTO;
import de.felixhertweck.seatreservation.common.dto.SeatWithStatusDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;

public record EventResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime bookingDeadline,
        EventLocationWithStatusDTO location,
        Integer reservationsAllowed) {
    public static EventResponseDTO toDTO(
            EventUserAllowance allowance, List<Reservation> reservations) {
        Event event = allowance.getEvent();
        Integer reservationsAllowed = allowance.getReservationsAllowedCount();

        List<SeatWithStatusDTO> seats = new ArrayList<>();
        List<Reservation> safeReservations =
                reservations != null ? reservations : java.util.Collections.emptyList();
        for (Reservation reservation : safeReservations) {
            seats.add(SeatWithStatusDTO.toDTO(reservation.getSeat(), reservation.getStatus()));
        }

        EventLocationWithStatusDTO location = null;
        if (event.getEventLocation() != null) {
            location = EventLocationWithStatusDTO.toDTO(event.getEventLocation(), seats);
        }

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                location,
                reservationsAllowed);
    }
}
