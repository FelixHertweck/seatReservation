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
import java.util.List;
import java.util.UUID;

import de.felixhertweck.seatreservation.common.dto.SeatStatusDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for event details in management API views.
 *
 * @param id event ID
 * @param name event name
 * @param description event description
 * @param startTime event start time
 * @param endTime event end time
 * @param bookingDeadline deadline for making bookings
 * @param bookingStartTime start time for making bookings
 * @param reminderSendDate scheduled date for reminder sending
 * @param isReminderSent whether reminder was sent
 * @param seatStatuses detailed seat statuses, present on detail requests
 * @param eventUserAllowancesIds IDs of user allowances
 * @param eventLocationId ID of associated location
 * @param createdByUserId ID of creator user
 * @param managerIds IDs of manager users
 * @param supervisorIds IDs of supervisor users
 * @param reservedCount total count of reserved seats
 */
@RegisterForReflection
public record EventResponseDTO(
        UUID id,
        String name,
        String description,
        Instant startTime,
        Instant endTime,
        Instant bookingDeadline,
        Instant bookingStartTime,
        Instant reminderSendDate,
        Boolean isReminderSent,
        List<SeatStatusDTO> seatStatuses,
        List<UUID> eventUserAllowancesIds,
        UUID eventLocationId,
        UUID createdByUserId,
        List<UUID> managerIds,
        List<UUID> supervisorIds,
        Integer reservedCount) {

    /**
     * Constructs EventResponseDTO for a given event, reserved count, and seat status list.
     *
     * @param event the event entity
     * @param reservedCount aggregate reserved seat count
     * @param seatStatuses detailed seat status list
     */
    public EventResponseDTO(Event event, Integer reservedCount, List<SeatStatusDTO> seatStatuses) {
        this(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                event.getBookingStartTime(),
                event.getReminderSendDate(),
                event.isReminderSent(),
                seatStatuses,
                event.getUserAllowances() != null
                        ? event.getUserAllowances().stream()
                                .map(userAllowance -> userAllowance.id)
                                .toList()
                        : List.of(),
                event.getEventLocation() != null ? event.getEventLocation().getId() : null,
                event.getCreatedBy() != null ? event.getCreatedBy().getId() : null,
                event.getManagers() != null
                        ? event.getManagers().stream().map(u -> u.id).toList()
                        : List.of(),
                event.getSupervisors() != null
                        ? event.getSupervisors().stream().map(u -> u.id).toList()
                        : List.of(),
                reservedCount);
    }

    /**
     * Constructs EventResponseDTO from an Event entity with detailed seat statuses.
     *
     * @param event the event entity
     */
    public EventResponseDTO(Event event) {
        this(
                event,
                event.getReservations() != null
                        ? (int)
                                event.getReservations().stream()
                                        .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                                        .count()
                        : 0,
                event.getReservations() != null
                        ? event.getReservations().stream().map(SeatStatusDTO::new).toList()
                        : null);
    }
}
