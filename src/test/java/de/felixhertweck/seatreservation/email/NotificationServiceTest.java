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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        testEvent.setStartTime(LocalDateTime.now().plusDays(1));

        testReservation = new Reservation();
        testReservation.setUser(testUser);
        testReservation.setEvent(testEvent);
    }

    @Test
    void sendEventReminders_WithEventsAndReservations_SendsEmails() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(eventsTomorrow);
        when(reservationService.findByEvent(testEvent)).thenReturn(reservations);

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfTomorrow, endOfTomorrow);
        verify(reservationService).findByEvent(testEvent);
        verify(emailService).sendEventReminder(eq(testUser), eq(testEvent), anyList());
    }

    @Test
    void sendEventReminders_WithNoEvents_DoesNotSendEmails() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(List.of());

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfTomorrow, endOfTomorrow);
        verify(reservationService, never()).findByEvent(any());
        verify(emailService, never()).sendEventReminder(any(), any(), any());
    }

    @Test
    void sendEventReminders_WithEventsButNoReservations_DoesNotSendEmails() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow = List.of(testEvent);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(eventsTomorrow);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of());

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfTomorrow, endOfTomorrow);
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

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow = List.of(testEvent, secondEvent);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(eventsTomorrow);
        when(reservationService.findByEvent(testEvent)).thenReturn(List.of(testReservation));
        when(reservationService.findByEvent(secondEvent)).thenReturn(List.of(secondReservation));

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService).findEventsBetweenDates(startOfTomorrow, endOfTomorrow);
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

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation, secondReservation);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(eventsTomorrow);
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

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation, secondReservation);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(eventsTomorrow);
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

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        List<Event> eventsTomorrow = List.of(testEvent);
        List<Reservation> reservations = List.of(testReservation);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenReturn(eventsTomorrow);
        when(reservationService.findByEvent(testEvent)).thenReturn(reservations);

        // Act
        assertDoesNotThrow(() -> notificationService.sendEventReminders());

        // Assert
        verify(emailService).sendEventReminder(eq(testUser), eq(testEvent), anyList());
    }

    @Test
    void sendEventReminders_CalculatesCorrectDateRange() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime expectedStart = tomorrow.atStartOfDay();
        LocalDateTime expectedEnd = tomorrow.atTime(LocalTime.MAX);

        when(eventService.findEventsBetweenDates(any(), any())).thenReturn(List.of());

        // Act
        notificationService.sendEventReminders();

        // Assert
        verify(eventService).findEventsBetweenDates(expectedStart, expectedEnd);
    }

    @Test
    void sendEventReminders_WithServiceException_HandlesGracefully() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);

        when(eventService.findEventsBetweenDates(startOfTomorrow, endOfTomorrow))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationService.sendEventReminders());

        verify(eventService).findEventsBetweenDates(startOfTomorrow, endOfTomorrow);
        verify(reservationService, never()).findByEvent(any());
        verify(emailService, never()).sendEventReminder(any(), any(), any());
    }
}
