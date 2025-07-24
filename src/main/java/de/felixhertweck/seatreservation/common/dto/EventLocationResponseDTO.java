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

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;

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
                eventLocation.getSeats() != null
                        ? eventLocation.getSeats().stream()
                                .map(
                                        seat -> {
                                            ReservationStatus status =
                                                    reservations.stream()
                                                            .filter(r -> r.getSeat().equals(seat))
                                                            .findFirst()
                                                            .map(Reservation::getStatus)
                                                            .orElse(null);
                                            return new SeatDTO(seat, status);
                                        })
                                .toList()
                        : List.of());
    }
}
