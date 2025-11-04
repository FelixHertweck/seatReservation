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
package de.felixhertweck.seatreservation.email;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.management.service.EventService;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.scheduler.Scheduled;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    @Inject EventService eventService;

    @Inject ReservationService reservationService;

    @Inject EmailService emailService;

    @Scheduled(every = "5m")
    public void sendEventReminders() {
        LOG.info("Starting scheduled event reminder task.");
        Instant now = Instant.now();
        // Check for events with reminder time in the last 5 minutes (to avoid missing any)
        Instant fiveMinutesAgo = now.minusSeconds(300);

        // Find all events with a reminder date set to the current time window
        List<Event> eventsWithReminders =
                eventService.findEventsWithReminderDateBetween(fiveMinutesAgo, now);
        LOG.debugf("Found %d events with reminders to send now.", eventsWithReminders.size());

        for (Event event : eventsWithReminders) {
            // Skip if reminder already sent
            if (event.isReminderSent()) {
                LOG.debugf(
                        "Skipping event %s (ID: %d) - reminder already sent",
                        event.getName(), event.id);
                continue;
            }

            LOG.debugf("Processing event: %s (ID: %d)", event.getName(), event.id);
            List<Reservation> reservations = reservationService.findByEvent(event);
            LOG.debugf("Found %d reservations for event %s.", reservations.size(), event.getName());

            Map<User, List<Reservation>> reservationsByUser =
                    reservations.stream().collect(Collectors.groupingBy(Reservation::getUser));

            reservationsByUser.forEach(
                    (user, userReservations) -> {
                        try {
                            LOG.debugf(
                                    "Sending reminder to user: %s for event: %s",
                                    user.getEmail(), event.getName());
                            emailService.sendEventReminder(user, event, userReservations);
                        } catch (Exception e) {
                            LOG.errorf(
                                    e,
                                    "Error sending reminder email to %s for event %s: %s",
                                    user.getEmail(),
                                    event.getName(),
                                    e.getMessage());
                        }
                    });

            // Mark reminder as sent
            eventService.markReminderAsSent(event);
            LOG.debugf("Marked reminder as sent for event: %s (ID: %d)", event.getName(), event.id);
        }
        LOG.info("Finished scheduled event reminder task.");
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyReservationCsvToManagers() {
        LOG.info("Starting scheduled CSV export task for event managers.");
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Instant startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                today.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault()).toInstant();

        List<Event> eventsToday = eventService.findEventsBetweenDates(startOfToday, endOfToday);
        LOG.debugf("Found %d events for today.", eventsToday.size());

        for (Event event : eventsToday) {
            LOG.debugf("Processing CSV export for event: %s (ID: %d)", event.getName(), event.id);

            try {
                // Get the event manager/owner
                User manager = event.getManager();
                if (manager != null) {
                    LOG.debugf(
                            "Sending CSV export to manager: %s for event: %s",
                            manager.getEmail(), event.getName());
                    emailService.sendEventReservationsCsvToManager(manager, event);
                } else {
                    LOG.warnf("No manager found for event: %s (ID: %d)", event.getName(), event.id);
                }
            } catch (EventNotFoundException | SecurityException | IOException e) {
                LOG.errorf(
                        e,
                        "Unexpected error during CSV generation or email preparation for event %s:"
                                + " %s",
                        event.getName(),
                        e.getMessage());
            }
        }
        LOG.info("Finished scheduled CSV export task for event managers.");
    }
}
