/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
package de.felixhertweck.seatreservation.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when an Event's schedule, location, or booking deadline is modified. Observers can
 * react asynchronously (e.g. notifying users who have existing reservations).
 */
public record EventRescheduledEvent(
        UUID eventId,
        String eventName,
        Instant oldStartTime,
        Instant newStartTime,
        Instant oldEndTime,
        Instant newEndTime,
        String oldLocationName,
        String newLocationName,
        Instant oldBookingDeadline,
        Instant newBookingDeadline) {}
