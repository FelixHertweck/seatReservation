package de.felixhertweck.seatreservation.eventManagement.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
public class EventServiceTest {

    @InjectMock EventRepository eventRepository;
    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    @Inject EventService eventService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private EventLocation eventLocation;
    private Event existingEvent;

    @BeforeEach
    void setUp() {
        Mockito.reset(eventRepository);
        Mockito.reset(eventLocationRepository);
        Mockito.reset(userRepository);
        Mockito.reset(eventUserAllowanceRepository);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        "hash",
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN));
        adminUser.id = 1L;
        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        "hash",
                        "Event",
                        "Manager",
                        Set.of(Roles.MANAGER));
        managerUser.id = 3L;
        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        "hash",
                        "Regular",
                        "User",
                        Set.of(Roles.USER));
        regularUser.id = 2L;

        eventLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        eventLocation.id = 1L;

        existingEvent =
                new Event(
                        "Konzert",
                        "Beschreibung",
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusHours(12),
                        eventLocation,
                        managerUser);
        existingEvent.id = 1L;
    }

    @Test
    void createEvent_Success() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("New Event");
        dto.setDescription("New Description");
        dto.setStartTime(LocalDateTime.now().plusDays(5));
        dto.setEndTime(LocalDateTime.now().plusDays(6));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(4));
        dto.setEventLocationId(eventLocation.id);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            event.id = 10L; // Simulate ID generation
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        DetailedEventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("New Event", createdEvent.name());
        assertEquals(eventLocation.id, createdEvent.location().id());
        verify(eventRepository, times(1)).persist(any(Event.class));
    }

    @Test
    void createEvent_IllegalArgumentException_LocationNotFound() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("New Event");
        dto.setDescription("New Description");
        dto.setStartTime(LocalDateTime.now().plusDays(5));
        dto.setEndTime(LocalDateTime.now().plusDays(6));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(4));
        dto.setEventLocationId(99L); // Non-existent location ID

        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class, () -> eventService.createEvent(dto, managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void updateEvent_Success_AsManager() throws EventNotFoundException {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(LocalDateTime.now().plusDays(10));
        dto.setEndTime(LocalDateTime.now().plusDays(11));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(9));
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        DetailedEventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        verify(eventRepository, times(1)).persist(existingEvent);
    }

    @Test
    void updateEvent_Success_AsAdmin() throws EventNotFoundException {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event Admin");
        dto.setDescription("Updated Description Admin");
        dto.setStartTime(LocalDateTime.now().plusDays(10));
        dto.setEndTime(LocalDateTime.now().plusDays(11));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(9));
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        DetailedEventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, adminUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event Admin", updatedEvent.name());
        verify(eventRepository, times(1)).persist(existingEvent);
    }

    @Test
    void updateEvent_EventNotFoundException() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(LocalDateTime.now().plusDays(10));
        dto.setEndTime(LocalDateTime.now().plusDays(11));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(9));
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.updateEvent(99L, dto, managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void updateEvent_ForbiddenException_NotManagerOrAdmin() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(LocalDateTime.now().plusDays(10));
        dto.setEndTime(LocalDateTime.now().plusDays(11));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(9));
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                SecurityException.class,
                () -> eventService.updateEvent(existingEvent.id, dto, regularUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void updateEvent_IllegalArgumentException_LocationNotFound() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(LocalDateTime.now().plusDays(10));
        dto.setEndTime(LocalDateTime.now().plusDays(11));
        dto.setBookingDeadline(LocalDateTime.now().plusDays(9));
        dto.setEventLocationId(99L); // Non-existent location ID

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(existingEvent.id, dto, managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void getEventsByCurrentManager_Success_AsAdmin() {
        List<Event> allEvents =
                List.of(
                        existingEvent,
                        new Event(
                                "Another Event",
                                "Desc",
                                LocalDateTime.now(),
                                LocalDateTime.now(),
                                LocalDateTime.now(),
                                eventLocation,
                                regularUser));
        when(eventRepository.listAll()).thenReturn(allEvents);

        List<DetailedEventResponseDTO> result = eventService.getEventsByCurrentManager(adminUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository, times(1)).listAll();
        verify(eventRepository, never()).findByManager(any(User.class));
    }

    @Test
    void getEventsByCurrentManager_Success_AsManager() {
        List<Event> managerEvents = List.of(existingEvent);
        when(eventRepository.findByManager(managerUser)).thenReturn(managerEvents);

        List<DetailedEventResponseDTO> result = eventService.getEventsByCurrentManager(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingEvent.getName(), result.getFirst().name());
        verify(eventRepository, times(1)).findByManager(managerUser);
        verify(eventRepository, never()).listAll();
    }

    @Test
    void getEventsByCurrentManager_Success_NoEventsForManager() {
        when(eventRepository.findByManager(managerUser)).thenReturn(Collections.emptyList());

        List<DetailedEventResponseDTO> result = eventService.getEventsByCurrentManager(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).findByManager(managerUser);
        verify(eventRepository, never()).listAll();
    }

    @Test
    void setReservationsAllowedForUser_Success_NewAllowance()
            throws EventNotFoundException, UserNotFoundException {
        EventUserAllowancesDto dto =
                new EventUserAllowancesDto(existingEvent.id, regularUser.id, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        // Mocking PanacheQuery for find method when no existing allowance is found
        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<EventUserAllowance> mockQueryNewAllowance =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventUserAllowanceRepository.find(
                        "user = ?1 and event = ?2", regularUser, existingEvent))
                .thenReturn(mockQueryNewAllowance);
        when(mockQueryNewAllowance.firstResultOptional()).thenReturn(Optional.empty());
        doAnswer(
                        invocation -> {
                            EventUserAllowance allowance = invocation.getArgument(0);
                            allowance.id = 1L; // Simulate ID generation
                            return null;
                        })
                .when(eventUserAllowanceRepository)
                .persist(any(EventUserAllowance.class));

        eventService.setReservationsAllowedForUser(dto, managerUser);

        verify(eventUserAllowanceRepository, times(1)).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_Success_UpdateAllowance()
            throws EventNotFoundException, UserNotFoundException {
        EventUserAllowancesDto dto =
                new EventUserAllowancesDto(existingEvent.id, regularUser.id, 10);
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        // Mocking PanacheQuery for find method
        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<EventUserAllowance> mockQuery =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventUserAllowanceRepository.find(
                        "user = ?1 and event = ?2", regularUser, existingEvent))
                .thenReturn(mockQuery);
        when(mockQuery.firstResultOptional()).thenReturn(Optional.of(existingAllowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        eventService.setReservationsAllowedForUser(dto, managerUser);

        assertEquals(10, existingAllowance.getReservationsAllowedCount());
        verify(eventUserAllowanceRepository, times(1)).persist(existingAllowance);
    }

    @Test
    void setReservationsAllowedForUser_Success_AsAdmin()
            throws EventNotFoundException, UserNotFoundException {
        // Arrange
        EventUserAllowancesDto dto =
                new EventUserAllowancesDto(existingEvent.id, regularUser.id, 5);
        ArgumentCaptor<EventUserAllowance> allowanceCaptor =
                ArgumentCaptor.forClass(EventUserAllowance.class);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<EventUserAllowance> mockQuery =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(eventUserAllowanceRepository.find(
                        "user = ?1 and event = ?2", regularUser, existingEvent))
                .thenReturn(mockQuery);
        when(mockQuery.firstResultOptional()).thenReturn(Optional.empty());

        // Act
        eventService.setReservationsAllowedForUser(dto, adminUser);

        // Assert
        verify(eventUserAllowanceRepository).persist(allowanceCaptor.capture());
        EventUserAllowance capturedAllowance = allowanceCaptor.getValue();

        assertNotNull(capturedAllowance);
        assertEquals(regularUser, capturedAllowance.getUser());
        assertEquals(existingEvent, capturedAllowance.getEvent());
        assertEquals(5, capturedAllowance.getReservationsAllowedCount());
    }

    @Test
    void setReservationsAllowedForUser_EventNotFoundException() {
        EventUserAllowancesDto dto = new EventUserAllowancesDto(99L, regularUser.id, 5);

        when(eventRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.setReservationsAllowedForUser(dto, managerUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_UserNotFoundException() {
        EventUserAllowancesDto dto = new EventUserAllowancesDto(existingEvent.id, 99L, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> eventService.setReservationsAllowedForUser(dto, managerUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_ForbiddenException_NotManagerOrAdmin() {
        EventUserAllowancesDto dto =
                new EventUserAllowancesDto(existingEvent.id, regularUser.id, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));

        assertThrows(
                SecurityException.class,
                () -> eventService.setReservationsAllowedForUser(dto, regularUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void updateEvent_NotFound() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.updateEvent(99L, dto, managerUser));
    }

    @Test
    void getReservationAllowanceById_Success_AsManager() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = 1L;

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        EventUserAllowancesDto result =
                eventService.getReservationAllowanceById(allowance.id, managerUser);

        assertNotNull(result);
        assertEquals(regularUser.id, result.userId());
        assertEquals(existingEvent.id, result.eventId());
        assertEquals(5, result.reservationsAllowedCount());
    }

    @Test
    void getReservationAllowanceById_Success_AsAdmin() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = 1L;

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        EventUserAllowancesDto result =
                eventService.getReservationAllowanceById(allowance.id, adminUser);

        assertNotNull(result);
        assertEquals(regularUser.id, result.userId());
    }

    @Test
    void getReservationAllowanceById_ForbiddenException_NotManagerOrAdmin() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = 1L;

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        assertThrows(
                SecurityException.class,
                () -> eventService.getReservationAllowanceById(allowance.id, regularUser));
    }

    @Test
    void getReservationAllowanceById_EventNotFoundException() {
        when(eventUserAllowanceRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.getReservationAllowanceById(99L, managerUser));
    }
}
