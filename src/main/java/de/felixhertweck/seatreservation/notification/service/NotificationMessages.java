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
package de.felixhertweck.seatreservation.notification.service;

/**
 * Central source of the copy used for push/in-app notification titles, bodies, and action labels,
 * so the three {@link PushNotificationService} event handlers no longer each inline their own
 * {@code String.format} literals.
 *
 * <p>Text is English, matching the existing email templates (see {@code
 * src/main/resources/templates/email/event-cancelled.html}) and the frontend's {@code fallbackLng:
 * "en"} default. {@code User} has no stored locale/language preference to route by yet; introducing
 * a per-user locale should only require adding a {@code Locale} parameter to each message method
 * here, not touching the call sites.
 */
final class NotificationMessages {

    static final String BOOKING_CONFIRMED_TITLE = "Booking confirmed";
    static final String VIEW_BOOKING_ACTION_LABEL = "View booking";
    static final String RESERVATION_CANCELLED_TITLE = "Reservation cancelled";
    static final String VIEW_RESERVATIONS_ACTION_LABEL = "View reservations";
    static final String EVENT_CANCELLED_TITLE = "Event cancelled";
    static final String BROWSE_EVENTS_ACTION_LABEL = "Browse events";

    private NotificationMessages() {}

    static String bookingConfirmedMessage(int seatCount, String eventName) {
        return String.format(
                "Your booking for %d seat(s) at %s was completed successfully.",
                seatCount, eventName);
    }

    static String reservationCancelledMessage(int seatCount) {
        return String.format("Your reservation (%d seat(s)) has been cancelled.", seatCount);
    }

    static String eventCancelledMessage(String eventName, String cancellationReason) {
        return String.format(
                "The event '%s' has been cancelled. Reason: %s",
                eventName, cancellationReason != null ? cancellationReason : "No reason given");
    }
}
