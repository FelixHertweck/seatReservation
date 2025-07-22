package de.felixhertweck.seatreservation.reservation.service;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;

@ApplicationScoped
public class EventService {

    @Inject UserRepository userRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    @Transactional
    public List<EventResponseDTO> getEventsForCurrentUser(String username)
            throws UserNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("User not found: " + username);
        }

        return eventUserAllowanceRepository.findByUser(user).stream()
                .map(
                        allowance ->
                                new EventResponseDTO(
                                        allowance.getEvent(),
                                        allowance.getReservationsAllowedCount()))
                .toList();
    }
}
