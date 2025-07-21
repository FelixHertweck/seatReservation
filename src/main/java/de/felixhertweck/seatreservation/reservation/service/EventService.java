package de.felixhertweck.seatreservation.reservation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.EventNotFoundException;
import de.felixhertweck.seatreservation.reservation.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;

@ApplicationScoped
public class EventService {

    @Inject UserRepository userRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject EventRepository eventRepository;

    @Transactional
    public List<EventResponseDTO> getEventsForCurrentUser(String username)
            throws UserNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("User not found: " + username);
        }

        Set<Event> accessibleEvents = new HashSet<>();

        eventUserAllowanceRepository.findByUser(user).stream()
                .map(EventUserAllowance::getEvent)
                .forEach(accessibleEvents::add);

        // Convert the set of unique events to a list and return it
        return accessibleEvents.stream().map(EventResponseDTO::new).toList();
    }

    @Transactional
    public int getAvailableSeatsForCurrentUser(Long eventId, String username)
            throws UserNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("User not found: " + username);
        }

        eventRepository
                .findByIdOptional(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByEventIdAndUserId(eventId, user.getId())
                        .orElseThrow(
                                () ->
                                        new EventNotFoundException(
                                                "User does not have access to this event: "
                                                        + eventId));

        return allowance.getReservationsAllowedCount();
    }
}
