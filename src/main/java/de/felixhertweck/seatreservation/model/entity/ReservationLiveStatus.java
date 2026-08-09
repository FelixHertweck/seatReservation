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
package de.felixhertweck.seatreservation.model.entity;

/**
 * Day-of check-in tracking for a {@link Reservation}, independent of its {@link ReservationStatus}.
 * {@code null} on the reservation means no check-in decision has been made yet ("still reserved,
 * not checked in") - it is NOT the same as {@link #NO_SHOW}.
 */
public enum ReservationLiveStatus {
    /** Set by {@code CheckInService} when the reservation holder is let in. */
    CHECKED_IN,

    /** Set by {@code CheckInService} when staff cancel the reservation at the door. */
    CANCELLED,

    /** Reserved for marking a reservation as a confirmed no-show after the fact. */
    NO_SHOW
}
