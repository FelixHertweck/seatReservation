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

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(description = "Aggregate stats for the manager dashboard overview")
public record ManagementOverviewStatsDTO(
        @Schema(description = "Total number of managed events") long eventsCount,
        @Schema(description = "Total number of upcoming managed events") long upcomingEventsCount,
        @Schema(description = "Total number of events with open booking") long bookingOpenCount,
        @Schema(description = "Total number of reservations across all managed events")
                long reservationsCount,
        @Schema(description = "Number of RESERVED reservations") long reservationsReserved,
        @Schema(description = "Number of BLOCKED reservations") long reservationsBlocked,
        @Schema(description = "Number of PENDING reservations") long reservationsPending,
        @Schema(description = "Occupancy percentage across upcoming events") int occupancyPercent,
        @Schema(description = "Total reserved seats across upcoming events") long occupancyReserved,
        @Schema(description = "Total seat capacity across upcoming events") long occupancyCapacity,
        @Schema(description = "Contingent usage percentage across allowances")
                int contingentUsagePercent,
        @Schema(description = "Total contingent seats used") long contingentUsed,
        @Schema(description = "Total contingent seats granted") long contingentGranted) {}
