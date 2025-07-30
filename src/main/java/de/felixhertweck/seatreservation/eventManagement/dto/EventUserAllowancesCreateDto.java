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
package de.felixhertweck.seatreservation.eventManagement.dto;

import java.util.Set;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class EventUserAllowancesCreateDto {

    @NotNull(message = "Event ID must not be null")
    private Long eventId;

    @NotNull(message = "User IDs must not be null")
    @NotEmpty(message = "User IDs must not be empty")
    private Set<Long> userIds;

    @NotNull(message = "Reservations allowed count must not be null")
    private int reservationsAllowedCount;

    public EventUserAllowancesCreateDto(
            Set<Long> userIds, Long eventId, int reservationsAllowedCount) {
        this.userIds = userIds;
        this.eventId = eventId;
        this.reservationsAllowedCount = reservationsAllowedCount;
    }

    public Long getEventId() {
        return eventId;
    }

    public Set<Long> getUserIds() {
        return userIds;
    }

    public int getReservationsAllowedCount() {
        return reservationsAllowedCount;
    }
}
