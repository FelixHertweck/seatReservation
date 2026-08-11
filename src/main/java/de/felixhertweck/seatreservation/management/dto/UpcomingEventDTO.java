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

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(description = "Summary of an upcoming event for the manager dashboard overview")
public record UpcomingEventDTO(
        @Schema(description = "Unique identifier of the event", required = true) UUID id,
        @Schema(description = "Name of the event", required = true) String name,
        @Schema(description = "Start time of the event") Instant startTime,
        @Schema(description = "Booking deadline for the event") Instant bookingDeadline,
        @Schema(description = "Name of the event location") String locationName,
        @Schema(description = "Number of reserved seats for this event") int reservedCount,
        @Schema(description = "Total capacity (seat count) of the event location") int capacity) {}
