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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.management.service.EventService;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
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

    @Mock private EmailService emailService;

    @InjectMocks private NotificationService notificationService;

    private User testUser;
    private Event testEvent;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void sendEventReminders_WithEventsAndReservations_SendsEmails() {
        // Arrange
        List<Event> eventsWithReminders = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation);

        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(eventsWithReminders);
        when(reservationService.findByEvent(testEvent)).thenReturn(reservations);

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService)
                .findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class));
        verify(reservationService).findByEvent(testEvent);
        verify(emailService).sendEventReminder(eq(testUser), eq(testEvent), anyList());
    }

    @Test
    void sendEventReminders_WithNoEvents_DoesNotSendEmails() {
        // Arrange
        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService)
                .findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class));
        verify(reservationService, never()).findByEvent(any());
        verify(emailService, never()).sendEventReminder(any(), any(), any());
    }

    @Test
    void sendEventReminders_WithEventsButNoReservations_DoesNotSendEmails() {
        // Arrange
        List<Event> eventsWithReminders = List.of(testEvent);

        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(eventsWithReminders);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of());

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService)
                .findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class));
        verify(reservationService).findByEvent(testEvent);
        verify(emailService, never()).sendEventReminder(any(), any(), any());
    }

    @Test
    void sendEventReminders_WithMultipleEvents_ProcessesAllEvents() {
        // Arrange
        Event secondEvent = new Event();
        secondEvent.setName("Second Event");

        User secondUser = new User();
        secondUser.setEmail("second@example.com");

        Reservation secondReservation = new Reservation();
        secondReservation.setUser(secondUser);
        secondReservation.setEvent(secondEvent);

        List<Event> eventsWithReminders = List.of(testEvent, secondEvent);

        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(eventsWithReminders);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of(testReservation));
        when(reservationService.findByEvent(secondEvent)).thenReturn(List.of(secondReservation));

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService)
                .findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class));
        verify(reservationService).findByEvent(testEvent);
        verify(reservationService).findByEvent(secondEvent);
        verify(emailService).sendEventReminder(eq(testUser), eq(testEvent), anyList());
        verify(emailService).sendEventReminder(eq(secondUser), eq(secondEvent), anyList());
    }

    @Test
    void sendEventReminders_WithMultipleReservationsPerUser_GroupsCorrectly() {
        // Arrange
        Reservation secondReservation = new Reservation();
        secondReservation.setUser(testUser); // Same user, different reservation
        secondReservation.setEvent(testEvent);

        List<Event> eventsWithReminders = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation, secondReservation);

        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(eventsWithReminders);
        when(reservationService.findByEvent(testEvent)).thenReturn(reservations);

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(emailService)
                .sendEventReminder(
                        eq(testUser),
                        eq(testEvent),
                        argThat(
                                (List<Reservation> reservationList) ->
                                        reservationList.size() == 2));
    }

    @Test
    void sendEventReminders_WithEmailException_ContinuesProcessing() {
        // Arrange
        User secondUser = new User();
        secondUser.setEmail("second@example.com");

        Reservation secondReservation = new Reservation();
        secondReservation.setUser(secondUser);
        secondReservation.setEvent(testEvent);

        List<Event> eventsWithReminders = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation, secondReservation);

        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(eventsWithReminders);
        when(reservationService.findByEvent(testEvent)).thenReturn(reservations);

        // First email fails, second should still be attempted
        doThrow(new RuntimeException("Email sending failed"))
                .when(emailService)
                .sendEventReminder(eq(testUser), eq(testEvent), anyList());
        doNothing().when(emailService).sendEventReminder(eq(secondUser), eq(testEvent), anyList());

        // Act
        assertDoesNotThrow(() -> notificationService.sendEventReminders());

        // Assert
        verify(emailService).sendEventReminder(eq(testUser), eq(testEvent), anyList());
        verify(emailService).sendEventReminder(eq(secondUser), eq(testEvent), anyList());
    }

    @Test
    void sendEventReminders_WithNullUserEmail_SkipsUserGracefully() {
        // Arrange
        testUser.setEmail(null);

        List<Event> eventsWithReminders = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation);

        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(eventsWithReminders);
        when(reservationService.findByEvent(testEvent)).thenReturn(reservations);

        // Act
        assertDoesNotThrow(() -> notificationService.sendEventReminders());

        // Assert
        verify(emailService).sendEventReminder(eq(testUser), eq(testEvent), anyList());
    }

    @Test
    void sendEventReminders_CalculatesCorrectDateRange() {
        // Arrange
        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService)
                .findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    void sendEventReminders_WithServiceException_HandlesGracefully() {
        // Arrange
        when(eventService.findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationService.sendEventReminders());

        verify(eventService)
                .findEventsWithReminderDateBetween(any(Instant.class), any(Instant.class));
        verify(reservationService, never()).findByEvent(any());
        verify(emailService, never()).sendEventReminder(any(), any(), any());
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
}
