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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesCreateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesResponseDto;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
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
    @Inject EventReservationAllowanceService eventReservationAllowanceService;

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
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusHours(12),
                        eventLocation,
                        managerUser);
        existingEvent.id = 1L;

        existingEvent.setReservations(List.of());
        existingEvent.setUserAllowances(Set.of());
    }

    @Test
    void createEvent_Success() {
        when(eventLocationRepository.findByIdWithManager(eq(1L)))
                .thenReturn(Optional.of(eventLocation));
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            event.id = 10L; // Simulate ID generation
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        Event createdEvent =
                eventService.createEvent(
                        "New Event",
                        "New Description",
                        LocalDateTime.now().plusDays(5),
                        LocalDateTime.now().plusDays(6),
                        LocalDateTime.now().plusDays(4),
                        1L,
                        managerUser);
        assertNotNull(createdEvent);
        assertEquals("New Event", createdEvent.getName());
        assertEquals(eventLocation.id, createdEvent.getEventLocation().id);
        verify(eventRepository, times(1)).persist(any(Event.class));
    }

    @Test
    void createEvent_IllegalArgumentException_LocationNotFound() {
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        eventService.createEvent(
                                "New Event",
                                "New Description",
                                LocalDateTime.now().plusDays(5),
                                LocalDateTime.now().plusDays(6),
                                LocalDateTime.now().plusDays(4),
                                99L,
                                managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void updateEvent_Success_AsManager() throws EventNotFoundException {
        when(eventRepository.findOptionalByIdWithManagerReservations(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        Event updatedEvent =
                eventService.updateEvent(
                        existingEvent.id,
                        "Updated Event",
                        "Updated Description",
                        LocalDateTime.now().plusDays(10),
                        LocalDateTime.now().plusDays(11),
                        LocalDateTime.now().plusDays(9),
                        eventLocation.id,
                        managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.getName());
        verify(eventRepository, times(1)).persist(existingEvent);
    }

    @Test
    void updateEvent_Success_AsAdmin() throws EventNotFoundException {
        when(eventRepository.findOptionalByIdWithManagerReservations(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        Event updatedEvent =
                eventService.updateEvent(
                        existingEvent.id,
                        "Updated Event Admin",
                        "Updated Description Admin",
                        LocalDateTime.now().plusDays(10),
                        LocalDateTime.now().plusDays(11),
                        LocalDateTime.now().plusDays(9),
                        eventLocation.id,
                        adminUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event Admin", updatedEvent.getName());
        verify(eventRepository, times(1)).persist(existingEvent);
    }

    @Test
    void updateEvent_EventNotFoundException() {
        when(eventRepository.findOptionalByIdWithManagerReservations(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventService.updateEvent(
                                99L,
                                "Updated Event",
                                "Updated Description",
                                LocalDateTime.now().plusDays(10),
                                LocalDateTime.now().plusDays(11),
                                LocalDateTime.now().plusDays(9),
                                eventLocation.id,
                                managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void updateEvent_ForbiddenException_NotManagerOrAdmin() {
        when(eventRepository.findOptionalByIdWithManagerReservations(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));

        assertThrows(
                SecurityException.class,
                () ->
                        eventService.updateEvent(
                                existingEvent.id,
                                "Updated Event",
                                "Updated Description",
                                LocalDateTime.now().plusDays(10),
                                LocalDateTime.now().plusDays(11),
                                LocalDateTime.now().plusDays(9),
                                eventLocation.id,
                                regularUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void updateEvent_IllegalArgumentException_LocationNotFound() {
        when(eventRepository.findOptionalByIdWithManagerReservations(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        eventService.updateEvent(
                                existingEvent.id,
                                "Updated Event",
                                "Updated Description",
                                LocalDateTime.now().plusDays(10),
                                LocalDateTime.now().plusDays(11),
                                LocalDateTime.now().plusDays(9),
                                99L,
                                managerUser));
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

        List<Event> result = eventService.getEventsByCurrentManager(adminUser);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository, times(1)).listAll();
        verify(eventRepository, never()).findByManager(any(User.class));
    }

    @Test
    void getEventsByCurrentManager_Success_AsManager() {
        List<Event> managerEvents = List.of(existingEvent);
        when(eventRepository.findByManager(managerUser)).thenReturn(managerEvents);

        List<Event> result = eventService.getEventsByCurrentManager(managerUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingEvent.getName(), result.getFirst().getName());
        verify(eventRepository, times(1)).findByManager(managerUser);
        verify(eventRepository, never()).listAll();
    }

    @Test
    void getEventsByCurrentManager_Success_NoEventsForManager() {
        when(eventRepository.findByManager(managerUser)).thenReturn(Collections.emptyList());

        List<Event> result = eventService.getEventsByCurrentManager(managerUser);

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

        when(eventRepository.findOptionalByIdWithManagerReservations(1L))
                .thenReturn(Optional.of(existingEvent));
        when(eventRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingEvent));
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

        when(eventRepository.findOptionalByIdWithManagerReservations(1L))
                .thenReturn(Optional.of(existingEvent));
        when(eventRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingEvent));
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

        when(eventRepository.findOptionalByIdWithManagerReservations(1L))
                .thenReturn(Optional.of(existingEvent));
        when(eventRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(regularUser.id)).thenReturn(Optional.of(regularUser));
        @SuppressWarnings("unchecked")
        io.quarkus.hibernate.orm.panache.PanacheQuery<EventUserAllowance> mockQuery =
                mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
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

        when(eventRepository.findOptionalByIdWithManagerReservations(anyLong()))
                .thenReturn(Optional.empty());

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

        when(eventRepository.findOptionalByIdWithManagerReservations(1L))
                .thenReturn(Optional.of(existingEvent));
        when(eventRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingEvent));
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

        when(eventRepository.findOptionalByIdWithManagerReservations(1L))
                .thenReturn(Optional.of(existingEvent));
        when(eventRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingEvent));
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
        when(eventRepository.findOptionalByIdWithManagerReservations(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventService.updateEvent(
                                99L,
                                "Updated Event",
                                null,
                                null,
                                null,
                                null,
                                eventLocation.id,
                                managerUser));
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

        EventUserAllowancesResponseDto result =
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

        EventUserAllowancesResponseDto result =
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

        EventUserAllowancesResponseDto result =
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

        EventUserAllowancesResponseDto result =
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
}
