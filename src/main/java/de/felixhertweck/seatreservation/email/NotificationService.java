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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendEventReminders() {
        LOG.info("Starting scheduled event reminder task.");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow =
                eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow);
        LOG.debugf("Found %d events for tomorrow.", eventsTomorrow.size());

        for (Event event : eventsTomorrow) {
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
                                    "Fehler beim Senden der Erinnerungs-E-Mail an %s für Event %s:"
                                            + " %s",
                                    user.getEmail(),
                                    event.getName(),
                                    e.getMessage());
                        }
                    });
        }
        LOG.info("Finished scheduled event reminder task.");
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendDailyReservationCsvToManagers() {
        LOG.info("Starting scheduled CSV export task for event managers.");
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

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
            } catch (Exception e) {
                LOG.errorf(
                        e,
                        "Error sending CSV export email for event %s: %s",
                        event.getName(),
                        e.getMessage());
            }
        }
        LOG.info("Finished scheduled CSV export task for event managers.");
    }
}
