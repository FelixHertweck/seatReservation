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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.events.EventCreatedEvent;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
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
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
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

    @InjectMock
    jakarta.enterprise.event.Event<de.felixhertweck.seatreservation.common.events.EventCreatedEvent>
            eventCreatedBus;

    @InjectMock
    jakarta.enterprise.event.Event<de.felixhertweck.seatreservation.common.events.EventUpdatedEvent>
            eventUpdatedBus;

    @InjectMock
    jakarta.enterprise.event.Event<de.felixhertweck.seatreservation.common.events.EventDeletedEvent>
            eventDeletedBus;

    @InjectMock
    jakarta.enterprise.event.Event<
                    de.felixhertweck.seatreservation.common.events.EventRescheduledEvent>
            eventRescheduledBus;

    @Inject EventService eventService;
    @Inject EventReservationAllowanceService eventReservationAllowanceService;

    private User adminUser;
    private User managerUser;
    private User coManagerUser;
    private User supervisorUser;
    private User regularUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser coManagerAuth;
    private AuthenticatedUser regularAuth;
    private EventLocation eventLocation;
    private Event existingEvent;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        Mockito.reset(eventRepository);
        Mockito.reset(eventLocationRepository);
        Mockito.reset(userRepository);
        Mockito.reset(eventUserAllowanceRepository);
        Mockito.reset(eventCreatedBus);
        Mockito.reset(eventUpdatedBus);
        Mockito.reset(eventDeletedBus);
        Mockito.reset(eventRescheduledBus);

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
        adminUser.id = id(1);
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
        managerUser.id = id(3);
        coManagerUser =
                new User(
                        "comanager",
                        "comanager@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "Co",
                        "Manager",
                        Set.of(Roles.MANAGER),
                        Set.of());
        coManagerUser.id = id(8);
        supervisorUser =
                new User(
                        "supervisor",
                        "supervisor@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "Event",
                        "Supervisor",
                        Set.of(Roles.SUPERVISOR),
                        Set.of());
        supervisorUser.id = id(4);
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
        regularUser.id = id(2);

        adminAuth = new AuthenticatedUser(adminUser.id, adminUser.getRoles());
        managerAuth = new AuthenticatedUser(managerUser.id, managerUser.getRoles());
        coManagerAuth = new AuthenticatedUser(coManagerUser.id, coManagerUser.getRoles());
        regularAuth = new AuthenticatedUser(regularUser.id, regularUser.getRoles());
        when(userRepository.getReference(managerUser.id)).thenReturn(managerUser);
        when(userRepository.getReference(coManagerUser.id)).thenReturn(coManagerUser);

        eventLocation = new EventLocation("Stadthalle", "Hauptstraße 1", managerUser);
        eventLocation.id = id(1);

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
                        Instant.now().plusSeconds(Duration.ofHours(13).toSeconds()),
                        Set.of(supervisorUser));
        existingEvent.id = id(1);

        when(eventRepository.isUserManager(any(UUID.class), any(UUID.class)))
                .thenAnswer(
                        invocation -> {
                            UUID eventId = invocation.getArgument(0);
                            UUID userId = invocation.getArgument(1);
                            if (existingEvent != null && existingEvent.id.equals(eventId)) {
                                return existingEvent.getManagers() != null
                                        && existingEvent.getManagers().stream()
                                                .anyMatch(u -> u.id != null && u.id.equals(userId));
                            }
                            return false;
                        });
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
                            event.id = id(10); // Simulate ID generation
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
    void createEvent_WithSupervisors_Success() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("New Event");
        dto.setDescription("New Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setEventLocationId(eventLocation.id);
        dto.setSupervisorIds(Set.of(supervisorUser.id));

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(userRepository.findByIds(List.of(supervisorUser.id)))
                .thenReturn(List.of(supervisorUser));
        doAnswer(
                        invocation -> {
                            Event event = invocation.getArgument(0);
                            event.id = id(11); // Simulate ID generation
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("New Event", createdEvent.name());
        assertEquals(eventLocation.id, createdEvent.eventLocationId());
        assertTrue(createdEvent.supervisorIds().contains(supervisorUser.id));
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
        dto.setEventLocationId(id(99)); // Non-existent location ID

        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> eventService.createEvent(dto, managerUser));
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
        verify(eventUpdatedBus, times(1))
                .fireAsync(
                        any(
                                de.felixhertweck.seatreservation.common.events.EventUpdatedEvent
                                        .class));
    }

    @Test
    void updateEvent_Success_AsCoManager() throws EventNotFoundException {
        // coManagerUser is a co-manager, not the event's creator - proves access isn't limited to
        // whoever created the event.
        existingEvent.getManagers().add(coManagerUser);

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
                eventService.updateEvent(existingEvent.id, dto, coManagerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        verify(eventRepository, times(1)).persist(existingEvent);
    }

    @Test
    void updateEvent_SupervisorsUpdated_Success_AsManager() throws EventNotFoundException {
        // prepare DTO with a different supervisor (regularUser)
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(10).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(11).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(8).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(9).toSeconds()));
        dto.setEventLocationId(eventLocation.id);
        dto.setSupervisorIds(Set.of(regularUser.id));

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(userRepository.findByIds(List.of(regularUser.id))).thenReturn(List.of(regularUser));

        EventResponseDTO updatedEvent =
                eventService.updateEvent(existingEvent.id, dto, managerUser);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.name());
        // verify that supervisors were updated to the new id
        assertTrue(updatedEvent.supervisorIds().contains(regularUser.id));
        // ensure old supervisor is no longer present
        assertTrue(!updatedEvent.supervisorIds().contains(supervisorUser.id));
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

        when(eventRepository.findByIdOptional(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.updateEvent(id(99), dto, managerUser));
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
                AccessDeniedException.class,
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
        dto.setEventLocationId(id(99)); // Non-existent location ID

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                ValidationException.class,
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
                                Instant.now(),
                                Set.of(supervisorUser)));
        when(eventRepository.listAll()).thenReturn(allEvents);

        List<EventResponseDTO> result = eventService.getEventsByCurrentManager(adminAuth);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository, times(1)).listAll();
        verify(eventRepository, never()).findByManager(any(User.class));
    }

    @Test
    void getEventsByCurrentManager_Success_AsManager() {
        List<Event> managerEvents = List.of(existingEvent);
        when(eventRepository.findByManager(managerUser)).thenReturn(managerEvents);

        List<EventResponseDTO> result = eventService.getEventsByCurrentManager(managerAuth);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingEvent.getName(), result.getFirst().name());
        verify(eventRepository, times(1)).findByManager(managerUser);
        verify(eventRepository, never()).listAll();
    }

    @Test
    void getEventsByCurrentManager_Success_NoEventsForManager() {
        when(eventRepository.findByManager(managerUser)).thenReturn(Collections.emptyList());

        List<EventResponseDTO> result = eventService.getEventsByCurrentManager(managerAuth);

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
        when(userRepository.findByIds(dto.getUserIds().stream().toList()))
                .thenReturn(List.of(regularUser));
        // Mocking the batch allowance lookup to report no existing allowance
        when(eventUserAllowanceRepository.findByEventAndUserIds(existingEvent, dto.getUserIds()))
                .thenReturn(List.of());
        doAnswer(
                        invocation -> {
                            Iterable<EventUserAllowance> allowances = invocation.getArgument(0);
                            for (EventUserAllowance allowance : allowances) {
                                allowance.id = id(100);
                            }
                            return null;
                        })
                .when(eventUserAllowanceRepository)
                .persist(any(Iterable.class));

        eventReservationAllowanceService.setReservationsAllowedForUser(dto, managerAuth);

        verify(eventUserAllowanceRepository, times(1)).persist(any(Iterable.class));
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
        when(userRepository.findByIds(dto.getUserIds().stream().toList()))
                .thenReturn(List.of(regularUser));
        when(eventUserAllowanceRepository.findByEventAndUserIds(existingEvent, dto.getUserIds()))
                .thenReturn(List.of(existingAllowance));
        doNothing().when(eventUserAllowanceRepository).persist(any(Iterable.class));

        eventReservationAllowanceService.setReservationsAllowedForUser(dto, managerAuth);

        assertEquals(10, existingAllowance.getReservationsAllowedCount());
        verify(eventUserAllowanceRepository, times(1)).persist(List.of(existingAllowance));
    }

    @Test
    void setReservationsAllowedForUser_Success_AsAdmin()
            throws EventNotFoundException, UserNotFoundException {
        // Arrange
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), existingEvent.id, 5);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<EventUserAllowance>> allowanceCaptor =
                ArgumentCaptor.forClass(Iterable.class);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIds(dto.getUserIds().stream().toList()))
                .thenReturn(List.of(regularUser));
        when(eventUserAllowanceRepository.findByEventAndUserIds(existingEvent, dto.getUserIds()))
                .thenReturn(List.of());

        // Act
        eventReservationAllowanceService.setReservationsAllowedForUser(dto, adminAuth);

        // Assert
        verify(eventUserAllowanceRepository).persist(allowanceCaptor.capture());
        EventUserAllowance capturedAllowance = allowanceCaptor.getValue().iterator().next();

        assertNotNull(capturedAllowance);
        assertEquals(regularUser, capturedAllowance.getUser());
        assertEquals(existingEvent, capturedAllowance.getEvent());
        assertEquals(5, capturedAllowance.getReservationsAllowedCount());
    }

    @Test
    void setReservationsAllowedForUser_EventNotFoundException() {
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(regularUser.id), id(99), 5);

        when(eventRepository.findByIdOptional(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventReservationAllowanceService.setReservationsAllowedForUser(
                                dto, managerAuth));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void setReservationsAllowedForUser_UserNotFoundException() {
        EventUserAllowancesCreateDto dto =
                new EventUserAllowancesCreateDto(Set.of(id(99)), existingEvent.id, 5);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIds(dto.getUserIds().stream().toList())).thenReturn(List.of());
        when(eventUserAllowanceRepository.findByEventAndUserIds(existingEvent, dto.getUserIds()))
                .thenReturn(List.of());

        assertThrows(
                UserNotFoundException.class,
                () ->
                        eventReservationAllowanceService.setReservationsAllowedForUser(
                                dto, managerAuth));
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
                AccessDeniedException.class,
                () ->
                        eventReservationAllowanceService.setReservationsAllowedForUser(
                                dto, regularAuth));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void updateEvent_NotFound() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Event");
        dto.setEventLocationId(eventLocation.id);

        when(eventRepository.findByIdOptional(id(99))).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.updateEvent(id(99), dto, managerUser));
    }

    @Test
    void updateReservationAllowance_Success_AsManager()
            throws EventNotFoundException, SecurityException {
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);
        existingAllowance.id = id(1);
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
    void updateReservationAllowance_Success_AsCoManager()
            throws EventNotFoundException, SecurityException {
        // coManagerUser is a co-manager, not the event's creator.
        existingEvent.getManagers().add(coManagerUser);

        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);
        existingAllowance.id = id(1);
        EventUserAllowanceUpdateDto dto =
                new EventUserAllowanceUpdateDto(
                        existingAllowance.id, existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(existingAllowance.id))
                .thenReturn(Optional.of(existingAllowance));

        EventUserAllowancesDto result =
                eventReservationAllowanceService.updateReservationAllowance(dto, coManagerUser);

        assertNotNull(result);
        assertEquals(10, result.reservationsAllowedCount());
        verify(eventUserAllowanceRepository, times(1)).persist(existingAllowance);
    }

    @Test
    void updateReservationAllowance_Success_AsAdmin()
            throws EventNotFoundException, SecurityException {
        EventUserAllowance existingAllowance =
                new EventUserAllowance(regularUser, existingEvent, 5);
        existingAllowance.id = id(1);
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
                new EventUserAllowanceUpdateDto(id(99), existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

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
        existingAllowance.id = id(1);
        EventUserAllowanceUpdateDto dto =
                new EventUserAllowanceUpdateDto(
                        existingAllowance.id, existingEvent.id, regularUser.id, 10);

        when(eventUserAllowanceRepository.findByIdOptional(existingAllowance.id))
                .thenReturn(Optional.of(existingAllowance));

        assertThrows(
                AccessDeniedException.class,
                () ->
                        eventReservationAllowanceService.updateReservationAllowance(
                                dto, regularUser));
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));
    }

    @Test
    void getReservationAllowanceById_Success_AsManager() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = id(1);

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
    void getReservationAllowanceById_Success_AsCoManager() {
        // coManagerUser is a co-manager, not the event's creator.
        existingEvent.getManagers().add(coManagerUser);

        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = id(1);

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        EventUserAllowancesDto result =
                eventReservationAllowanceService.getReservationAllowanceById(
                        allowance.id, coManagerUser);

        assertNotNull(result);
        assertEquals(regularUser.id, result.userId());
        assertEquals(existingEvent.id, result.eventId());
        assertEquals(5, result.reservationsAllowedCount());
        assertEquals(allowance.id, result.id());
    }

    @Test
    void getReservationAllowanceById_Success_AsAdmin() {
        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = id(1);

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
        allowance.id = id(1);

        when(eventUserAllowanceRepository.findByIdOptional(allowance.id))
                .thenReturn(Optional.of(allowance));

        assertThrows(
                AccessDeniedException.class,
                () ->
                        eventReservationAllowanceService.getReservationAllowanceById(
                                allowance.id, regularUser));
    }

    @Test
    void getReservationAllowances_Success_AsCoManager() {
        // coManagerUser is a co-manager, not the event's creator - the "list my allowances"
        // endpoint must include allowances for events reached only through co-management.
        existingEvent.getManagers().add(coManagerUser);

        EventUserAllowance allowance = new EventUserAllowance(regularUser, existingEvent, 5);
        allowance.id = id(1);

        when(eventUserAllowanceRepository.findByEventManager(coManagerUser))
                .thenReturn(List.of(allowance));

        List<EventUserAllowancesDto> result =
                eventReservationAllowanceService.getReservationAllowances(coManagerAuth);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(allowance.id, result.get(0).id());
        assertEquals(existingEvent.id, result.get(0).eventId());
    }

    @Test
    void getReservationAllowanceById_EventNotFoundException() {
        when(eventUserAllowanceRepository.findByIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () ->
                        eventReservationAllowanceService.getReservationAllowanceById(
                                id(99), managerUser));
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
                            event.id = id(10);
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

        when(eventRepository.findByReminderSendDateBetween(start, end))
                .thenReturn(List.of(event1, event2));

        List<Event> events = eventService.findEventsWithReminderDateBetween(start, end);

        assertNotNull(events);
        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> "Event 1".equals(e.getName())));
        assertTrue(events.stream().anyMatch(e -> "Event 2".equals(e.getName())));
    }

    @Test
    void createEvent_WithoutReminderSendDate_FiresCreatedEventWithNullReminder() {
        // Event without reminder date should still fire EventCreatedEvent, with a null reminder
        // date -- NotificationService's own observer decides not to schedule anything for that.
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
                            event.id = id(10);
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("Event without Reminder", createdEvent.name());
        assertEquals(null, createdEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));

        ArgumentCaptor<EventCreatedEvent> captor = ArgumentCaptor.forClass(EventCreatedEvent.class);
        verify(eventCreatedBus, times(1)).fire(captor.capture());
        assertEquals(id(10), captor.getValue().eventId());
        assertEquals(null, captor.getValue().reminderSendDate());
    }

    @Test
    void createEvent_WithReminderSendDate_FiresCreatedEventWithReminder() {
        // Event with reminder date should fire EventCreatedEvent carrying that reminder date
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
                            event.id = id(10);
                            return null;
                        })
                .when(eventRepository)
                .persist(any(Event.class));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);

        assertNotNull(createdEvent);
        assertEquals("Event with Reminder", createdEvent.name());
        assertEquals(reminderDate, createdEvent.reminderSendDate());
        verify(eventRepository, times(1)).persist(any(Event.class));

        ArgumentCaptor<EventCreatedEvent> captor = ArgumentCaptor.forClass(EventCreatedEvent.class);
        verify(eventCreatedBus, times(1)).fire(captor.capture());
        assertEquals(id(10), captor.getValue().eventId());
        assertEquals(reminderDate, captor.getValue().reminderSendDate());
    }

    @Test
    void updateEvent_FromNoReminderToReminder_FiresUpdatedEventWithReminder() throws Exception {
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

        ArgumentCaptor<de.felixhertweck.seatreservation.common.events.EventUpdatedEvent> captor =
                ArgumentCaptor.forClass(
                        de.felixhertweck.seatreservation.common.events.EventUpdatedEvent.class);
        verify(eventUpdatedBus, times(1)).fireAsync(captor.capture());
        assertEquals(newReminderDate, captor.getValue().reminderSendDate());
    }

    @Test
    void updateEvent_FromReminderToNoReminder_FiresUpdatedEventWithNullReminder() throws Exception {
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

        ArgumentCaptor<de.felixhertweck.seatreservation.common.events.EventUpdatedEvent> captor =
                ArgumentCaptor.forClass(
                        de.felixhertweck.seatreservation.common.events.EventUpdatedEvent.class);
        verify(eventUpdatedBus, times(1)).fireAsync(captor.capture());
        assertEquals(null, captor.getValue().reminderSendDate());
    }

    @Test
    void updateEvent_FromReminderToNewReminder_FiresUpdatedEventWithNewReminder() throws Exception {
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

        ArgumentCaptor<de.felixhertweck.seatreservation.common.events.EventUpdatedEvent> captor =
                ArgumentCaptor.forClass(
                        de.felixhertweck.seatreservation.common.events.EventUpdatedEvent.class);
        verify(eventUpdatedBus, times(1)).fireAsync(captor.capture());
        assertEquals(newReminderDate, captor.getValue().reminderSendDate());
    }

    @Test
    void createEvent_WithManagers_Success() {
        User extraManager =
                new User(
                        "extra",
                        "extra@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "Ex",
                        "Tra",
                        Set.of(Roles.MANAGER),
                        Set.of());
        extraManager.id = id(5);

        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Multi Manager Event");
        dto.setDescription("Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setEventLocationId(eventLocation.id);
        dto.setManagerIds(Set.of(extraManager.id));

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(userRepository.findByIds(List.of(extraManager.id))).thenReturn(List.of(extraManager));

        EventResponseDTO createdEvent = eventService.createEvent(dto, managerUser);
        assertNotNull(createdEvent);
        assertTrue(createdEvent.managerIds().contains(managerUser.id));
        assertTrue(createdEvent.managerIds().contains(extraManager.id));
    }

    @Test
    void createEvent_WithNonManagerUser_ThrowsException() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Multi Manager Event");
        dto.setDescription("Description");
        dto.setStartTime(Instant.now().plusSeconds(Duration.ofDays(5).toSeconds()));
        dto.setEndTime(Instant.now().plusSeconds(Duration.ofDays(6).toSeconds()));
        dto.setBookingStartTime(Instant.now().plusSeconds(Duration.ofDays(3).toSeconds()));
        dto.setBookingDeadline(Instant.now().plusSeconds(Duration.ofDays(4).toSeconds()));
        dto.setEventLocationId(eventLocation.id);
        dto.setManagerIds(Set.of(regularUser.id));

        when(eventLocationRepository.findByIdOptional(eventLocation.id))
                .thenReturn(Optional.of(eventLocation));
        when(userRepository.findByIds(List.of(regularUser.id))).thenReturn(List.of(regularUser));

        assertThrows(ValidationException.class, () -> eventService.createEvent(dto, managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void addManager_Success() {
        User newManager =
                new User(
                        "newmgr",
                        "newmgr@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "New",
                        "Mgr",
                        Set.of(Roles.MANAGER),
                        Set.of());
        newManager.id = id(6);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIds(List.of(newManager.id))).thenReturn(List.of(newManager));

        EventResponseDTO result =
                eventService.addManager(existingEvent.id, newManager.id, managerUser);
        assertNotNull(result);
        assertTrue(result.managerIds().contains(newManager.id));
    }

    @Test
    void addManager_RejectsUserWithoutManagerRole() {
        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIds(List.of(regularUser.id))).thenReturn(List.of(regularUser));

        assertThrows(
                ValidationException.class,
                () -> eventService.addManager(existingEvent.id, regularUser.id, managerUser));
        verify(eventRepository, never()).persist(any(Event.class));
    }

    @Test
    void removeManager_Success() {
        User coManager =
                new User(
                        "comgr",
                        "comgr@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "Co",
                        "Mgr",
                        Set.of(Roles.MANAGER),
                        Set.of());
        coManager.id = id(7);
        existingEvent.getManagers().add(coManager);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findByIdOptional(coManager.id)).thenReturn(Optional.of(coManager));

        EventResponseDTO result =
                eventService.removeManager(existingEvent.id, coManager.id, managerUser);
        assertNotNull(result);
        assertTrue(!result.managerIds().contains(coManager.id));
    }

    @Test
    void updateEvent_ScheduleChanged_FiresEventRescheduledEvent() throws Exception {
        Instant newStartTime = existingEvent.getStartTime().plusSeconds(3600);
        Instant newEndTime = existingEvent.getEndTime().plusSeconds(3600);

        EventRequestDTO dto = new EventRequestDTO();
        dto.setName(existingEvent.getName());
        dto.setDescription(existingEvent.getDescription());
        dto.setStartTime(newStartTime);
        dto.setEndTime(newEndTime);
        dto.setBookingStartTime(existingEvent.getBookingStartTime());
        dto.setBookingDeadline(existingEvent.getBookingDeadline());
        dto.setReminderSendDate(existingEvent.getReminderSendDate());
        dto.setEventLocationId(existingEvent.getEventLocation().id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(existingEvent.getEventLocation().id))
                .thenReturn(Optional.of(existingEvent.getEventLocation()));

        EventResponseDTO result = eventService.updateEvent(existingEvent.id, dto, managerUser);
        assertNotNull(result);

        ArgumentCaptor<de.felixhertweck.seatreservation.common.events.EventRescheduledEvent>
                captor =
                        ArgumentCaptor.forClass(
                                de.felixhertweck.seatreservation.common.events.EventRescheduledEvent
                                        .class);
        verify(eventRescheduledBus, times(1)).fireAsync(captor.capture());

        de.felixhertweck.seatreservation.common.events.EventRescheduledEvent captured =
                captor.getValue();
        assertEquals(existingEvent.id, captured.eventId());
        assertEquals(newStartTime, captured.newStartTime());
        assertEquals(newEndTime, captured.newEndTime());
    }

    @Test
    void updateEvent_LocationChanged_FiresEventRescheduledEvent() throws Exception {
        EventLocation newLocation = new EventLocation("Neue Halle", "Neue Straße 2", managerUser);
        newLocation.id = id(99);

        EventRequestDTO dto = new EventRequestDTO();
        dto.setName(existingEvent.getName());
        dto.setDescription(existingEvent.getDescription());
        dto.setStartTime(existingEvent.getStartTime());
        dto.setEndTime(existingEvent.getEndTime());
        dto.setBookingStartTime(existingEvent.getBookingStartTime());
        dto.setBookingDeadline(existingEvent.getBookingDeadline());
        dto.setReminderSendDate(existingEvent.getReminderSendDate());
        dto.setEventLocationId(newLocation.id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(newLocation.id))
                .thenReturn(Optional.of(newLocation));

        EventResponseDTO result = eventService.updateEvent(existingEvent.id, dto, managerUser);
        assertNotNull(result);

        ArgumentCaptor<de.felixhertweck.seatreservation.common.events.EventRescheduledEvent>
                captor =
                        ArgumentCaptor.forClass(
                                de.felixhertweck.seatreservation.common.events.EventRescheduledEvent
                                        .class);
        verify(eventRescheduledBus, times(1)).fireAsync(captor.capture());

        de.felixhertweck.seatreservation.common.events.EventRescheduledEvent captured =
                captor.getValue();
        assertEquals("Stadthalle", captured.oldLocationName());
        assertEquals("Neue Halle", captured.newLocationName());
    }

    @Test
    void updateEvent_BookingDeadlineChanged_FiresEventRescheduledEvent() throws Exception {
        Instant newDeadline = existingEvent.getBookingDeadline().minusSeconds(1800);

        EventRequestDTO dto = new EventRequestDTO();
        dto.setName(existingEvent.getName());
        dto.setDescription(existingEvent.getDescription());
        dto.setStartTime(existingEvent.getStartTime());
        dto.setEndTime(existingEvent.getEndTime());
        dto.setBookingStartTime(existingEvent.getBookingStartTime());
        dto.setBookingDeadline(newDeadline);
        dto.setReminderSendDate(existingEvent.getReminderSendDate());
        dto.setEventLocationId(existingEvent.getEventLocation().id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(existingEvent.getEventLocation().id))
                .thenReturn(Optional.of(existingEvent.getEventLocation()));

        EventResponseDTO result = eventService.updateEvent(existingEvent.id, dto, managerUser);
        assertNotNull(result);

        ArgumentCaptor<de.felixhertweck.seatreservation.common.events.EventRescheduledEvent>
                captor =
                        ArgumentCaptor.forClass(
                                de.felixhertweck.seatreservation.common.events.EventRescheduledEvent
                                        .class);
        verify(eventRescheduledBus, times(1)).fireAsync(captor.capture());

        de.felixhertweck.seatreservation.common.events.EventRescheduledEvent captured =
                captor.getValue();
        assertEquals(newDeadline, captured.newBookingDeadline());
    }

    @Test
    void updateEvent_OnlyNameOrDescriptionChanged_DoesNotFireEventRescheduledEvent()
            throws Exception {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setName("Updated Title Only");
        dto.setDescription("Updated Description Only");
        dto.setStartTime(existingEvent.getStartTime());
        dto.setEndTime(existingEvent.getEndTime());
        dto.setBookingStartTime(existingEvent.getBookingStartTime());
        dto.setBookingDeadline(existingEvent.getBookingDeadline());
        dto.setReminderSendDate(existingEvent.getReminderSendDate());
        dto.setEventLocationId(existingEvent.getEventLocation().id);

        when(eventRepository.findByIdOptional(existingEvent.id))
                .thenReturn(Optional.of(existingEvent));
        when(eventLocationRepository.findByIdOptional(existingEvent.getEventLocation().id))
                .thenReturn(Optional.of(existingEvent.getEventLocation()));

        EventResponseDTO result = eventService.updateEvent(existingEvent.id, dto, managerUser);
        assertNotNull(result);

        verify(eventRescheduledBus, never()).fireAsync(any());
    }
}
