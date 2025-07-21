package de.felixhertweck.seatreservation.reservation.service;

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.EventNotFoundException;
import de.felixhertweck.seatreservation.reservation.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventServiceTest {

    @Inject EventService eventService;

    @InjectMock UserRepository userRepository;

    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    @InjectMock EventRepository eventRepository;

    private User user;
    private Event event;
    private EventUserAllowance allowance;

    @BeforeEach
    void setUp() {
        user = new User();
        user.id = 1L;
        user.setUsername("testuser");

        var location = new de.felixhertweck.seatreservation.model.entity.EventLocation();
        location.id = 1L;

        event = new Event();
        event.id = 1L;
        event.setEventLocation(location);

        allowance = new EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(event);
        allowance.setReservationsAllowedCount(5);
    }

    @Test
    void getEventsForCurrentUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance));

        List<EventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(event.id, result.getFirst().id());
    }

    @Test
    void getEventsForCurrentUser_UserNotFoundException() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> eventService.getEventsForCurrentUser("unknownuser"));
    }

    @Test
    void getEventsForCurrentUser_Success_NoEvents() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(Collections.emptyList());

        List<EventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableSeatsForCurrentUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventRepository.findByIdOptional(1L)).thenReturn(java.util.Optional.of(event));
        when(eventUserAllowanceRepository.findByEventIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.of(allowance));

        int availableSeats = eventService.getAvailableSeatsForCurrentUser(1L, "testuser");

        assertEquals(5, availableSeats);
    }

    @Test
    void getAvailableSeatsForCurrentUser_UserNotFoundException() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> eventService.getAvailableSeatsForCurrentUser(1L, "unknownuser"));
    }

    @Test
    void getAvailableSeatsForCurrentUser_NotFoundException_EventNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventRepository.findByIdOptional(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.getAvailableSeatsForCurrentUser(99L, "testuser"));
    }

    @Test
    void getAvailableSeatsForCurrentUser_ForbiddenException_NoAccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventRepository.findByIdOptional(1L)).thenReturn(java.util.Optional.of(event));
        when(eventUserAllowanceRepository.findByEventIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.getAvailableSeatsForCurrentUser(1L, "testuser"));
    }
}
