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

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.eventManagement.dto.DetailedEventResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.EventNotFoundException;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class);

    @Inject EventRepository eventRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Inject UserRepository userRepository;

    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

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
    public DetailedEventResponseDTO createEvent(EventRequestDTO dto, User manager) {
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

        Event event =
                new Event(
                        dto.getName(),
                        dto.getDescription(),
                        dto.getStartTime(),
                        dto.getEndTime(),
                        dto.getBookingDeadline(),
                        location,
                        manager);
        eventRepository.persist(event);
        LOG.infof(
                "Event '%s' (ID: %d) created successfully by manager: %s (ID: %d)",
                event.getName(), event.getId(), manager.getUsername(), manager.getId());
        return new DetailedEventResponseDTO(event);
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
    public DetailedEventResponseDTO updateEvent(Long id, EventRequestDTO dto, User manager)
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
        event.setBookingDeadline(dto.getBookingDeadline());
        event.setEventLocation(location);
        eventRepository.persist(event);
        LOG.infof(
                "Event '%s' (ID: %d) updated successfully by manager: %s (ID: %d)",
                event.getName(), event.getId(), manager.getUsername(), manager.getId());
        return new DetailedEventResponseDTO(event);
    }

    /**
     * Retrieves a list of Events belonging to the currently authenticated manager. Access control:
     * If the user is an administrator, all Events are returned. Otherwise, only Events whose
     * manager is the current user are returned.
     *
     * @return A list of DTOs representing the Events.
     */
    public List<DetailedEventResponseDTO> getEventsByCurrentManager(User manager) {
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
        return events.stream().map(DetailedEventResponseDTO::new).collect(Collectors.toList());
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

    @Transactional
    public EventUserAllowancesDto setReservationsAllowedForUser(
            EventUserAllowancesDto dto, User manager)
            throws EventNotFoundException, UserNotFoundException {
        LOG.debugf(
                "Attempting to set reservation allowance for user ID: %d, event ID: %d by manager:"
                        + " %s (ID: %d)",
                dto.userId(), dto.eventId(), manager.getUsername(), manager.getId());
        Event event = getEventById(dto.eventId());
        User user = getUserById(dto.userId());

        if (!event.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to set reservation allowance for event ID"
                            + " %d.",
                    manager.getUsername(), manager.getId(), dto.eventId());
            throw new SecurityException("User is not the manager of this event");
        }

        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .find("user = ?1 and event = ?2", user, event)
                        .firstResultOptional()
                        .orElse(
                                new EventUserAllowance(
                                        user, event, dto.reservationsAllowedCount()));

        allowance.setReservationsAllowedCount(dto.reservationsAllowedCount());
        eventUserAllowanceRepository.persist(allowance);
        LOG.infof(
                "Reservation allowance set to %d for user ID %d and event ID %d by manager: %s (ID:"
                        + " %d)",
                dto.reservationsAllowedCount(),
                dto.userId(),
                dto.eventId(),
                manager.getUsername(),
                manager.getId());
        return new EventUserAllowancesDto(allowance);
    }

    public EventUserAllowancesDto getReservationAllowanceById(Long id, User manager) {
        LOG.debugf(
                "Attempting to retrieve reservation allowance with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation allowance with ID %d not found for"
                                                    + " manager: %s (ID: %d)",
                                            id, manager.getUsername(), manager.getId());
                                    return new EventNotFoundException("Allowance not found");
                                });

        if (!allowance.getEvent().getManager().equals(manager)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to view reservation allowance with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not authorized to view this allowance");
        }
        LOG.infof(
                "Successfully retrieved reservation allowance with ID %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        return new EventUserAllowancesDto(allowance);
    }

    /**
     * Retrieves all reservation allowances for the currently authenticated user. Access control: If
     * the user is an administrator, all allowances are returned. Otherwise, only allowances for
     * events managed by the current user are returned.
     *
     * @param currentUser The currently authenticated user.
     * @throws SecurityException If the user is not authorized to view the allowances.
     * @return A list of DTOs representing the reservation allowances for the current user.
     */
    public List<EventUserAllowancesDto> getReservationAllowances(User currentUser)
            throws SecurityException {
        LOG.debugf(
                "Attempting to retrieve all reservation allowances for user: %s (ID: %d)",
                currentUser.getUsername(), currentUser.getId());
        List<EventUserAllowance> allowances;
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all reservation allowances.");
            allowances = eventUserAllowanceRepository.listAll();
        } else {
            LOG.debugf(
                    "User is MANAGER, listing reservation allowances for events managed by user ID:"
                            + " %d",
                    currentUser.getId());
            allowances = eventUserAllowanceRepository.find("event.manager", currentUser).list();
        }
        LOG.infof(
                "Retrieved %d reservation allowances for user: %s (ID: %d)",
                allowances.size(), currentUser.getUsername(), currentUser.getId());
        return allowances.stream().map(EventUserAllowancesDto::new).collect(Collectors.toList());
    }

    /**
     * Retrieves all reservation allowances for a specific event. Access control: The user must be
     * the manager of the event or have the ADMIN role to view the allowances.
     *
     * @param eventId The ID of the event for which to retrieve allowances.
     * @param currentUser The currently authenticated user.
     * @throws SecurityException If the user is not authorized to view the allowances for this
     *     event.
     * @throws EventNotFoundException If the event with the specified ID is not found.
     * @return A list of DTOs representing the reservation allowances for the specified event.
     */
    public List<EventUserAllowancesDto> getReservationAllowancesByEventId(
            Long eventId, User currentUser) throws SecurityException, EventNotFoundException {
        LOG.debugf(
                "Attempting to retrieve reservation allowances for event ID: %d by user: %s (ID:"
                        + " %d)",
                eventId, currentUser.getUsername(), currentUser.getId());
        Event event = getEventById(eventId);
        if (!event.getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to view allowances for event ID %d.",
                    currentUser.getUsername(), currentUser.getId(), eventId);
            throw new SecurityException("User is not authorized to view allowances for this event");
        }
        List<EventUserAllowancesDto> result =
                eventUserAllowanceRepository.findByEventId(eventId).stream()
                        .map(EventUserAllowancesDto::new)
                        .collect(Collectors.toList());
        LOG.infof(
                "Retrieved %d reservation allowances for event ID %d by user: %s (ID: %d)",
                result.size(), eventId, currentUser.getUsername(), currentUser.getId());
        return result;
    }

    /**
     * Deletes a reservation allowance. Access control: The user must be the manager of the event
     * associated with the allowance or have the ADMIN role to delete it.
     *
     * @param id The ID of the reservation allowance to be deleted.
     * @param currentUser The currently authenticated user.
     * @throws EventNotFoundException If the reservation allowance with the specified ID is not
     *     found.
     * @throws SecurityException If the user is not authorized to delete this allowance.
     */
    @Transactional
    public void deleteReservationAllowance(Long id, User currentUser)
            throws EventNotFoundException, SecurityException {
        LOG.debugf(
                "Attempting to delete reservation allowance with ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation allowance with ID %d not found for"
                                                    + " deletion by user: %s (ID: %d)",
                                            id, currentUser.getUsername(), currentUser.getId());
                                    return new EventNotFoundException("Allowance not found");
                                });

        if (!allowance.getEvent().getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to delete reservation allowance with ID"
                            + " %d.",
                    currentUser.getUsername(), currentUser.getId(), id);
            throw new SecurityException("User is not authorized to delete this allowance");
        }

        eventUserAllowanceRepository.delete(allowance);
        LOG.infof(
                "Reservation allowance with ID %d deleted successfully by user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
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

    private User getUserById(Long id) throws UserNotFoundException {
        LOG.debugf("Attempting to find user by ID: %d", id);
        return userRepository
                .findByIdOptional(id)
                .orElseThrow(
                        () -> {
                            LOG.warnf("User with ID %d not found.", id);
                            return new UserNotFoundException("User with id " + id + " not found");
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
    public DetailedEventResponseDTO getEventByIdForManager(Long id, User manager)
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
        LOG.infof(
                "Successfully retrieved event with ID %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        return new DetailedEventResponseDTO(event);
    }
}
