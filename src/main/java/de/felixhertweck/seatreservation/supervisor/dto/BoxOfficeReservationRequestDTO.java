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

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

/** Request to create box office reservations for a known, registered user. */
@RegisterForReflection
public class BoxOfficeReservationRequestDTO {
    @NotNull(message = "Event ID must not be null")
    private UUID eventId;

    @NotNull(message = "User ID must not be null")
    private UUID userId;

    @NotNull(message = "Seat IDs must not be null")
    private Set<UUID> seatIds;

    private boolean deductAllowance = true;

    private boolean checkedIn = false;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Set<UUID> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(Set<UUID> seatIds) {
        this.seatIds = seatIds;
    }

    public boolean isDeductAllowance() {
        return deductAllowance;
    }

    public void setDeductAllowance(boolean deductAllowance) {
        this.deductAllowance = deductAllowance;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }
}
