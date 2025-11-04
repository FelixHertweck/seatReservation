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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.management.service.EventService;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    @Inject EventService eventService;

    @Inject ReservationService reservationService;

    @Inject EmailService emailService;

    @Inject Scheduler scheduler;

    private ExecutorService executorService;

    @PostConstruct
    void init() {
        executorService = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory());
    }

    @PreDestroy
    void destroy() {
        executorService.shutdown();
    }

    /**
     * Schedules a reminder job for the given event.
     *
     * @param event The event for which to schedule a reminder
     */
    public void scheduleEventReminder(Event event) {
        if (event.getReminderSendDate() == null) {
            LOG.debugf(
                    "No reminder date set for event %s (ID: %d), skipping",
                    event.getName(), event.id);
            return;
        }

        // Cancel existing job if any
        String jobId = "reminder-event-" + event.id;
        scheduler.unscheduleJob(jobId);

        // Calculate delay until reminder should be sent
        Instant now = Instant.now();
        Instant reminderTime = event.getReminderSendDate();

        if (reminderTime.isBefore(now)) {
            LOG.warnf(
                    "Reminder date %s for event %s (ID: %d) is in the past, skipping",
                    reminderTime, event.getName(), event.id);
            return;
        }

        long delaySeconds = Duration.between(now, reminderTime).getSeconds();

        LOG.infof(
                "Scheduling reminder for event %s (ID: %d) at %s (in %d seconds)",
                event.getName(), event.id, reminderTime, delaySeconds);

        // Schedule the reminder job
        scheduler
                .newJob(jobId)
                .setInterval(delaySeconds + "s")
                .setTask(
                        executionContext -> {
                            executorService.execute(() -> sendReminderForEvent(event.id));
                        })
                .schedule();
    }

    /**
     * Sends reminder emails for a specific event.
     *
     * @param eventId The ID of the event
     */
    private void sendReminderForEvent(Long eventId) {
        try {
            LOG.infof("Executing reminder task for event ID: %d", eventId);

            // Fetch fresh event data
            Event event = eventService.findById(eventId);
            if (event == null) {
                LOG.warnf("Event with ID %d not found, skipping reminder", eventId);
                return;
            }

            // Skip if reminder already sent
            if (event.isReminderSent()) {
                LOG.debugf(
                        "Skipping event %s (ID: %d) - reminder already sent",
                        event.getName(), event.id);
                return;
            }

            LOG.debugf("Processing event: %s (ID: %d)", event.getName(), event.id);
            List<Reservation> reservations = reservationService.findByEvent(event);
            LOG.debugf("Found %d reservations for event %s.", reservations.size(), event.getName());

            if (reservations.isEmpty()) {
                LOG.debugf("No reservations for event %s, skipping reminder", event.getName());
                eventService.markReminderAsSent(event);
                return;
            }

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
            LOG.infof("Reminder sent and marked for event: %s (ID: %d)", event.getName(), event.id);
        } catch (Exception e) {
            LOG.errorf(e, "Error processing reminder for event ID: %d", eventId);
        }
    }

    /**
     * Cancels the scheduled reminder for an event.
     *
     * @param eventId The ID of the event
     */
    public void cancelEventReminder(Long eventId) {
        String jobId = "reminder-event-" + eventId;
        scheduler.unscheduleJob(jobId);
        LOG.debugf("Cancelled reminder job for event ID: %d", eventId);
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
