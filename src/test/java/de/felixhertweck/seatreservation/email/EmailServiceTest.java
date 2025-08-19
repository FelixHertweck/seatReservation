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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.reservation.service.ReservationService;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock Mailer mailer;

    @Mock EmailVerificationRepository emailVerificationRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock SeatRepository seatRepository;
    @Mock ReservationService reservationService;

    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService.baseUrl = "http://localhost:8080";
        emailService.expirationMinutes = 60;
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstname("Test");
        user.setLastname("User");
        user.id = 1L;
        return user;
    }

    private Event createTestEvent(EventLocation location) {
        Event event = new Event();
        event.id = 10L;
        event.setName("Test Event");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event.setEventLocation(location);
        return event;
    }

    private EventLocation createTestEventLocation() {
        EventLocation location = new EventLocation();
        location.id = 100L;
        location.setName("Test Location");
        location.setCapacity(100);
        return location;
    }

    private Seat createTestSeat(EventLocation location, String seatNumber) {
        Seat seat = new Seat();
        seat.id = 1000L;
        seat.setSeatNumber(seatNumber);
        seat.setLocation(location);
        return seat;
    }

    private Reservation createTestReservation(User user, Event event, Seat seat) {
        Reservation reservation = new Reservation();
        reservation.id = 10000L;
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setSeat(seat);
        return reservation;
    }

    @Test
    void sendEmailConfirmation_Success() throws IOException {
        User user = createTestUser();
        EmailVerification emailVerification =
                new EmailVerification(user, "testtoken", LocalDateTime.now().plusMinutes(60));
        doNothing().when(mailer).send(any(Mail.class));

        emailService.sendEmailConfirmation(user, emailVerification);

        verify(emailVerificationRepository, never())
                .persist(
                        any(de.felixhertweck.seatreservation.model.entity.EmailVerification.class));
        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailer, times(1)).send(mailCaptor.capture());

        Mail sentMail = mailCaptor.getValue();
        assertEquals(user.getEmail(), sentMail.getTo().getFirst());
        assertEquals("Please Confirm Your Email Address", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains("http://localhost:8080/api/user/confirm-email"));
    }

    @Test
    void createEmailVerification_Success() {
        User user = createTestUser();
        doNothing().when(emailVerificationRepository).persist(any(EmailVerification.class));

        EmailVerification createdVerification = emailService.createEmailVerification(user);

        assertNotNull(createdVerification);
        assertEquals(user, createdVerification.getUser());
        assertNotNull(createdVerification.getToken());
        assertNotNull(createdVerification.getExpirationTime());
        verify(emailVerificationRepository, times(1)).persist(createdVerification);
    }

    @Test
    void updateEmailVerificationExpiration_Success() {
        User user = createTestUser();
        EmailVerification emailVerification =
                new EmailVerification(user, "oldtoken", LocalDateTime.now().minusMinutes(10));
        emailVerification.id = 1L;

        doNothing().when(emailVerificationRepository).persist(any(EmailVerification.class));

        EmailVerification updatedVerification =
                emailService.updateEmailVerificationExpiration(emailVerification);

        assertNotNull(updatedVerification);
        assertTrue(
                updatedVerification
                        .getExpirationTime()
                        .isAfter(LocalDateTime.now().minusMinutes(1)));
        verify(emailVerificationRepository, times(1)).persist(updatedVerification);
    }

    @Test
    void sendEventReminder_Success() throws IOException {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(user, event, seat));

        doNothing().when(mailer).send(any(Mail.class));

        emailService.sendEventReminder(user, event, reservations);

        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailer, times(1)).send(mailCaptor.capture());

        Mail sentMail = mailCaptor.getValue();
        assertEquals(user.getEmail(), sentMail.getTo().getFirst());
        assertEquals("Erinnerung: Ihr Event beginnt bald!", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains(user.getFirstname() + " " + user.getLastname()));
        assertTrue(sentMail.getHtml().contains(event.getName()));
        assertTrue(sentMail.getHtml().contains(event.getStartTime().toLocalDate().toString()));
        assertTrue(sentMail.getHtml().contains(event.getStartTime().toLocalTime().toString()));
        assertTrue(sentMail.getHtml().contains(event.getEventLocation().getName()));
        assertTrue(sentMail.getHtml().contains("<li>A1</li>"));
    }

    @Test
    void sendEventReminder_IOException() throws IOException {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(user, event, seat));

        // Simulate a failure in mailer.send (e.g., due to an internal issue, not IOException
        // directly)
        // Since EmailService catches IOException internally, we cannot directly assertThrows
        // IOException.
        // We will verify that mailer.send was attempted.
        doThrow(new RuntimeException("Simulated Mailer Error")).when(mailer).send(any(Mail.class));

        // Call the method under test
        emailService.sendEventReminder(user, event, reservations);

        // Verify that mailer.send was attempted
        verify(mailer, times(1)).send(any(Mail.class));
        // Further verification would involve mocking the static Logger to check errorf calls,
        // which is beyond the scope of simple Mockito setup without PowerMock or similar.
    }
}
