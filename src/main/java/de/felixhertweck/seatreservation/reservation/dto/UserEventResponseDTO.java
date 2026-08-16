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
import java.util.List;
import java.util.UUID;

import de.felixhertweck.seatreservation.common.dto.SeatStatusDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventStatus;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for event details in user-facing reservation API views.
 *
 * @param id event ID
 * @param name event name
 * @param description event description
 * @param startTime event start time
 * @param endTime event end time
 * @param bookingDeadline deadline for making bookings
 * @param bookingStartTime start time for making bookings
 * @param seatStatuses detailed seat statuses, present on detail requests
 * @param locationId ID of associated location
 * @param reservationsAllowed reservations allowed for current user
 * @param status current status of event (ACTIVE, CANCELLED)
 * @param cancellationReason reason for cancellation if cancelled
 */
@RegisterForReflection
public record UserEventResponseDTO(
        UUID id,
        String name,
        String description,
        Instant startTime,
        Instant endTime,
        Instant bookingDeadline,
        Instant bookingStartTime,
        List<SeatStatusDTO> seatStatuses,
        UUID locationId,
        Integer reservationsAllowed,
        EventStatus status,
        String cancellationReason) {

    /**
     * Constructs UserEventResponseDTO for a given event, reservations allowed count, and
     * reservations list.
     *
     * @param event the event to map
     * @param reservationsAllowed the reservations-allowed count to embed
     * @param reservations the event's reservations, or null if seat statuses are omitted
     */
    public UserEventResponseDTO(
            Event event, Integer reservationsAllowed, List<Reservation> reservations) {
        this(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                event.getBookingStartTime(),
                reservations == null
                        ? null
                        : reservations.stream().map(SeatStatusDTO::new).toList(),
                event.getEventLocation() == null ? null : event.getEventLocation().getId(),
                reservationsAllowed,
                event.getStatus(),
                event.getCancellationReason());
    }
}
