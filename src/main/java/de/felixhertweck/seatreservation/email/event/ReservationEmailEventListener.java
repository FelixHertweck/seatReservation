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
package de.felixhertweck.seatreservation.email.event;

import java.io.IOException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.email.EmailService;
import org.jboss.logging.Logger;

/**
 * Asynchronous event listener for reservation email notifications. Handles sending confirmation
 * emails for new reservations and updates in a non-blocking manner.
 */
@ApplicationScoped
public class ReservationEmailEventListener {

    private static final Logger LOG = Logger.getLogger(ReservationEmailEventListener.class);

    @Inject EmailService emailService;

    /**
     * Asynchronously handles reservation confirmation events. This method is invoked in a separate
     * thread, allowing the reservation request to complete without waiting for email delivery.
     *
     * @param event The reservation confirmation event containing user and reservation details
     */
    public void onReservationConfirmation(@ObservesAsync ReservationConfirmationEvent event) {
        LOG.debugf(
                "Processing async reservation confirmation email for user: %s",
                event.getUser().getEmail());
        try {
            emailService.sendReservationConfirmation(
                    event.getUser(), event.getReservations(), event.getAdditionalMailAddress());
            LOG.debugf(
                    "Async reservation confirmation email sent successfully to: %s",
                    event.getUser().getEmail());
        } catch (IOException e) {
            LOG.errorf(
                    e,
                    "Failed to send async reservation confirmation email to: %s",
                    event.getUser().getEmail());
        }
    }

    /**
     * Asynchronously handles reservation update events. This method is invoked in a separate
     * thread, allowing the reservation update request to complete without waiting for email
     * delivery.
     *
     * @param event The reservation update event containing user, deleted and active reservation
     *     details
     */
    public void onReservationUpdate(@ObservesAsync ReservationUpdateEvent event) {
        LOG.debugf(
                "Processing async reservation update email for user: %s",
                event.getUser().getEmail());
        try {
            emailService.sendUpdateReservationConfirmation(
                    event.getUser(),
                    event.getDeletedReservations(),
                    event.getActiveReservations(),
                    event.getAdditionalMailAddress());
            LOG.debugf(
                    "Async reservation update email sent successfully to: %s",
                    event.getUser().getEmail());
        } catch (IOException e) {
            LOG.errorf(
                    e,
                    "Failed to send async reservation update email to: %s",
                    event.getUser().getEmail());
        }
    }
}
