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

import de.felixhertweck.seatreservation.common.dto.SeatStatusDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record EventResponseDTO(
        Long id,
        String name,
        String description,
        Instant startTime,
        Instant endTime,
        Instant bookingDeadline,
        List<SeatStatusDTO> seatStatuses,
        List<Long> eventUserAllowancesIds,
        Long eventLocationId,
        Long managerId) {
    public EventResponseDTO(Event event) {
        this(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                event.getReservations().stream().map(SeatStatusDTO::new).toList(),
                event.getUserAllowances() != null
                        ? event.getUserAllowances().stream()
                                .map(userAllowance -> userAllowance.id)
                                .toList()
                        : List.of(),
                event.getEventLocation() != null ? event.getEventLocation().getId() : null,
                event.getManager() != null ? event.getManager().getId() : null);
    }
}
