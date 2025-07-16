package de.felixhertweck.seatreservation.manager.service;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;

import de.felixhertweck.seatreservation.manager.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.manager.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.manager.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.user.EventNotFoundException;
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
    public EventResponseDTO createEvent(EventRequestDTO dto, User manager) {
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
     * @throws ForbiddenException If the user is not authorized to update the Event.
     */
    @Transactional
    public EventResponseDTO updateEvent(Long id, EventRequestDTO dto, User manager) {
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
            throw new ForbiddenException("User is not the manager of this event");
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
        return new EventResponseDTO(event);
    }

    /**
     * Retrieves a list of Events belonging to the currently authenticated manager. Access control:
     * If the user is an administrator, all Events are returned. Otherwise, only Events whose
     * manager is the current user are returned.
     *
     * @return A list of DTOs representing the Events.
     * @throws ForbiddenException If no authenticated user is found.
     */
    public List<EventResponseDTO> getEventsByCurrentManager(User manager)
            throws ForbiddenException {
        List<Event> events;
        // Access control: If the user is an ADMIN, all Events are returned.
        // Otherwise, only Events belonging to this manager are returned.
        if (manager.getRoles().contains(Roles.ADMIN)) {
            events = eventRepository.listAll();
        } else {
            events = eventRepository.findByManager(manager);
        }
        return events.stream().map(EventResponseDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public void setReservationsAllowedForUser(EventUserAllowancesDto dto, User manager)
            throws EventNotFoundException, UserNotFoundException, ForbiddenException {
        Event event = getEventById(dto.eventId());
        User user = getUserById(dto.userId());

        if (!event.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new ForbiddenException("User is not the manager of this event");
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
