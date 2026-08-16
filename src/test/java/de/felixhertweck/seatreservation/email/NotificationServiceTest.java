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
package de.felixhertweck.seatreservation.email;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.events.EventRescheduledEvent;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.email.service.NotificationService;
import de.felixhertweck.seatreservation.management.service.EventService;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.Coordinate;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private EventService eventService;

    @Mock private ReservationService reservationService;

    @Mock private ReservationRepository reservationRepository;

    @Mock private EmailService emailService;

    @InjectMocks private NotificationService notificationService;

    private User testUser;
    private Event testEvent;
    private Reservation testReservation;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(Roles.USER));

        testEvent = new Event();
        testEvent.setName("Test Event");
        testEvent.setStartTime(Instant.now().plusSeconds(Duration.ofDays(1).toSeconds()));

        testReservation = new Reservation();
        testReservation.setUser(testUser);
        testReservation.setEvent(testEvent);

        // sendDailyReservationCsvToManagers() calls its transactional/request-scoped steps via the
        // CDI self-proxy so the @Transactional/@ActivateRequestContext interceptors apply; outside
        // of CDI, wire it back to the same instance so those steps still run.
        Field selfField = NotificationService.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(notificationService, notificationService);
    }

    // Note: The old periodic scheduling tests have been removed as the implementation
    // now uses programmatic scheduling where reminders are scheduled when events are
    // created/updated.
    // These tests would have tested the old sendEventReminders() method which no longer exists.

    // loadReminderData() force-loads event.getEventLocation()'s seats/markers/areas (and each
    // area's boundary) before returning, since sendReminderEmails() later reads them outside the
    // @Transactional boundary (see eagerLoadEntities()). Because eventService/reservationService
    // are Mockito mocks here, event/location/reservation are plain POJOs, never real Hibernate
    // proxies - so these tests cannot reproduce a genuine LazyInitializationException. What they do
    // verify: the eager-load traversal is null-safe and structurally correct for a location that
    // actually has seats, markers, areas and boundary points, and for the null-location and
    // empty-collection edge cases. Verifying against a real lazy proxy needs a @QuarkusTest with a
    // genuine persistence context.

    @Test
    void loadReminderData_WithFullEventLocationGraph_DoesNotThrow() {
        // Arrange
        EventLocation location = new EventLocation("Main Hall", "Test Street 1", null);
        Seat seat = new Seat("A1", location, "A", 1, 1, null, null);
        location.setSeats(List.of(seat));
        location.setMarkers(List.of(new EventLocationMarker("Entrance", 0, 0)));

        EventLocationArea area = new EventLocationArea("VIP");
        area.setBoundary(List.of(new Coordinate(0, 0), new Coordinate(5, 0), new Coordinate(5, 5)));
        location.setAreas(List.of(area));

        testEvent.setEventLocation(location);
        testReservation.setSeat(seat);

        when(eventService.findById(id(1))).thenReturn(testEvent);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of(testReservation));

        // Act & Assert
        NotificationService.ReminderData data =
                assertDoesNotThrow(() -> notificationService.loadReminderData(id(1)));
        assertNotNull(data);
        verify(eventService).findById(id(1));
        verify(reservationService).findByEvent(testEvent);
    }

    @Test
    void loadReminderData_WithNullEventLocation_DoesNotThrow() {
        // Arrange: testEvent has no location set (default from setUp()).
        when(eventService.findById(id(2))).thenReturn(testEvent);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of(testReservation));

        // Act & Assert
        NotificationService.ReminderData data =
                assertDoesNotThrow(() -> notificationService.loadReminderData(id(2)));
        assertNotNull(data);
    }

    @Test
    void loadReminderData_WithEmptyLocationCollections_DoesNotThrow() {
        // Arrange: a location with no seats/markers/areas (all default to empty lists).
        EventLocation location = new EventLocation("Empty Hall", "Test Street 2", null);
        testEvent.setEventLocation(location);

        when(eventService.findById(id(3))).thenReturn(testEvent);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of(testReservation));

        // Act & Assert
        NotificationService.ReminderData data =
                assertDoesNotThrow(() -> notificationService.loadReminderData(id(3)));
        assertNotNull(data);
    }

    @Test
    void sendDailyReservationCsvToManagers_WithEventsAndManagers_SendsCsvEmails() throws Exception {
        // Arrange
        User manager = new User();
        manager.setEmail("manager@example.com");
        manager.setFirstname("Manager");
        manager.setLastname("Test");

        testEvent.setManager(manager);

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        List<Event> eventsToday = List.of(testEvent);

        when(eventService.findEventsBetweenDates(startOfToday, endOfToday)).thenReturn(eventsToday);

        // Act
        notificationService.sendDailyReservationCsvToManagers();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfToday, endOfToday);
        verify(emailService).sendEventReservationsCsvToManager(manager, testEvent);
    }

    @Test
    void sendDailyReservationCsvToManagers_WithNoEvents_DoesNotSendEmails() throws Exception {
        // Arrange
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        when(eventService.findEventsBetweenDates(startOfToday, endOfToday)).thenReturn(List.of());

        // Act
        notificationService.sendDailyReservationCsvToManagers();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfToday, endOfToday);
        verify(emailService, never()).sendEventReservationsCsvToManager(any(), any());
    }

    @Test
    void sendDailyReservationCsvToManagers_WithEventButNoManager_DoesNotSendEmail()
            throws Exception {
        // Arrange
        testEvent.setManager(null); // No manager assigned

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        List<Event> eventsToday = List.of(testEvent);

        when(eventService.findEventsBetweenDates(startOfToday, endOfToday)).thenReturn(eventsToday);

        // Act
        notificationService.sendDailyReservationCsvToManagers();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfToday, endOfToday);
        verify(emailService, never()).sendEventReservationsCsvToManager(any(), any());
    }

    @Test
    void sendDailyReservationCsvToManagers_WithMultipleEvents_ProcessesAllEvents()
            throws Exception {
        // Arrange
        User manager1 = new User();
        manager1.setEmail("manager1@example.com");

        User manager2 = new User();
        manager2.setEmail("manager2@example.com");

        Event secondEvent = new Event();
        secondEvent.setName("Second Event");
        secondEvent.setManager(manager2);

        testEvent.setManager(manager1);

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        List<Event> eventsToday = List.of(testEvent, secondEvent);

        when(eventService.findEventsBetweenDates(startOfToday, endOfToday)).thenReturn(eventsToday);

        // Act
        notificationService.sendDailyReservationCsvToManagers();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfToday, endOfToday);
        verify(emailService).sendEventReservationsCsvToManager(manager1, testEvent);
        verify(emailService).sendEventReservationsCsvToManager(manager2, secondEvent);
    }

    @Test
    void sendDailyReservationCsvToManagers_WithEmailException_ContinuesProcessing()
            throws Exception {
        // Arrange
        User manager1 = new User();
        manager1.setEmail("manager1@example.com");

        User manager2 = new User();
        manager2.setEmail("manager2@example.com");

        Event secondEvent = new Event();
        secondEvent.setName("Second Event");
        secondEvent.setManager(manager2);

        testEvent.setManager(manager1);

        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        List<Event> eventsToday = List.of(testEvent, secondEvent);

        when(eventService.findEventsBetweenDates(startOfToday, endOfToday)).thenReturn(eventsToday);

        // First email fails, second should still be attempted
        doThrow(new IOException("CSV generation failed"))
                .when(emailService)
                .sendEventReservationsCsvToManager(manager1, testEvent);
        doNothing().when(emailService).sendEventReservationsCsvToManager(manager2, secondEvent);

        // Act
        assertDoesNotThrow(() -> notificationService.sendDailyReservationCsvToManagers());

        // Assert
        verify(emailService).sendEventReservationsCsvToManager(manager1, testEvent);
        verify(emailService).sendEventReservationsCsvToManager(manager2, secondEvent);
    }

    @Test
    void sendDailyReservationCsvToManagers_CalculatesCorrectDateRange() throws Exception {
        // Arrange
        Instant expectedStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant expectedEnd =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        when(eventService.findEventsBetweenDates(any(), any())).thenReturn(List.of());

        // Act
        notificationService.sendDailyReservationCsvToManagers();

        // Assert
        verify(eventService).findEventsBetweenDates(expectedStart, expectedEnd);
    }

    @Test
    void sendDailyReservationCsvToManagers_WithServiceException_HandlesGracefully()
            throws Exception {
        // Arrange
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfToday =
                LocalDate.now()
                        .atTime(23, 59, 59, 999_999_999)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

        when(eventService.findEventsBetweenDates(startOfToday, endOfToday))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> notificationService.sendDailyReservationCsvToManagers());

        verify(eventService).findEventsBetweenDates(startOfToday, endOfToday);
        verify(emailService, never()).sendEventReservationsCsvToManager(any(), any());
    }

    @Test
    void onEventRescheduled_NoReservations_DoesNotSendEmail() {
        UUID eventId = id(10);
        testEvent.id = eventId;

        EventRescheduledEvent rescheduledEvent =
                new EventRescheduledEvent(
                        eventId,
                        "Test Event",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Instant.now().plusSeconds(7200),
                        Instant.now().plusSeconds(10800),
                        "Old Location",
                        "New Location",
                        Instant.now().minusSeconds(3600),
                        Instant.now().minusSeconds(1800));

        when(eventService.findById(eventId)).thenReturn(testEvent);
        when(reservationRepository.findByEventIdWithUserAndSeat(eventId)).thenReturn(List.of());

        assertDoesNotThrow(() -> notificationService.onEventRescheduled(rescheduledEvent));

        verify(emailService, never())
                .sendEventRescheduledNotification(
                        any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onEventRescheduled_WithReservations_SendsEmailPerUser() {
        UUID eventId = id(10);
        testEvent.id = eventId;

        User user2 = new User();
        user2.id = id(2);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        testUser.id = id(1);

        Reservation res1 = new Reservation();
        res1.setUser(testUser);
        res1.setEvent(testEvent);

        Reservation res2 = new Reservation();
        res2.setUser(testUser);
        res2.setEvent(testEvent);

        Reservation res3 = new Reservation();
        res3.setUser(user2);
        res3.setEvent(testEvent);

        EventRescheduledEvent rescheduledEvent =
                new EventRescheduledEvent(
                        eventId,
                        "Test Event",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Instant.now().plusSeconds(7200),
                        Instant.now().plusSeconds(10800),
                        "Old Location",
                        "New Location",
                        Instant.now().minusSeconds(3600),
                        Instant.now().minusSeconds(1800));

        when(eventService.findById(eventId)).thenReturn(testEvent);
        when(reservationRepository.findByEventIdWithUserAndSeat(eventId))
                .thenReturn(List.of(res1, res2, res3));

        assertDoesNotThrow(() -> notificationService.onEventRescheduled(rescheduledEvent));

        verify(emailService)
                .sendEventRescheduledNotification(
                        org.mockito.ArgumentMatchers.eq(testUser),
                        org.mockito.ArgumentMatchers.eq(testEvent),
                        org.mockito.ArgumentMatchers.argThat(list -> list.size() == 2),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldStartTime()),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldEndTime()),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldLocationName()),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldBookingDeadline()),
                        org.mockito.ArgumentMatchers.isNull());

        verify(emailService)
                .sendEventRescheduledNotification(
                        org.mockito.ArgumentMatchers.eq(user2),
                        org.mockito.ArgumentMatchers.eq(testEvent),
                        org.mockito.ArgumentMatchers.argThat(list -> list.size() == 1),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldStartTime()),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldEndTime()),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldLocationName()),
                        org.mockito.ArgumentMatchers.eq(rescheduledEvent.oldBookingDeadline()),
                        org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void onEventRescheduled_EventNotFound_DoesNotSendEmail() {
        UUID eventId = id(10);
        EventRescheduledEvent rescheduledEvent =
                new EventRescheduledEvent(
                        eventId,
                        "Test Event",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Instant.now().plusSeconds(7200),
                        Instant.now().plusSeconds(10800),
                        "Old Location",
                        "New Location",
                        Instant.now().minusSeconds(3600),
                        Instant.now().minusSeconds(1800));

        when(eventService.findById(eventId)).thenReturn(null);

        assertDoesNotThrow(() -> notificationService.onEventRescheduled(rescheduledEvent));

        verify(reservationRepository, never()).findByEventIdWithUserAndSeat(any());
        verify(emailService, never())
                .sendEventRescheduledNotification(
                        any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onEventCancelled_WithReservations_SendsCancellationEmailPerUser() {
        UUID eventId = id(10);
        User user2 = new User();
        user2.id = id(2);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        testUser.id = id(1);

        Reservation res1 = new Reservation();
        res1.setUser(testUser);
        Reservation res2 = new Reservation();
        res2.setUser(user2);

        de.felixhertweck.seatreservation.common.events.EventCancelledEvent cancelledEvent =
                new de.felixhertweck.seatreservation.common.events.EventCancelledEvent(
                        eventId,
                        "Test Event",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        "Test Location",
                        "Cancelled due to storm",
                        List.of(res1, res2));

        notificationService.onEventCancelled(cancelledEvent);

        verify(emailService)
                .sendEventCancelledNotification(
                        org.mockito.ArgumentMatchers.eq(testUser),
                        org.mockito.ArgumentMatchers.eq(cancelledEvent));
        verify(emailService)
                .sendEventCancelledNotification(
                        org.mockito.ArgumentMatchers.eq(user2),
                        org.mockito.ArgumentMatchers.eq(cancelledEvent));
    }

    @Test
    void onEventCancelled_NoReservations_DoesNotSendEmail() {
        UUID eventId = id(10);
        de.felixhertweck.seatreservation.common.events.EventCancelledEvent cancelledEvent =
                new de.felixhertweck.seatreservation.common.events.EventCancelledEvent(
                        eventId,
                        "Test Event",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        "Test Location",
                        "Cancelled due to storm",
                        List.of());

        notificationService.onEventCancelled(cancelledEvent);

        verify(emailService, never()).sendEventCancelledNotification(any(), any());
    }
}
