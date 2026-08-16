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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.events.EventCreatedEvent;
import de.felixhertweck.seatreservation.common.events.EventDeletedEvent;
import de.felixhertweck.seatreservation.common.events.EventRescheduledEvent;
import de.felixhertweck.seatreservation.common.events.EventUpdatedEvent;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.management.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.ManagerResolutionUtils;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class);

    @Inject EventRepository eventRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Inject UserRepository userRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject EventAccessService eventAccessService;

    @Inject jakarta.enterprise.event.Event<EventCreatedEvent> eventCreatedBus;

    @Inject jakarta.enterprise.event.Event<EventUpdatedEvent> eventUpdatedBus;

    @Inject jakarta.enterprise.event.Event<EventDeletedEvent> eventDeletedBus;

    @Inject jakarta.enterprise.event.Event<EventRescheduledEvent> eventRescheduledBus;

    /**
     * Creates a new Event and assigns the currently authenticated manager as its creator. Access
     * control: The currently authenticated user is automatically set as the manager of the Event.
     * This ensures that only the creator (manager) can later modify the event, unless the user is
     * an administrator.
     *
     * @param dto The DTO containing the details of the Event to be created.
     * @param manager The currently authenticated user.
     * @throws ValidationException If the EventLocation or any supervisor user is not found.
     * @return A DTO representing the newly created Event.
     */
    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO dto, User manager)
            throws ValidationException {
        LOG.debugf(
                "Attempting to create event with name: %s for manager: %s (ID: %s)",
                dto.getName(), manager.id, manager.getId());
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %s not found for event"
                                                    + " creation.",
                                            dto.getEventLocationId());
                                    return new ValidationException(
                                            "EventLocation with id "
                                                    + dto.getEventLocationId()
                                                    + " not found");
                                });

        validateEventTiming(dto);

        Set<User> supervisors = getSupervisorsFromIds(dto.getSupervisorIds());
        Set<User> additionalManagers =
                ManagerResolutionUtils.resolveManagers(
                        userRepository, dto.getManagerIds(), "event");

        Event event =
                new Event(
                        dto.getName(),
                        dto.getDescription(),
                        dto.getStartTime(),
                        dto.getEndTime(),
                        dto.getBookingDeadline(),
                        dto.getBookingStartTime(),
                        location,
                        manager,
                        dto.getReminderSendDate(),
                        supervisors,
                        additionalManagers);
        eventRepository.persist(event);
        LOG.infof("Event ID: %s created successfully.", event.getId());
        LOG.debugf(
                "Event '%s' (ID: %s) created successfully by manager: %s (ID: %s) with %d"
                        + " assigned supervisors and %d managers",
                event.getName(),
                event.getId(),
                manager.id,
                manager.getId(),
                supervisors.size(),
                event.getManagers().size());

        eventCreatedBus.fire(new EventCreatedEvent(event.getId(), event.getReminderSendDate()));

        return new EventResponseDTO(event);
    }

    /**
     * Updates an existing Event. Access control: The update is only allowed if the currently
     * authenticated user is a manager of the Event or has the ADMIN role.
     *
     * @param id The ID of the Event to be updated.
     * @param dto The DTO containing the updated details of the Event.
     * @return A DTO representing the updated Event.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws AccessDeniedException If the user is not authorized to update the Event.
     */
    @Transactional
    public EventResponseDTO updateEvent(UUID id, EventRequestDTO dto, User manager)
            throws EventNotFoundException, ValidationException {
        LOG.debugf(
                "Attempting to update event with ID: %s for manager: %s (ID: %s)",
                id, manager.id, manager.getId());
        Event event =
                eventRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Event with ID %s not found for update by manager: %s"
                                                    + " (ID: %s)",
                                            id, manager.id, manager.getId());
                                    return new EventNotFoundException(
                                            "Event with id " + id + " not found");
                                });

        // Access control: Checks if the current user is a manager of the event
        // or if the user has the ADMIN role.
        eventAccessService.requireAccess(event, AuthenticatedUser.of(manager));

        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "EventLocation with id %s not found for event update.",
                                            dto.getEventLocationId());
                                    return new ValidationException(
                                            "EventLocation with id "
                                                    + dto.getEventLocationId()
                                                    + " not found");
                                });

        validateEventTiming(dto);

        Set<User> supervisors = getSupervisorsFromIds(dto.getSupervisorIds());

        if (dto.getManagerIds() != null) {
            Set<User> newManagers =
                    ManagerResolutionUtils.resolveManagers(
                            userRepository, dto.getManagerIds(), "event");
            if (event.getCreatedBy() != null) {
                newManagers.add(event.getCreatedBy());
            }
            if (!manager.getRoles().contains(Roles.ADMIN)) {
                newManagers.add(manager);
            }
            event.setManagers(newManagers);
        }

        int oldSupervisorCount = event.getSupervisors() == null ? 0 : event.getSupervisors().size();
        int newSupervisorCount = supervisors == null ? 0 : supervisors.size();

        Instant oldStartTime = event.getStartTime();
        Instant oldEndTime = event.getEndTime();
        Instant oldBookingDeadline = event.getBookingDeadline();
        String oldLocationName =
                event.getEventLocation() != null ? event.getEventLocation().getName() : null;

        Instant newStartTime = dto.getStartTime();
        Instant newEndTime = dto.getEndTime();
        Instant newBookingDeadline = dto.getBookingDeadline();
        String newLocationName = location != null ? location.getName() : null;

        boolean scheduleChanged =
                !Objects.equals(oldStartTime, newStartTime)
                        || !Objects.equals(oldEndTime, newEndTime)
                        || !Objects.equals(oldBookingDeadline, newBookingDeadline)
                        || !Objects.equals(oldLocationName, newLocationName);

        LOG.debugf(
                "Updating event ID %s: name='%s' -> '%s', description='%s' -> '%s', startTime='%s'"
                        + " -> '%s', endTime='%s' -> '%s', bookingDeadline='%s' -> '%s',"
                        + " eventLocationId='%s' -> '%s' , supervisors=%d -> %d",
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
                dto.getEventLocationId(),
                oldSupervisorCount,
                newSupervisorCount);

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setBookingStartTime(dto.getBookingStartTime());
        event.setBookingDeadline(dto.getBookingDeadline());
        event.setReminderSendDate(dto.getReminderSendDate());
        event.setEventLocation(location);
        event.setSupervisors(supervisors);

        eventRepository.persist(event);
        LOG.infof("Event '%s' (ID: %s) updated successfully", event.getName(), event.getId());
        LOG.debugf(
                "Event '%s' (ID: %s) updated successfully by manager: %s (ID: %s)",
                event.getName(), event.getId(), manager.id, manager.getId());

        eventUpdatedBus.fireAsync(
                new EventUpdatedEvent(
                        event.getId(),
                        event.getName(),
                        location != null ? location.getName() : null,
                        location != null ? location.getAddress() : null,
                        event.getStartTime(),
                        event.getEndTime(),
                        event.getReminderSendDate()));

        if (scheduleChanged) {
            eventRescheduledBus.fireAsync(
                    new EventRescheduledEvent(
                            event.getId(),
                            event.getName(),
                            oldStartTime,
                            newStartTime,
                            oldEndTime,
                            newEndTime,
                            oldLocationName,
                            newLocationName,
                            oldBookingDeadline,
                            newBookingDeadline));
        }

        return new EventResponseDTO(event);
    }

    /** Adds a manager to an event. */
    @Transactional
    public EventResponseDTO addManager(UUID eventId, UUID newManagerId, User currentUser)
            throws EventNotFoundException, AccessDeniedException, ValidationException {
        if (currentUser == null) {
            throw new AccessDeniedException(
                    "User is not authorized to add a manager to this event");
        }
        Event event = getEventById(eventId);
        eventAccessService.requireAccess(event, AuthenticatedUser.of(currentUser));
        User newManager =
                ManagerResolutionUtils.resolveManagers(
                                userRepository, Set.of(newManagerId), "event")
                        .iterator()
                        .next();
        event.getManagers().add(newManager);
        eventRepository.persist(event);
        LOG.infof("Added manager %s to event %s", newManagerId, eventId);
        return new EventResponseDTO(event);
    }

    /** Removes a manager from an event. */
    @Transactional
    public EventResponseDTO removeManager(UUID eventId, UUID managerToRemoveId, User currentUser)
            throws EventNotFoundException, AccessDeniedException, ValidationException {
        if (currentUser == null) {
            throw new AccessDeniedException(
                    "User is not authorized to remove a manager from this event");
        }
        Event event = getEventById(eventId);
        eventAccessService.requireAccess(event, AuthenticatedUser.of(currentUser));
        if (managerToRemoveId.equals(currentUser.getId())) {
            LOG.warnf(
                    "User ID %s attempted to remove themselves from event ID %s",
                    currentUser.id, eventId);
            throw new ValidationException("Manager cannot remove themselves from the event");
        }
        if (event.getCreatedBy() != null
                && managerToRemoveId.equals(event.getCreatedBy().getId())) {
            LOG.warnf(
                    "Attempted to remove primary creator %s from event ID %s",
                    managerToRemoveId, eventId);
            throw new ValidationException("The event creator cannot be removed as manager");
        }
        if (event.getManagers().size() <= 1) {
            throw new ValidationException("An event must have at least one manager");
        }
        User managerToRemove =
                userRepository
                        .findByIdOptional(managerToRemoveId)
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                "User with id "
                                                        + managerToRemoveId
                                                        + " not found"));
        event.getManagers().remove(managerToRemove);
        eventRepository.persist(event);
        LOG.infof("Removed manager %s from event %s", managerToRemoveId, eventId);
        return new EventResponseDTO(event);
    }

    /**
     * Retrieves a set of User entities based on the provided supervisor IDs.
     *
     * @param supervisorIds Set of supervisor user IDs
     * @return Set of User entities
     * @throws ValidationException if any supervisor ID is invalid
     */
    private Set<User> getSupervisorsFromIds(Set<UUID> supervisorIds) throws ValidationException {
        Set<User> supervisors = new HashSet<>();
        if (supervisorIds == null || supervisorIds.isEmpty()) {
            // empty set when no supervisors are provided
            return supervisors;
        }
        List<User> foundSupervisors = userRepository.findByIds(supervisorIds.stream().toList());
        supervisors.addAll(foundSupervisors);

        if (supervisors.size() != supervisorIds.size()) {
            Set<UUID> foundIds =
                    foundSupervisors.stream().map(u -> u.id).collect(Collectors.toSet());
            for (UUID supervisorId : supervisorIds) {
                if (!foundIds.contains(supervisorId)) {
                    LOG.warnf("User with id %s not found for event creation.", supervisorId);
                    throw new ValidationException("User with id " + supervisorId + " not found");
                }
            }
        }
        return supervisors;
    }

    /**
     * Retrieves a list of Events that occur between the specified start and end times.
     *
     * @param start The start time of the period to search for events.
     * @param end The end time of the period to search for events.
     * @return A list of Events that occur within the specified time range.
     */
    public List<Event> findEventsBetweenDates(Instant start, Instant end) {
        return eventRepository.findBetweenStartTimes(start, end);
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
        return eventRepository.findByReminderSendDateBetween(start, end);
    }

    /**
     * Retrieves a list of Events belonging to the currently authenticated manager. Access control:
     * If the user is an administrator, all Events are returned. Otherwise, only Events whose
     * manager is the current user are returned.
     *
     * @return A list of DTOs representing the Events.
     */
    public List<EventResponseDTO> getEventsByCurrentManager(AuthenticatedUser manager) {
        LOG.debugf("Attempting to retrieve events for manager ID: %s", manager.id());
        List<Event> events;
        // Access control: If the user is an ADMIN, all Events are returned.
        // Otherwise, only Events belonging to this manager are returned.
        if (manager.isAdmin()) {
            LOG.debug("User is ADMIN, listing all events.");
            events = eventRepository.listAll();
        } else {
            LOG.debugf("User is MANAGER, listing events for manager ID: %s", manager.id());
            events = eventRepository.findByManager(userRepository.getReference(manager.id()));
        }

        List<UUID> eventIds = events.stream().map(Event::getId).toList();
        Map<UUID, Integer> reservedCounts =
                reservationRepository.getReservedSeatCountsByEventIds(eventIds);

        return events.stream()
                .map(e -> new EventResponseDTO(e, reservedCounts.getOrDefault(e.getId(), 0), null))
                .toList();
    }

    /**
     * Deletes an event. Access control: The deletion is only allowed if the currently authenticated
     * user is a manager of the Event or has the ADMIN role. Deleting an event will also delete all
     * associated user allowances and reservations due to cascading settings in the Event entity.
     *
     * <p>Known limitation: this cascading delete of {@code EventUserAllowance} happens directly
     * through JPA/Hibernate and never calls {@link
     * de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository#delete}, so
     * its usual seat-cart access-grant invalidation (see {@link
     * de.felixhertweck.seatreservation.reservation.service.SeatCartAccessGrantStore}) is bypassed
     * here. A user can therefore keep adding seats to their cart for an already-deleted event until
     * the grant's TTL runs out. Deliberately not chased with an explicit invalidation call here -
     * the seat cart is a best-effort cache by design (see {@link
     * de.felixhertweck.seatreservation.reservation.service.SeatCartService} class docs), the
     * exposure is bounded by the (short) grant TTL, and no reservation can ever actually complete
     * against a deleted event anyway, so this is accepted rather than adding another invalidation
     * call site outside {@code EventUserAllowanceRepository}.
     *
     * @param ids The IDs of the Events to be deleted.
     * @param currentUser The currently authenticated user.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws AccessDeniedException If the user is not authorized to delete the Event.
     * @throws ValidationException If no IDs are provided.
     */
    @Transactional
    public void deleteEvent(List<UUID> ids, User currentUser)
            throws EventNotFoundException, AccessDeniedException, ValidationException {
        if (ids == null || ids.isEmpty()) {
            LOG.warnf(
                    "No events to delete for user ID: %s (ID: %s)",
                    currentUser.id, currentUser.getId());
            throw new ValidationException("No event IDs provided for deletion.");
        }

        LOG.debugf(
                "Attempting to delete events with IDs: %s for user ID: %s (ID: %s)",
                ids, currentUser.id, currentUser.getId());

        List<Event> fetchedEvents = eventRepository.findByIdsWithManager(ids);

        Map<UUID, Event> eventMap =
                fetchedEvents.stream()
                        .collect(Collectors.toMap(e -> e.getId(), e -> e, (e1, e2) -> e1));

        List<Event> eventsToDelete = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Event event = eventMap.get(id);
            if (event == null) {
                LOG.warnf(
                        "Event with ID %s not found for deletion by user: %s (ID: %s)",
                        id, currentUser.id, currentUser.getId());
                throw new EventNotFoundException("Event with id " + id + " not found");
            }

            eventAccessService.requireAccess(event, AuthenticatedUser.of(currentUser));

            eventsToDelete.add(event);
        }

        for (Event event : eventsToDelete) {
            eventRepository.delete(event);
            LOG.debugf(
                    "Event '%s' (ID: %s) deleted successfully by user ID: %s (ID: %s)",
                    event.getName(), event.getId(), currentUser.id, currentUser.getId());
            eventDeletedBus.fire(new EventDeletedEvent(event.getId()));
        }

        LOG.infof("Events '%s' deleted successfully", ids);
    }

    /**
     * Finds an event by its ID without access control checks.
     *
     * @param id The ID of the event
     * @return The event if found, null otherwise
     */
    public Event findById(UUID id) {
        LOG.debugf("Attempting to find event by ID: %s", id);
        return eventRepository.findByIdOptional(id).orElse(null);
    }

    private Event getEventById(UUID id) throws EventNotFoundException {
        LOG.debugf("Attempting to find event by ID: %s", id);
        Event event = eventRepository.findByIdOptional(id).orElse(null);
        if (event == null) {
            LOG.warnf("Event with ID %s not found.", id);
            throw new EventNotFoundException("Event with id " + id + " not found");
        }
        return event;
    }

    /**
     * Retrieves a specific Event by its ID for a manager. Access control: The event is only
     * returned if the currently authenticated user is a manager of the Event or has the ADMIN role.
     *
     * @param id The ID of the Event to be retrieved.
     * @param manager The currently authenticated user.
     * @return A DTO representing the retrieved Event.
     * @throws EventNotFoundException If the Event with the specified ID is not found.
     * @throws AccessDeniedException If the user is not authorized to view the Event.
     */
    public EventResponseDTO getEventByIdForManager(UUID id, User manager)
            throws EventNotFoundException, AccessDeniedException {
        LOG.debugf(
                "Attempting to retrieve event with ID: %s for manager: %s (ID: %s)",
                id, manager.id, manager.getId());
        Event event = getEventById(id);
        eventAccessService.requireAccess(event, AuthenticatedUser.of(manager));
        LOG.debugf(
                "Successfully retrieved event with ID %s for manager: %s (ID: %s)",
                id, manager.id, manager.getId());
        return new EventResponseDTO(event);
    }

    /**
     * Marks the reminder as sent for the given event.
     *
     * @param event The event to mark the reminder as sent
     */
    @Transactional
    public void markReminderAsSent(Event event) {
        LOG.debugf("Marking reminder as sent for event ID: %s", event.id);
        event.setReminderSent(true);
        eventRepository.persist(event);
    }

    /**
     * Validates event timing constraints.
     *
     * @param dto The event request DTO containing the timing information
     * @throws ValidationException if timing constraints are violated
     */
    private void validateEventTiming(EventRequestDTO dto) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new ValidationException("Start time must be before end time");
        }

        if (!dto.getBookingDeadline().isBefore(dto.getEndTime())) {
            throw new ValidationException("Booking deadline must be before end time");
        }

        if (!dto.getBookingStartTime().isBefore(dto.getBookingDeadline())) {
            throw new ValidationException("Booking start time must be before booking deadline");
        }

        if (dto.getReminderSendDate() != null
                && !dto.getReminderSendDate().isBefore(dto.getStartTime())) {
            throw new ValidationException("Reminder send date must be before event start time");
        }
    }
}
