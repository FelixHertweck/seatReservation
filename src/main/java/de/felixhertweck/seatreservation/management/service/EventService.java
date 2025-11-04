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
package de.felixhertweck.seatreservation.management.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.email.NotificationService;
import de.felixhertweck.seatreservation.management.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class);

    @Inject EventRepository eventRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Inject NotificationService notificationService;

    /**
     * Creates a new Event and assigns the currently authenticated manager as its creator. Access
     * control: The currently authenticated user is automatically set as the manager of the Event.
     * This ensures that only the creator (manager) can later modify the event, unless the user is
     * an administrator.
     *
     * @param dto The DTO containing the details of the Event to be created.
     * @return A DTO representing the newly created Event.
     */
    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO dto, User manager) {
        LOG.debugf(
                "Attempting to create event with name: %s for manager: %s (ID: %d)",
                dto.getName(), manager.getUsername(), manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %d not found for event"
                                                    + " creation.",
                                            dto.getEventLocationId());
                                    return new IllegalArgumentException(
                                            "EventLocation with id "
                                                    + dto.getEventLocationId()
                                                    + " not found");
                                });

        validateEventTiming(dto);

        Event event =
                new Event(
                        dto.getName(),
                        dto.getDescription(),
                        dto.getStartTime(),
                        dto.getEndTime(),
                        dto.getBookingDeadline(),
                        dto.getBookingStartTime(),
                        location,
                        manager);
        event.setReminderSendDate(dto.getReminderSendDate());
        eventRepository.persist(event);
        LOG.infof("Event '%s' (ID: %d) created successfully.", event.getName(), event.getId());
        LOG.debugf(
                "Event '%s' (ID: %d) created successfully by manager: %s (ID: %d)",
                event.getName(), event.getId(), manager.getUsername(), manager.getId());

        // Schedule reminder if reminder date is set
        if (event.getReminderSendDate() != null) {
            notificationService.scheduleEventReminder(event);
        }

        return new EventResponseDTO(event);
    }

    /**
     * Updates an existing Event. Access control: The update is only allowed if the currently
     * authenticated user is the manager of the Event or has the ADMIN role.
     *
     * @param id The ID of the Event to be updated.
     * @param dto The DTO containing the updated details of the Event.
     * @return A DTO representing the updated Event.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to update the Event.
     */
    @Transactional
    public EventResponseDTO updateEvent(Long id, EventRequestDTO dto, User manager)
            throws EventNotFoundException, IllegalArgumentException {
        LOG.debugf(
                "Attempting to update event with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        Event event =
                eventRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for update by manager: %s"
                                                    + " (ID: %d)",
                                            id, manager.getUsername(), manager.getId());
                                    return new EventNotFoundException(
                                            "Event with id " + id + " not found");
                                });

        // Access control: Checks if the current user is the manager of the event
        // or if the user has the ADMIN role.
        if (!event.getManager().equals(manager) && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to update event with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not the manager of this event");
        }

        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %d not found for event update.",
                                            dto.getEventLocationId());
                                    return new IllegalArgumentException(
                                            "EventLocation with id "
                                                    + dto.getEventLocationId()
                                                    + " not found");
                                });

        validateEventTiming(dto);

        LOG.debugf(
                "Updating event ID %d: name='%s' -> '%s', description='%s' -> '%s', startTime='%s'"
                        + " -> '%s', endTime='%s' -> '%s', bookingDeadline='%s' -> '%s',"
                        + " eventLocationId='%d' -> '%d'",
                id,
                event.getName(),
                dto.getName(),
                event.getDescription(),
                dto.getDescription(),
                event.getStartTime(),
                dto.getStartTime(),
                event.getEndTime(),
                dto.getEndTime(),
                event.getBookingDeadline(),
                dto.getBookingDeadline(),
                event.getEventLocation().getId(),
                dto.getEventLocationId());
        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setBookingStartTime(dto.getBookingStartTime());
        event.setBookingDeadline(dto.getBookingDeadline());
        event.setReminderSendDate(dto.getReminderSendDate());
        event.setEventLocation(location);
        eventRepository.persist(event);
        LOG.infof("Event '%s' (ID: %d) updated successfully", event.getName(), event.getId());
        LOG.debugf(
                "Event '%s' (ID: %d) updated successfully by manager: %s (ID: %d)",
                event.getName(), event.getId(), manager.getUsername(), manager.getId());

        // Reschedule reminder when event is updated
        if (event.getReminderSendDate() != null) {
            notificationService.scheduleEventReminder(event);
        } else {
            // Cancel reminder if reminder date was removed
            notificationService.cancelEventReminder(event.getId());
        }

        return new EventResponseDTO(event);
    }

    /**
     * Retrieves a list of Events that occur between the specified start and end times.
     *
     * @param start The start time of the period to search for events.
     * @param end The end time of the period to search for events.
     * @return A list of Events that occur within the specified time range.
     */
    public List<Event> findEventsBetweenDates(Instant start, Instant end) {
        return eventRepository.find("startTime BETWEEN ?1 AND ?2", start, end).list();
    }

    /**
     * Retrieves a list of Events that have a reminder send date between the specified start and end
     * times.
     *
     * @param start The start time of the period to search for reminder dates.
     * @param end The end time of the period to search for reminder dates.
     * @return A list of Events that have a reminder send date within the specified time range.
     */
    public List<Event> findEventsWithReminderDateBetween(Instant start, Instant end) {
        return eventRepository.find("reminderSendDate BETWEEN ?1 AND ?2", start, end).list();
    }

    /**
     * Retrieves a list of Events belonging to the currently authenticated manager. Access control:
     * If the user is an administrator, all Events are returned. Otherwise, only Events whose
     * manager is the current user are returned.
     *
     * @return A list of DTOs representing the Events.
     */
    public List<EventResponseDTO> getEventsByCurrentManager(User manager) {
        LOG.debugf(
                "Attempting to retrieve events for manager: %s (ID: %d)",
                manager.getUsername(), manager.getId());
        List<Event> events;
        // Access control: If the user is an ADMIN, all Events are returned.
        // Otherwise, only Events belonging to this manager are returned.
        if (manager.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all events.");
            events = eventRepository.listAll();
        } else {
            LOG.debugf("User is MANAGER, listing events for manager ID: %d", manager.getId());
            events = eventRepository.findByManager(manager);
        }
        return events.stream().map(EventResponseDTO::new).collect(Collectors.toList());
    }

    /**
     * Deletes an event. Access control: The deletion is only allowed if the currently authenticated
     * user is the manager of the Event or has the ADMIN role. Deleting an event will also delete
     * all associated user allowances and reservations due to cascading settings in the Event
     * entity.
     *
     * @param ids The IDs of the Events to be deleted.
     * @param currentUser The currently authenticated user.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to delete the Event.
     * @throws IllegalArgumentException If no IDs are provided.
     */
    @Transactional
    public void deleteEvent(List<Long> ids, User currentUser)
            throws EventNotFoundException, SecurityException, IllegalArgumentException {
        if (ids == null || ids.isEmpty()) {
            LOG.warnf(
                    "No events to delete for user: %s (ID: %d)",
                    currentUser.getUsername(), currentUser.getId());
            throw new IllegalArgumentException("No event IDs provided for deletion.");
        }

        LOG.debugf(
                "Attempting to delete events with IDs: %s for user: %s (ID: %d)",
                ids, currentUser.getUsername(), currentUser.getId());

        for (Long id : ids) {

            Event event =
                    eventRepository
                            .findByIdOptional(id)
                            .orElseThrow(
                                    () -> {
                                        LOG.warnf(
                                                "Event with ID %d not found for deletion by user:"
                                                        + " %s (ID: %d)",
                                                id, currentUser.getUsername(), currentUser.getId());
                                        return new EventNotFoundException(
                                                "Event with id " + id + " not found");
                                    });

            if (!event.getManager().equals(currentUser)
                    && !currentUser.getRoles().contains(Roles.ADMIN)) {
                LOG.warnf(
                        "User %s (ID: %d) is not authorized to delete event with ID %d.",
                        currentUser.getUsername(), currentUser.getId(), id);
                throw new SecurityException("User is not authorized to delete this event");
            }

            eventRepository.delete(event);
            LOG.debugf(
                    "Event '%s' (ID: %d) deleted successfully by user: %s (ID: %d)",
                    event.getName(), event.getId(), currentUser.getUsername(), currentUser.getId());
        }
        LOG.infof("Events '%s' deleted successfully", ids);
    }

    /**
     * Finds an event by its ID without access control checks.
     *
     * @param id The ID of the event
     * @return The event if found, null otherwise
     */
    public Event findById(Long id) {
        LOG.debugf("Attempting to find event by ID: %d", id);
        return eventRepository.findByIdOptional(id).orElse(null);
    }

    private Event getEventById(Long id) throws EventNotFoundException {
        LOG.debugf("Attempting to find event by ID: %d", id);
        return eventRepository
                .findByIdOptional(id)
                .orElseThrow(
                        () -> {
                            LOG.warnf("Event with ID %d not found.", id);
                            return new EventNotFoundException("Event with id " + id + " not found");
                        });
    }

    /**
     * Retrieves a specific Event by its ID for a manager. Access control: The event is only
     * returned if the currently authenticated user is the manager of the Event or has the ADMIN
     * role.
     *
     * @param id The ID of the Event to be retrieved.
     * @param manager The currently authenticated user.
     * @return A DTO representing the retrieved Event.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to view the Event.
     */
    public EventResponseDTO getEventByIdForManager(Long id, User manager)
            throws EventNotFoundException, SecurityException {
        LOG.debugf(
                "Attempting to retrieve event with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        Event event = getEventById(id);
        if (!event.getManager().equals(manager) && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to view event with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not authorized to view this event");
        }
        LOG.debugf(
                "Successfully retrieved event with ID %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        return new EventResponseDTO(event);
    }

    /**
     * Marks the reminder as sent for the given event.
     *
     * @param event The event to mark the reminder as sent
     */
    @Transactional
    public void markReminderAsSent(Event event) {
        LOG.debugf("Marking reminder as sent for event ID: %d", event.id);
        event.setReminderSent(true);
        eventRepository.persist(event);
    }

    /**
     * Validates event timing constraints.
     *
     * @param dto The event request DTO containing the timing information
     * @throws IllegalArgumentException if timing constraints are violated
     */
    private void validateEventTiming(EventRequestDTO dto) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (!dto.getBookingDeadline().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Booking deadline must be before end time");
        }

        if (!dto.getBookingStartTime().isBefore(dto.getBookingDeadline())) {
            throw new IllegalArgumentException(
                    "Booking start time must be before booking deadline");
        }
    }
}
