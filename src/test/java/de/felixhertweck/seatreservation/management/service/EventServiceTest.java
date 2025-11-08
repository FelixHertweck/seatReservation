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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.email.service.NotificationService;
import de.felixhertweck.seatreservation.management.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.management.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.management.dto.EventUserAllowancesCreateDto;
import de.felixhertweck.seatreservation.management.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
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
    @InjectMock NotificationService notificationService;

    @Inject EventService eventService;
    @Inject EventReservationAllowanceService eventReservationAllowanceService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private EventLocation eventLocation;
    private Event existingEvent;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        Mockito.reset(eventRepository);
        Mockito.reset(eventLocationRepository);
        Mockito.reset(userRepository);
        Mockito.reset(eventUserAllowanceRepository);
        Mockito.reset(notificationService);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN),
                        Set.of());
        adminUser.id = 1L;
        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "Event",
                        "Manager",
                        Set.of(Roles.MANAGER),
                        Set.of());
        managerUser.id = 3L;
        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "Regular",
                        "User",
                        Set.of(Roles.USER),
                        Set.of());
        regularUser.id = 2L;

        eventLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser, 100);
        eventLocation.id = 1L;

        existingEvent =
                new Event(
                        "Konzert",
                        "Beschreibung",
                        Instant.now().plusSeconds(Duration.ofDays(1).toSeconds()),
                        Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()),
                        Instant.now().plusSeconds(Duration.ofHours(12).toSeconds()),
                        Instant.now().plusSeconds(Duration.ofHours(1).toSeconds()),
                        eventLocation,
                        managerUser,
                        Instant.now().plusSeconds(Duration.ofHours(13).toSeconds()));
        existingEvent.id = 1L;
    }

    @Test
    void createEvent_Success() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("New Event");
        dto.setDescription("New Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
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

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("New Event", createdEvent.name());
        assertEquals(eventLocation.id, createdEvent.eventLocationId());
        verify(eventRepository, times(1)).persist(any(Event.class));
    }

    @Test
    void createEvent_IllegalArgumentException_LocationNotFound() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("New Event");
        dto.setDescription("New Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
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
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(11).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(8).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(9).toSeconds()));
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        EventResponseDTO updatedEvent =
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
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(11).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(8).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(9).toSeconds()));
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        EventResponseDTO updatedEvent = eventService.updateEvent(existingEvent.id, dto, adminUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event Admin", updatedEvent.name());
        verify(eventRepository, times(1)).persist(existingEvent);
    }

    @Test
    void updateEvent_EventNotFoundException() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(11).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(9).toSeconds()));
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
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(11).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(9).toSeconds()));
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
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(11).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(9).toSeconds()));
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
                                Instant.now(),
                                Instant.now(),
                                Instant.now(),
                                Instant.now(),
                                eventLocation,
                                regularUser,
                                Instant.now()));
        when(eventRepository.listAll()).thenReturn(allEvents);

        List<EventResponseDTO> result = eventService.getEventsByCurrentManager(adminUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository, times(1)).listAll();
        verify(eventRepository, never()).findByManager(any(User.class));
    }

    @Test
    void getEventsByCurrentManager_Success_AsManager() {
        List<Event> managerEvents = List.of(existingEvent);
        when(eventRepository.findByManager(managerUser)).thenReturn(managerEvents);

        List<EventResponseDTO> result = eventService.getEventsByCurrentManager(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingEvent.getName(), result.getFirst().name());
        verify(eventRepository, times(1)).findByManager(managerUser);
        verify(eventRepository, never()).listAll();
    }

    @Test
    void getEventsByCurrentManager_Success_NoEventsForManager() {
        when(eventRepository.findByManager(managerUser)).thenReturn(Collections.emptyList());

        List<EventResponseDTO> result = eventService.getEventsByCurrentManager(managerUser);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).findByManager(managerUser);
        verify(eventRepository, never()).listAll();
    }

    @Test
    void setReservationsAllowedForUser_Success_NewAllowance()
            throws EventNotFoundException, UserNotFoundException {
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), existingEvent.id, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        // Mocking PanacheQuery for find method when no existing allowance is found
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> mockQueryNewAllowance = mock(PanacheQuery.class);
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

        eventReservationAllowanceService.setReservationsAllowedForUser(dto, managerUser);

        verify(eventUserAllowanceRepository, times(1)).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_Success_UpdateAllowance()
            throws EventNotFoundException, UserNotFoundException {
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), existingEvent.id, 10);
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        // Mocking PanacheQuery for find method
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> mockQuery = mock(PanacheQuery.class);
        when(eventUserAllowanceRepository.find(
                        "user = ?1 and event = ?2", regularUser, existingEvent))
                .thenReturn(mockQuery);
        when(mockQuery.firstResultOptional()).thenReturn(Optional.of(existingAllowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(EventUserAllowance.class));

        eventReservationAllowanceService.setReservationsAllowedForUser(dto, managerUser);

        assertEquals(10, existingAllowance.getReservationsAllowedCount());
        verify(eventUserAllowanceRepository, times(1)).persist(existingAllowance);
    }

    @Test
    void setReservationsAllowedForUser_Success_AsAdmin()
            throws EventNotFoundException, UserNotFoundException {
        // Arrange
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), existingEvent.id, 5);
        ArgumentCaptor<EventUserAllowance> allowanceCaptor =
                ArgumentCaptor.forClass(EventUserAllowance.class);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        @SuppressWarnings("unchecked")
        PanacheQuery<EventUserAllowance> mockQuery = mock(PanacheQuery.class);
        when(eventUserAllowanceRepository.find(
                        "user = ?1 and event = ?2", regularUser, existingEvent))
                .thenReturn(mockQuery);
        when(mockQuery.firstResultOptional()).thenReturn(Optional.empty());

        // Act
        eventReservationAllowanceService.setReservationsAllowedForUser(dto, adminUser);

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
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), 99L, 5);

        when(eventRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventReservationAllowanceService.setReservationsAllowedForUser(
                                dto, managerUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_UserNotFoundException() {
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(99L), existingEvent.id, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () ->
                        eventReservationAllowanceService.setReservationsAllowedForUser(
                                dto, managerUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_ForbiddenException_NotManagerOrAdmin() {
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), existingEvent.id, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));

        assertThrows(
                SecurityException.class,
                () ->
                        eventReservationAllowanceService.setReservationsAllowedForUser(
                                dto, regularUser));
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
    void updateReservationAllowance_Success_AsManager()
            throws EventNotFoundException, SecurityException {
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);
        existingAllowance.id = 1L;
        EventUserAllowanceUpdateDto dto =
                new EventUserAllowanceUpdateDto(
                        existingAllowance.id, existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(existingAllowance.id))
                .thenReturn(Optional.of(existingAllowance));

        EventUserAllowancesDto result =
                eventReservationAllowanceService.updateReservationAllowance(dto, managerUser);

        assertNotNull(result);
        assertEquals(10, result.reservationsAllowedCount());
        verify(eventUserAllowanceRepository, times(1)).persist(existingAllowance);
    }

    @Test
    void updateReservationAllowance_Success_AsAdmin()
            throws EventNotFoundException, SecurityException {
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);
        existingAllowance.id = 1L;
        EventUserAllowanceUpdateDto dto =
                new EventUserAllowanceUpdateDto(
                        existingAllowance.id, existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(existingAllowance.id))
                .thenReturn(Optional.of(existingAllowance));

        EventUserAllowancesDto result =
                eventReservationAllowanceService.updateReservationAllowance(dto, adminUser);

        assertNotNull(result);
        assertEquals(10, result.reservationsAllowedCount());
        verify(eventUserAllowanceRepository, times(1)).persist(existingAllowance);
    }

    @Test
    void updateReservationAllowance_EventNotFoundException_AllowanceNotFound() {
        EventUserAllowanceUpdateDto dto =
                new EventUserAllowanceUpdateDto(99L, existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventReservationAllowanceService.updateReservationAllowance(
                                dto, managerUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void updateReservationAllowance_SecurityException_NotManagerOrAdmin() {
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);
        existingAllowance.id = 1L;
        EventUserAllowanceUpdateDto dto =
                new EventUserAllowanceUpdateDto(
                        existingAllowance.id, existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(existingAllowance.id))
                .thenReturn(Optional.of(existingAllowance));

        assertThrows(
                SecurityException.class,
                () ->
                        eventReservationAllowanceService.updateReservationAllowance(
                                dto, regularUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void getReservationAllowanceById_Success_AsManager() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = 1L;

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        EventUserAllowancesDto result =
                eventReservationAllowanceService.getReservationAllowanceById(
                        allowance.id, managerUser);

        assertNotNull(result);
        assertEquals(regularUser.id, result.userId());
        assertEquals(existingEvent.id, result.eventId());
        assertEquals(5, result.reservationsAllowedCount());
        assertEquals(allowance.id, result.id());
    }

    @Test
    void getReservationAllowanceById_Success_AsAdmin() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = 1L;

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        EventUserAllowancesDto result =
                eventReservationAllowanceService.getReservationAllowanceById(
                        allowance.id, adminUser);

        assertNotNull(result);
        assertEquals(regularUser.id, result.userId());
        assertEquals(allowance.id, result.id());
    }

    @Test
    void getReservationAllowanceById_ForbiddenException_NotManagerOrAdmin() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = 1L;

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        assertThrows(
                SecurityException.class,
                () ->
                        eventReservationAllowanceService.getReservationAllowanceById(
                                allowance.id, regularUser));
    }

    @Test
    void getReservationAllowanceById_EventNotFoundException() {
        when(eventUserAllowanceRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventReservationAllowanceService.getReservationAllowanceById(
                                99L, managerUser));
    }

    @Test
    void createEvent_WithReminderSendDate_Success() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Event with Reminder");
        dto.setDescription("Event Description");
        Instant eventStart = Instant.now().plusSeconds(Duration.ofDays(5).toSeconds());
        Instant reminderDate = Instant.now().plusSeconds(Duration.ofDays(4).toSeconds());
        dto.setStartTime(eventStart);
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(reminderDate);
        dto.setEventLocationId(eventLocation.id);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            event.id = 10L;
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("Event with Reminder", createdEvent.name());
        assertEquals(reminderDate, createdEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
    }

    @Test
    void updateEvent_WithReminderSendDate_Success() throws Exception {
        Instant newReminderDate = Instant.now().plusSeconds(Duration.ofDays(3).toSeconds());
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(newReminderDate);
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        EventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        assertEquals(newReminderDate, updatedEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
    }

    @Test
    void findEventsWithReminderDateBetween_Success() {
        Instant start = Instant.now();
        Instant end = Instant.now().plusSeconds(Duration.ofDays(1).toSeconds());

        Event event1 = new Event();
        event1.setName("Event 1");
        event1.setReminderSendDate(start.plusSeconds(3600)); // 1 hour after start

        Event event2 = new Event();
        event2.setName("Event 2");
        event2.setReminderSendDate(start.plusSeconds(7200)); // 2 hours after start

        when(eventRepository.find("reminderSendDate BETWEEN ?1 AND ?2", start, end))
                .thenReturn(mock(PanacheQuery.class));
        when(eventRepository.find("reminderSendDate BETWEEN ?1 AND ?2", start, end).list())
                .thenReturn(List.of(event1, event2));

        List<Event> events = eventService.findEventsWithReminderDateBetween(start, end);

        assertNotNull(events);
        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> "Event 1".equals(e.getName())));
        assertTrue(events.stream().anyMatch(e -> "Event 2".equals(e.getName())));
    }

    @Test
    void createEvent_WithoutReminderSendDate_NoReminderScheduled() {
        // Event without reminder date should not schedule a reminder
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Event without Reminder");
        dto.setDescription("Event Description");
        Instant eventStart = Instant.now().plusSeconds(Duration.ofDays(5).toSeconds());
        dto.setStartTime(eventStart);
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(null); // No reminder date
        dto.setEventLocationId(eventLocation.id);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            event.id = 10L;
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("Event without Reminder", createdEvent.name());
        assertEquals(null, createdEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
        // Verify that no reminder was scheduled
        verify(notificationService, never()).scheduleEventReminder(any(Event.class));
    }

    @Test
    void createEvent_WithReminderSendDate_ReminderScheduled() {
        // Event with reminder date should schedule a reminder
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Event with Reminder");
        dto.setDescription("Event Description");
        Instant eventStart = Instant.now().plusSeconds(Duration.ofDays(5).toSeconds());
        Instant reminderDate = Instant.now().plusSeconds(Duration.ofDays(4).toSeconds());
        dto.setStartTime(eventStart);
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(reminderDate);
        dto.setEventLocationId(eventLocation.id);

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            event.id = 10L;
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("Event with Reminder", createdEvent.name());
        assertEquals(reminderDate, createdEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
        // Verify that reminder was scheduled
        verify(notificationService, times(1)).scheduleEventReminder(any(Event.class));
    }

    @Test
    void updateEvent_FromNoReminderToReminder_ReminderScheduled() throws Exception {
        // Update event from no reminder to having a reminder
        existingEvent.setReminderSendDate(null); // Start without reminder

        Instant newReminderDate = Instant.now().plusSeconds(Duration.ofDays(3).toSeconds());
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(newReminderDate);
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        EventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        assertEquals(newReminderDate, updatedEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
        // Should cancel old (non-existent) reminder and schedule new one
        verify(notificationService, times(1)).cancelEventReminder(existingEvent.id);
        verify(notificationService, times(1)).scheduleEventReminder(any(Event.class));
    }

    @Test
    void updateEvent_FromReminderToNoReminder_ReminderCancelled() throws Exception {
        // Update event from having a reminder to no reminder
        Instant oldReminderDate = Instant.now().plusSeconds(Duration.ofDays(3).toSeconds());
        existingEvent.setReminderSendDate(oldReminderDate); // Start with reminder

        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(null); // Remove reminder
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        EventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        assertEquals(null, updatedEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
        // Should cancel old reminder and not schedule a new one
        verify(notificationService, times(1)).cancelEventReminder(existingEvent.id);
        verify(notificationService, never()).scheduleEventReminder(any(Event.class));
    }

    @Test
    void updateEvent_FromReminderToNewReminder_ReminderRescheduled() throws Exception {
        // Update event from one reminder time to another
        Instant oldReminderDate = Instant.now().plusSeconds(Duration.ofDays(3).toSeconds());
        existingEvent.setReminderSendDate(oldReminderDate); // Start with old reminder

        Instant newReminderDate = Instant.now().plusSeconds(Duration.ofDays(2).toSeconds());
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setReminderSendDate(newReminderDate); // New reminder date
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        EventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        assertEquals(newReminderDate, updatedEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));
        // Should cancel old reminder and schedule new one
        verify(notificationService, times(1)).cancelEventReminder(existingEvent.id);
        verify(notificationService, times(1)).scheduleEventReminder(any(Event.class));
    }
}
