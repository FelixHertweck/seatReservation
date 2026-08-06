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
package de.felixhertweck.seatreservation.supervisor.dto;

import java.time.Instant;
import java.util.UUID;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.model.entity.GuestSeatAssignment;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record GuestSeatAssignmentResponseDTO(
        UUID id,
        UUID eventId,
        SeatDTO seat,
        String guestName,
        String assignedByUsername,
        Instant assignedAt) {
    public GuestSeatAssignmentResponseDTO(GuestSeatAssignment assignment) {
        this(
                assignment.id,
                assignment.getEvent().getId(),
                new SeatDTO(assignment.getSeat()),
                assignment.getGuestName(),
                assignment.getAssignedBy() != null
                        ? assignment.getAssignedBy().getUsername()
                        : null,
                assignment.getAssignedAt());
    }
}
