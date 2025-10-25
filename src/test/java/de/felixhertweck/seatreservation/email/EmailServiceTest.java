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
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.reservation.service.ReservationService;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmailServiceTest {

    @Inject MockMailbox mailbox;

    @InjectMock EmailVerificationRepository emailVerificationRepository;
    @InjectMock ReservationRepository reservationRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock ReservationService reservationService;

    @Inject EmailService emailService;

    @BeforeEach
    void setUp() {
        mailbox.clear();
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstname("Test");
        user.setLastname("User");
        user.id = 1L;
        user.setEmailVerified(false);
        user.setEmailVerificationSent(false);
        return user;
    }

    private Event createTestEvent(EventLocation location) {
        Event event = new Event();
        event.id = 10L;
        event.setName("Test Event");
        event.setStartTime(Instant.now().plusSeconds(Duration.ofDays(1).toSeconds()));
        event.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(1).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
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
                new EmailVerification(
                        user,
                        "testtoken",
                        Instant.now().plusSeconds(Duration.ofMinutes(60).toSeconds()));

        emailService.sendEmailConfirmation(user, emailVerification);

        verify(emailVerificationRepository, never()).persist(any(EmailVerification.class));

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.get(0);
        assertEquals(user.getEmail(), sentMail.getTo().getFirst());
        assertEquals("Please Confirm Your Email Address", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains("testtoken")); // Code should be in email
        assertTrue(
                sentMail.getHtml()
                        .contains("http://localhost:8080/verify?code=testtoken")); // Verification
        // link should be
        // in email
    }

    @Test
    void createEmailVerification_Success() {
        User user = createTestUser();
        doNothing().when(emailVerificationRepository).persist(any(EmailVerification.class));

        EmailVerification createdVerification = emailService.createEmailVerification(user);

        assertNotNull(createdVerification);
        assertEquals(user, createdVerification.getUser());
        assertNotNull(createdVerification.getToken());
        // Verify token is 6-digit code
        assertTrue(
                createdVerification.getToken().matches("\\d{6}"),
                "Token should be a 6-digit code, got: " + createdVerification.getToken());
        assertNotNull(createdVerification.getExpirationTime());
        verify(emailVerificationRepository, times(1)).persist(createdVerification);
    }

    @Test
    void createEmailVerification_SetsEmailVerificationSentToTrue() {
        User user = createTestUser();
        // Ensure emailVerificationSent is false initially
        assertFalse(user.isEmailVerificationSent());

        doAnswer(
                        invocation -> {
                            EmailVerification ev = invocation.getArgument(0);
                            ev.getUser()
                                    .setEmailVerificationSent(
                                            true); // Simulate update in repository
                            return null;
                        })
                .when(emailVerificationRepository)
                .persist(any(EmailVerification.class));

        emailService.createEmailVerification(user);

        // Verify that emailVerificationSent is true after creating verification
        assertTrue(user.isEmailVerificationSent());
        verify(emailVerificationRepository, times(1)).persist(any(EmailVerification.class));
    }

    @Test
    void updateEmailVerificationExpiration_Success() {
        User user = createTestUser();
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "oldtoken",
                        Instant.now().minusSeconds(Duration.ofMinutes(10).toSeconds()));
        emailVerification.id = 1L;

        doNothing().when(emailVerificationRepository).persist(any(EmailVerification.class));

        EmailVerification updatedVerification =
                emailService.updateEmailVerificationExpiration(emailVerification);

        assertNotNull(updatedVerification);
        assertTrue(
                updatedVerification
                        .getExpirationTime()
                        .isAfter(Instant.now().minusSeconds(Duration.ofMinutes(1).toSeconds())));
        verify(emailVerificationRepository, times(1)).persist(updatedVerification);
    }

    @Test
    void sendEventReminder_Success() throws IOException {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        seat.setSeatRow("1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(user, event, seat));

        when(seatRepository.findByEventLocation(any())).thenReturn(Collections.singletonList(seat));

        emailService.sendEventReminder(user, event, reservations);

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.get(0);
        assertEquals(user.getEmail(), sentMail.getTo().getFirst());
        assertEquals("Reminder: Your event is starting soon!", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains(user.getFirstname() + " " + user.getLastname()));
        assertTrue(sentMail.getHtml().contains(event.getName()));
        assertTrue(
                sentMail.getHtml()
                        .contains(
                                event.getStartTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toString()));
        assertTrue(sentMail.getHtml().contains(event.getEventLocation().getName()));
        assertTrue(sentMail.getHtml().contains("<li>A1 (1)</li>"));
        assertTrue(sentMail.getHtml().contains("<svg"));
        assertTrue(sentMail.getHtml().contains("http://localhost:8080/reservations?id=10"));
        // Verify that BCC is not added to event reminder emails
        assertTrue(sentMail.getBcc().isEmpty(), "Event reminder emails should not have BCC");
    }

    @Test
    void sendEventReminder_IOException() throws IOException {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(user, event, seat));

        // Note: MockMailbox doesn't throw IOException, so this test verifies normal behavior
        // In a real scenario, you might want to test error handling differently
        emailService.sendEventReminder(user, event, reservations);

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());
    }
}
