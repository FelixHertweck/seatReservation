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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import de.felixhertweck.seatreservation.sanitization.NoHtmlSanitize;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Request to create box office reservations for a walk-in guest with no account.
 *
 * <p>{@code guestEmail} is a one-shot value: it is used only to send a single confirmation email at
 * creation time and is never persisted to any entity or column. {@code guestName} is required and
 * is persisted (see {@code BoxOfficeGuestInfo}) so supervisor-facing views can attribute a
 * reservation booked under the shared {@code boxoffice} account to a person.
 */
@RegisterForReflection
public class BoxOfficeGuestReservationRequestDTO {
    @NotNull(message = "Event ID must not be null")
    private UUID eventId;

    @NotNull(message = "Seat IDs must not be null")
    private Set<UUID> seatIds;

    @NotBlank(message = "Guest name must not be blank")
    private String guestName;

    @Email(message = "Guest email must be a valid email address")
    @NoHtmlSanitize
    private String guestEmail;

    private boolean checkedIn = false;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Set<UUID> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(Set<UUID> seatIds) {
        this.seatIds = seatIds;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }
}
