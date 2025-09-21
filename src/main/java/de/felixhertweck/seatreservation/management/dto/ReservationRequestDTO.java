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

import java.util.Set;
import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ReservationRequestDTO {
    @NotNull(message = "Event ID must not be null")
    private Long eventId;

    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Seat ID must not be null")
    private Set<Long> seatIds;

    private boolean deductAllowance = true;

    public Long getEventId() {
        return eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public Set<Long> getSeatIds() {
        return seatIds;
    }

    public boolean isDeductAllowance() {
        return deductAllowance;
    }

    public void setDeductAllowance(boolean deductAllowance) {
        this.deductAllowance = deductAllowance;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setSeatIds(Set<Long> seatIds) {
        this.seatIds = seatIds;
    }
}
