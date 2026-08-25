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

import java.util.List;
import java.util.UUID;

/**
 * Event fired when an Event is deleted by a manager. Observers can react (e.g. cancelling any
 * reminder scheduled for the event, or expiring wallet passes for its reservations).
 *
 * <p>{@code reservationIds} lists all reservations the event had (of any status), captured before
 * deletion — deletion is only allowed once no reservation is active, but reservations that were
 * never individually cancelled (e.g. already checked-in/completed ones) still need this list so
 * observers can revoke anything they issued for them.
 */
public record EventDeletedEvent(UUID eventId, List<UUID> reservationIds) {}
