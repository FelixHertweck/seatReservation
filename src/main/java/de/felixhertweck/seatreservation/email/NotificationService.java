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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    @Inject NotificationService self;

    private ExecutorService executorService;

    /** Cache of scheduled job IDs to avoid expensive lookups */
    private final Set<String> scheduledJobIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    @SuppressWarnings("unused")
    void init() {
        executorService = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory());
    }

    @PreDestroy
    @SuppressWarnings("unused")
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

        cancelEventReminder(event.id);

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

        String jobId = "reminder-event-" + event.id;
        // Schedule the reminder job as a one-time task
        // We use a very long interval (1 year) to make it effectively a one-time job
        // The task itself will unschedule the job after execution
        scheduler
                .newJob(jobId)
                .setInterval("365d") // Set a long interval to satisfy the scheduler requirement
                .setDelayed(delaySeconds + "s") // Set the delay until first execution
                .setTask(
                        executionContext -> {
                            executorService.execute(() -> self.sendReminderForEvent(event.id));
                        })
                .schedule();

        // Add to cache for efficient lookups
        scheduledJobIds.add(jobId);
    }

    /**
     * Orchestrates the reminder sending process with short transactions. Uses separate transactions
     * for loading data and marking complete to avoid long-running transactions during email
     * sending.
     *
     * @param eventId The ID of the event
     */
    public void sendReminderForEvent(Long eventId) {
        try {
            LOG.infof("Executing reminder task for event ID: %d", eventId);

            // Step 1: Load data in a SHORT transaction
            ReminderData data = self.loadReminderData(eventId);
            if (data == null) {
                // Event not found or already sent, cancel the job
                cancelEventReminder(eventId);
                return;
            }

            // Check if there are no reservations
            if (data.reservations.isEmpty()) {
                LOG.debugf(
                        "No reservations for event %s, marking reminder as sent",
                        data.event.getName());
                self.markReminderComplete(eventId);
                cancelEventReminder(eventId);
                return;
            }

            // Step 2: Send emails OUTSIDE transaction (can take as long as needed)
            sendReminderEmails(data);

            // Step 3: Mark as complete in a SHORT transaction
            self.markReminderComplete(eventId);

            cancelEventReminder(eventId);
            LOG.infof(
                    "Reminder sent and marked for event: %s (ID: %d)",
                    data.event.getName(), data.event.id);
        } catch (Exception e) {
            cancelEventReminder(eventId);
            LOG.errorf(e, "Error processing reminder for event ID: %d", eventId);
        }
    }

    /**
     * Loads all required data for sending reminders in a short transaction. Only performs read
     * operations to minimize transaction duration. Eagerly loads all required lazy fields to avoid
     * LazyInitializationException.
     *
     * @param eventId The ID of the event
     * @return ReminderData containing event and reservations, or null if not applicable
     */
    @Transactional
    public ReminderData loadReminderData(Long eventId) {
        // Fetch fresh event data
        Event event = eventService.findById(eventId);
        if (event == null) {
            LOG.warnf("Event with ID %d not found, skipping reminder", eventId);
            cancelEventReminder(eventId);
            return null;
        }

        // Skip if reminder already sent
        if (event.isReminderSent()) {
            LOG.debugf(
                    "Skipping event %s (ID: %d) - reminder already sent",
                    event.getName(), event.id);
            return null;
        }

        LOG.debugf("Processing event: %s (ID: %d)", event.getName(), event.id);
        List<Reservation> reservations = reservationService.findByEvent(event);
        LOG.debugf("Found %d reservations for event %s.", reservations.size(), event.getName());

        // Return early if no reservations, but let the orchestration method handle marking as sent
        if (reservations.isEmpty()) {
            LOG.debugf(
                    "No reservations for event %s, will mark reminder as sent in orchestration"
                            + " method",
                    event.getName());
            // Return ReminderData with empty reservations list to signal this case
            return new ReminderData(event, reservations);
        }

        // Eagerly load all lazy fields that will be needed outside the transaction
        // This prevents LazyInitializationException
        eagerLoadEntities(event, reservations);

        return new ReminderData(event, reservations);
    }

    /**
     * Eagerly loads all lazy-loaded fields that will be accessed outside the transaction. This must
     * be called within an active transaction/session.
     *
     * @param event The event entity
     * @param reservations The list of reservations
     */
    private void eagerLoadEntities(Event event, List<Reservation> reservations) {
        // Force load event fields
        event.getName();
        event.getDescription();
        event.getStartTime();
        if (event.getEventLocation() != null) {
            event.getEventLocation().getName();
        }

        // Force load user and seat data for all reservations
        reservations.forEach(
                reservation -> {
                    User user = reservation.getUser();
                    if (user != null) {
                        // Force load user fields
                        user.getEmail();
                        user.getFirstname();
                        user.getLastname();
                    }
                    if (reservation.getSeat() != null) {
                        // Force load seat fields
                        reservation.getSeat().getSeatNumber();
                        reservation.getSeat().getSeatRow();
                    }
                });
    }

    /**
     * Sends reminder emails to all users. Uses @ActivateRequestContext to enable database queries
     * in EmailService without holding a long transaction.
     *
     * @param data The reminder data containing event and reservations
     */
    @ActivateRequestContext
    public void sendReminderEmails(ReminderData data) {
        Map<User, List<Reservation>> reservationsByUser =
                data.reservations.stream().collect(Collectors.groupingBy(Reservation::getUser));

        LOG.debugf(
                "Sending reminders to %d users for event %s",
                reservationsByUser.size(), data.event.getName());

        reservationsByUser.forEach(
                (user, userReservations) -> {
                    try {
                        LOG.debugf(
                                "Sending reminder to user: %s for event: %s",
                                user.getEmail(), data.event.getName());
                        emailService.sendEventReminder(user, data.event, userReservations);
                    } catch (Exception e) {
                        LOG.errorf(
                                e,
                                "Error sending reminder email to %s for event %s: %s",
                                user.getEmail(),
                                data.event.getName(),
                                e.getMessage());
                    }
                });
    }

    /**
     * Marks the reminder as sent in a short transaction. Performs a single update operation to
     * minimize transaction duration.
     *
     * @param eventId The ID of the event
     */
    @Transactional
    public void markReminderComplete(Long eventId) {
        Event event = eventService.findById(eventId);
        if (event != null) {
            eventService.markReminderAsSent(event);
        }
    }

    /**
     * Helper class to hold reminder data loaded from database. Used to pass data between
     * transactional boundaries.
     */
    public static class ReminderData {
        final Event event;
        final List<Reservation> reservations;

        ReminderData(Event event, List<Reservation> reservations) {
            this.event = event;
            this.reservations = reservations;
        }
    }

    /**
     * Cancels the scheduled reminder for an event if exists.
     *
     * @param eventId The ID of the event
     */
    public void cancelEventReminder(Long eventId) {
        String jobId = "reminder-event-" + eventId;

        if (!scheduledJobIds.contains(jobId)) {
            LOG.debugf("No existing reminder job for event ID: %d to cancel.", eventId);
            return;
        }

        scheduler.unscheduleJob(jobId);
        scheduledJobIds.remove(jobId);
        LOG.debugf("Cancelled reminder job for event ID: %d", eventId);
    }

    /** Sends daily reservation CSVs to event managers for events happening today. */
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
