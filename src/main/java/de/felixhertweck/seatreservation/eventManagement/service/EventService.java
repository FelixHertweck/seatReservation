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
package de.felixhertweck.seatreservation.eventManagement.service;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class);

    @Inject EventRepository eventRepository;

    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    /**
     * Creates a new Event and assigns the currently authenticated manager as its creator. Access
     * control: The currently authenticated user is automatically set as the manager of the Event.
     * This ensures that only the creator (manager) can later modify the event, unless the user is
     * an administrator.
     *
     * @param name The name of the Event.
     * @param description The description of the Event.
     * @param startTime The start time of the Event.
     * @param endTime The end time of the Event.
     * @param bookingDeadline The booking deadline for the Event.
     * @param eventLocationId The ID of the EventLocation where the Event will take place
     * @param manager The currently authenticated user who will be set as the manager of the Event.
     * @return The newly created Event.
     */
    @Transactional
    public Event createEvent(
            String name,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime bookingDeadline,
            Long eventLocationId,
            User manager) {
        LOG.debugf(
                "Attempting to create event with name: %s for manager: %s (ID: %d)",
                name, manager.getUsername(), manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdWithManager(eventLocationId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %d not found for event"
                                                    + " creation.",
                                            eventLocationId);
                                    return new IllegalArgumentException(
                                            "EventLocation with id "
                                                    + eventLocationId
                                                    + " not found");
                                });

        Event event =
                new Event(
                        name, description, startTime, endTime, bookingDeadline, location, manager);
        eventRepository.persist(event);
        LOG.infof(
                "Event '%s' (ID: %d) created successfully by manager: %s (ID: %d)",
                event.getName(), event.getId(), manager.getUsername(), manager.getId());
        return event;
    }

    /**
     * Updates an existing Event. Access control: The update is only allowed if the currently
     * authenticated user is the manager of the Event or has the ADMIN role.
     *
     * @param id The ID of the Event to be updated.
     * @param name The new name of the Event.
     * @param description The new description of the Event.
     * @param startTime The new start time of the Event.
     * @param endTime The new end time of the Event.
     * @param bookingDeadline The new booking deadline for the Event.
     * @param eventLocationId The ID of the new EventLocation where the Event will take place
     * @param manager The currently authenticated user attempting the update.
     * @return The updated Event.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to update the Event.
     */
    @Transactional
    public Event updateEvent(
            Long id,
            String name,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime bookingDeadline,
            Long eventLocationId,
            User manager)
            throws EventNotFoundException, IllegalArgumentException {
        LOG.debugf(
                "Attempting to update event with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        Event event =
                eventRepository
                        .findOptionalByIdWithManagerReservations(id)
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
                        .findByIdOptional(eventLocationId)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %d not found for event update.",
                                            eventLocationId);
                                    return new IllegalArgumentException(
                                            "EventLocation with id "
                                                    + eventLocationId
                                                    + " not found");
                                });

        LOG.debugf(
                "Updating event ID %d: name='%s' -> '%s', description='%s' -> '%s', startTime='%s'"
                        + " -> '%s', endTime='%s' -> '%s', bookingDeadline='%s' -> '%s',"
                        + " eventLocationId='%d' -> '%d'",
                id,
                event.getName(),
                name,
                event.getDescription(),
                description,
                event.getStartTime(),
                startTime,
                event.getEndTime(),
                endTime,
                event.getBookingDeadline(),
                bookingDeadline,
                event.getEventLocation().getId(),
                location.getId());
        event.setName(name);
        event.setDescription(description);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setBookingDeadline(bookingDeadline);
        event.setEventLocation(location);
        eventRepository.persist(event);
        LOG.infof(
                "Event '%s' (ID: %d) updated successfully by manager: %s (ID: %d)",
                event.getName(), event.getId(), manager.getUsername(), manager.getId());
        return event;
    }

    /**
     * Retrieves a list of Events that occur between the specified start and end times.
     *
     * @param start The start time of the period to search for events.
     * @param end The end time of the period to search for events.
     * @return A list of Events that occur within the specified time range.
     */
    public List<Event> findEventsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return eventRepository.find("startTime BETWEEN ?1 AND ?2", start, end).list();
    }

    /**
     * Retrieves a list of Events belonging to the currently authenticated manager. Access control:
     * If the user is an administrator, all Events are returned. Otherwise, only Events whose
     * manager is the current user are returned.
     *
     * @return A list of DTOs representing the Events.
     */
    public List<Event> getEventsByCurrentManager(User manager) {
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
        LOG.infof(
                "Retrieved %d events for manager: %s (ID: %d)",
                events.size(), manager.getUsername(), manager.getId());
        return events;
    }

    /**
     * Deletes an event. Access control: The deletion is only allowed if the currently authenticated
     * user is the manager of the Event or has the ADMIN role. Deleting an event will also delete
     * all associated user allowances and reservations due to cascading settings in the Event
     * entity.
     *
     * @param id The ID of the Event to be deleted.
     * @param currentUser The currently authenticated user.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws SecurityException If the user is not authorized to delete the Event.
     */
    @Transactional
    public void deleteEvent(Long id, User currentUser)
            throws EventNotFoundException, SecurityException {
        LOG.debugf(
                "Attempting to delete event with ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        Event event =
                eventRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %d not found for deletion by user: %s"
                                                    + " (ID: %d)",
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
        LOG.infof(
                "Event '%s' (ID: %d) deleted successfully by user: %s (ID: %d)",
                event.getName(), event.getId(), currentUser.getUsername(), currentUser.getId());
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
    public Event getEventByIdForManager(Long id, User manager)
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
        return event;
    }
}
