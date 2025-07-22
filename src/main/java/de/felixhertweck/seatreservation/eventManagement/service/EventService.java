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

@ApplicationScoped
public class EventService {

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
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "EventLocation with id "
                                                        + dto.getEventLocationId()
                                                        + " not found"));

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
        Event event =
                eventRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new EventNotFoundException(
                                                "Event with id " + id + " not found"));

        // Access control: Checks if the current user is the manager of the event
        // or if the user has the ADMIN role.
        if (!event.getManager().equals(manager) && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not the manager of this event");
        }

        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "EventLocation with id "
                                                        + dto.getEventLocationId()
                                                        + " not found"));

        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setBookingDeadline(dto.getBookingDeadline());
        event.setEventLocation(location);
        eventRepository.persist(event);
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
        List<Event> events;
        // Access control: If the user is an ADMIN, all Events are returned.
        // Otherwise, only Events belonging to this manager are returned.
        if (manager.getRoles().contains(Roles.ADMIN)) {
            events = eventRepository.listAll();
        } else {
            events = eventRepository.findByManager(manager);
        }
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
        Event event =
                eventRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new EventNotFoundException(
                                                "Event with id " + id + " not found"));

        if (!event.getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not authorized to delete this event");
        }

        eventRepository.delete(event);
    }

    @Transactional
    public EventUserAllowancesDto setReservationsAllowedForUser(
            EventUserAllowancesDto dto, User manager)
            throws EventNotFoundException, UserNotFoundException {
        Event event = getEventById(dto.eventId());
        User user = getUserById(dto.userId());

        if (!event.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
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
        return new EventUserAllowancesDto(allowance);
    }

    public EventUserAllowancesDto getReservationAllowanceById(Long id, User manager) {
        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(id)
                        .orElseThrow(() -> new EventNotFoundException("Allowance not found"));

        if (!allowance.getEvent().getManager().equals(manager)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not authorized to view this allowance");
        }

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
        List<EventUserAllowance> allowances;
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            allowances = eventUserAllowanceRepository.listAll();
        } else {
            allowances = eventUserAllowanceRepository.find("event.manager", currentUser).list();
        }
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
        Event event = getEventById(eventId);
        if (!event.getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not authorized to view allowances for this event");
        }
        return eventUserAllowanceRepository.findByEventId(eventId).stream()
                .map(EventUserAllowancesDto::new)
                .collect(Collectors.toList());
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
        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(id)
                        .orElseThrow(() -> new EventNotFoundException("Allowance not found"));

        if (!allowance.getEvent().getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not authorized to delete this allowance");
        }

        eventUserAllowanceRepository.delete(allowance);
    }

    private Event getEventById(Long id) throws EventNotFoundException {
        return eventRepository
                .findByIdOptional(id)
                .orElseThrow(
                        () -> new EventNotFoundException("Event with id " + id + " not found"));
    }

    private User getUserById(Long id) throws UserNotFoundException {
        return userRepository
                .findByIdOptional(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }
}
