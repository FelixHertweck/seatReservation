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
package de.felixhertweck.seatreservation.common.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;

public record EventLocationResponseDTO(
        Long id,
        String name,
        String address,
        Integer capacity,
        LimitedUserInfoDTO manager,
        List<SeatDTO> seats) {
    public EventLocationResponseDTO(EventLocation eventLocation) {
        this(eventLocation, Collections.emptyList());
    }

    public EventLocationResponseDTO(EventLocation eventLocation, List<Reservation> reservations) {
        this(
                eventLocation.getId(),
                eventLocation.getName(),
                eventLocation.getAddress(),
                eventLocation.getCapacity(),
                eventLocation.getManager() != null
                        ? new LimitedUserInfoDTO(eventLocation.getManager())
                        : null,
                createSeatDTOs(eventLocation.getSeats(), reservations));
    }

    private static List<SeatDTO> createSeatDTOs(List<Seat> seats, List<Reservation> reservations) {
        if (seats == null) {
            return List.of();
        }

        // If no reservations are provided, return seats with null reservation status
        if (reservations == null) {
            return seats.stream().map(seat -> new SeatDTO(seat, null)).toList();
        }

        Map<Long, ReservationStatus> reservationStatusMap =
                reservations.stream()
                        .filter(r -> r.getSeat() != null)
                        .collect(
                                Collectors.toMap(
                                        r -> r.getSeat().getId(),
                                        Reservation::getStatus,
                                        (existing, replacement) -> existing));

        return seats.stream()
                .map(seat -> new SeatDTO(seat, reservationStatusMap.get(seat.getId())))
                .toList();
    }
}
