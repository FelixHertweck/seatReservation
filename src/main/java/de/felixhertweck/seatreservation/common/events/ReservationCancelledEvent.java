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

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;

/**
 * Event fired synchronously, while the persistence context is still open, when a user cancels
 * reservations for an event. Carries both the cancelled reservations and the user's still-active
 * reservations for the same event, so observers can build an accurate "what's left" confirmation.
 */
public record ReservationCancelledEvent(
        User user, List<Reservation> deletedReservations, List<Reservation> activeReservations) {}
