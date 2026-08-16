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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.zxing.WriterException;
import de.felixhertweck.seatreservation.email.queue.EmailDispatcher;
import de.felixhertweck.seatreservation.email.service.EmailSeatMapService;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.email.service.ReservationEmailContent;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.QRCodeImage;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@QuarkusTest
class EmailServiceTest {

    @Inject MockMailbox mailbox;

    @InjectMock EmailVerificationRepository emailVerificationRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock EmailSeatMapService emailSeatMapService;
    @InjectMock ReservationRepository reservationRepository;
    @InjectMock ReservationService reservationService;

    @Inject EmailService emailService;

    @Inject EmailDispatcher emailDispatcher;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mailbox.clear();
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstname("Test");
        user.setLastname("User");
        user.setUsername("testuser");
        user.id = id(1);
        user.setEmailVerified(false);
        user.setEmailVerificationSent(false);
        return user;
    }

    private Event createTestEvent(EventLocation location) {
        Event event = new Event();
        event.id = id(10);
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
        location.id = id(100);
        location.setName("Test Location");
        return location;
    }

    private Seat createTestSeat(EventLocation location, String seatNumber) {
        Seat seat = new Seat(seatNumber, "", location);
        seat.id = id(1000);
        seat.setArea(new EventLocationArea("Parkett"));
        return seat;
    }

    private Reservation createTestReservation(User user, Event event, Seat seat) {
        Reservation reservation = new Reservation();
        reservation.id = id(10000);
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setSeat(seat);
        reservation.setCheckInToken(new CheckInToken(user, event, "CODE123"));
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
        emailDispatcher.drainQueue();

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
    void sendPasswordResetEmail_Success() throws IOException {
        User user = createTestUser();
        PasswordResetToken passwordResetToken =
                new PasswordResetToken(
                        user,
                        "testresettoken",
                        Instant.now().plusSeconds(Duration.ofMinutes(60).toSeconds()));

        emailService.sendPasswordResetEmail(user, passwordResetToken);
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.get(0);
        assertEquals(user.getEmail(), sentMail.getTo().getFirst());
        assertEquals("Password Reset Request", sentMail.getSubject());
        assertTrue(
                sentMail.getHtml()
                        .contains("http://localhost:8080/reset-password?token=testresettoken"));
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
        emailVerification.id = id(1);

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

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-token-123");
        when(emailSeatMapService.getPngImage(anyString())).thenReturn(Optional.of(new byte[0]));

        emailService.sendEventReminder(user, event, reservations);
        emailDispatcher.drainQueue();

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
        assertTrue(sentMail.getHtml().contains("<li>A1 (1) - Parkett</li>"));
        assertTrue(
                sentMail.getHtml()
                        .contains("http://localhost:8080/email/seatmap?token=test-token-123"));
        assertTrue(
                sentMail.getHtml()
                        .contains("http://localhost:8080/events/reservations?eventId=" + event.id));
        assertTrue(sentMail.getHtml().contains("<img src=\"cid:qrcode-image\""));
        assertTrue(sentMail.getHtml().contains(user.getUsername()));
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

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-token-123");
        when(emailSeatMapService.getPngImage(anyString())).thenReturn(Optional.of(new byte[0]));

        // Note: MockMailbox doesn't throw IOException, so this test verifies normal behavior
        // In a real scenario, you might want to test error handling differently
        emailService.sendEventReminder(user, event, reservations);
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());
    }

    @Test
    void sendBoxOfficeConfirmation_WithQrCode_SendsEmailAndReturnsDisplayHtmlWithDataUri() {
        User boxofficeUser = new User();
        boxofficeUser.id = id(5);
        boxofficeUser.setUsername("boxoffice");
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        seat.setSeatRow("1");
        Reservation reservation = createTestReservation(boxofficeUser, event, seat);
        reservation.setCheckInToken(new CheckInToken(boxofficeUser, event, "CODE123"));
        List<Reservation> reservations = Collections.singletonList(reservation);

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));

        ReservationEmailContent.BoxOfficeConfirmationContent content =
                emailService.sendBoxOfficeConfirmation(
                        boxofficeUser, reservations, "Jane Doe", "guest@example.com", true);
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo("guest@example.com");
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.getFirst();
        assertEquals("Box Office Reservation Confirmation", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains("Jane Doe"));
        assertTrue(sentMail.getHtml().contains("<li>A1 (1) - Parkett</li>"));
        assertTrue(sentMail.getHtml().contains("<img src=\"cid:qrcode-image\""));
        assertEquals(1, sentMail.getAttachments().size());

        // The API response (used for the frontend print copy) has no MIME attachment channel to
        // resolve a cid: reference against, so it must embed the QR as a data: URI instead.
        assertNotNull(content);
        assertTrue(content.displayHtml().contains("data:image/png;base64,"));
        assertFalse(content.displayHtml().contains("cid:qrcode-image"));
        assertTrue(content.emailHtml().contains("cid:qrcode-image"));
        assertTrue(content.qrCodeImage().length > 0);
    }

    @Test
    void sendBoxOfficeConfirmation_CheckedInAtCreation_OmitsQrCode() {
        User targetUser = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(targetUser, event, seat));

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));

        ReservationEmailContent.BoxOfficeConfirmationContent content =
                emailService.sendBoxOfficeConfirmation(
                        targetUser, reservations, "Test User", null, false);
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(targetUser.getEmail());
        assertEquals(1, sentMails.size());
        assertTrue(
                sentMails.getFirst().getAttachments().isEmpty(),
                "No QR attachment should be queued when the reservation was already checked in");
        assertFalse(sentMails.getFirst().getHtml().contains("<img"));

        assertEquals(0, content.qrCodeImage().length);
        assertFalse(content.displayHtml().contains("data:image/png;base64,"));
    }

    @Test
    void sendBoxOfficeConfirmation_NoValidRecipient_SendsNoEmailButReturnsRenderedContent() {
        User boxofficeUser = new User();
        boxofficeUser.id = id(5);
        boxofficeUser.setUsername("boxoffice");
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(boxofficeUser, event, seat));

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));

        // boxofficeUser has no email and no additionalMailAddress is given (guest didn't type
        // one) -- no valid recipient exists, so the confirmation must still render (for print)
        // without queuing an email.
        ReservationEmailContent.BoxOfficeConfirmationContent content =
                emailService.sendBoxOfficeConfirmation(
                        boxofficeUser, reservations, "Jane Doe", null, true);
        emailDispatcher.drainQueue();

        assertEquals(0, mailbox.getTotalMessagesSent());
        assertFalse(content.displayHtml().isEmpty());
        assertTrue(content.displayHtml().contains("Jane Doe"));
    }

    @Test
    void sendEventReminder_QrCodeGenerationThrowsException() throws Exception {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations =
                Collections.singletonList(createTestReservation(user, event, seat));

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-token-123");
        when(emailSeatMapService.getPngImage(anyString())).thenReturn(Optional.of(new byte[0]));

        try (MockedStatic<QRCodeImage> qrCodeImageMock = Mockito.mockStatic(QRCodeImage.class)) {
            qrCodeImageMock
                    .when(() -> QRCodeImage.generateQrCodeImage(anyString(), anyInt(), anyInt()))
                    .thenThrow(new WriterException("Simulated WriterException"));

            emailService.sendEventReminder(user, event, reservations);
        }

        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.get(0);
        assertTrue(
                sentMail.getAttachments().isEmpty(),
                "Mail should have no attachments when QR code generation fails");
    }

    @Test
    void sendUpdateReservationConfirmation_AllSeatsDeleted_OmitsQrCodeAndSeatmap()
            throws IOException {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        seat.setSeatRow("1");
        Reservation deletedRes = createTestReservation(user, event, seat);

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));

        emailService.sendUpdateReservationConfirmation(user, List.of(deletedRes), List.of());
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.getFirst();
        assertEquals("Your Reservation Update", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains("You have deleted the following seats:"));
        assertFalse(
                sentMail.getHtml().contains("Your Check-in QR Code"),
                "QR code block should be omitted when all reservations are deleted");
        assertFalse(
                sentMail.getHtml().contains("cid:seatmap-image"),
                "Seatmap image should be omitted when all reservations are deleted");
        assertTrue(
                sentMail.getAttachments().isEmpty(),
                "Attachments list should be empty when no active seats remain");
    }

    @Test
    void sendUpdateReservationConfirmation_WithActiveSeats_IncludesQrCodeAndSeatmap()
            throws IOException {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat1 = createTestSeat(location, "A1");
        seat1.setSeatRow("1");
        Seat seat2 = createTestSeat(location, "A2");
        seat2.id = id(1001);
        seat2.setSeatRow("1");
        Reservation deletedRes = createTestReservation(user, event, seat1);
        Reservation activeRes = createTestReservation(user, event, seat2);
        activeRes.setCheckInToken(new CheckInToken(user, event, "ACTIVE_TOKEN"));

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat1, seat2));
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-update-token");
        when(emailSeatMapService.getPngImage(anyString()))
                .thenReturn(Optional.of(new byte[] {1, 2, 3}));

        emailService.sendUpdateReservationConfirmation(
                user, List.of(deletedRes), List.of(activeRes));
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.getFirst();
        assertEquals("Your Reservation Update", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains("You have deleted the following seats:"));
        assertTrue(sentMail.getHtml().contains("You have still reserved the following seats:"));
        assertTrue(sentMail.getHtml().contains("Your Check-in QR Code"));
        assertEquals(2, sentMail.getAttachments().size(), "Should attach seatmap and qrcode PNGs");
    }

    @Test
    void sendReservationConfirmation_Success() {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        seat.setSeatRow("1");
        Reservation reservation = createTestReservation(user, event, seat);
        List<Reservation> reservations = List.of(reservation);

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));
        when(reservationRepository.findByUserAndEvent(user, event)).thenReturn(reservations);
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-confirmation-token");
        when(emailSeatMapService.getPngImage(anyString()))
                .thenReturn(Optional.of(new byte[] {1, 2, 3}));

        String expectedQrContent = user.id + ";" + event.id + ";CODE123";
        try (MockedStatic<QRCodeImage> qrCodeImageMock = Mockito.mockStatic(QRCodeImage.class)) {
            qrCodeImageMock
                    .when(() -> QRCodeImage.generateQrCodeImage(anyString(), anyInt(), anyInt()))
                    .thenReturn(new byte[] {9, 9, 9});

            boolean sent = emailService.sendReservationConfirmation(user, reservations);
            assertTrue(sent, "Confirmation should be enqueued for a valid recipient");

            qrCodeImageMock.verify(
                    () ->
                            QRCodeImage.generateQrCodeImage(
                                    eq(expectedQrContent), anyInt(), anyInt()));
        }
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.getFirst();
        assertEquals("Your Reservation Confirmation", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains(event.getName()));
        assertTrue(sentMail.getHtml().contains("<li>A1 (1) - Parkett</li>"));
        assertFalse(
                sentMail.getHtml().contains("You have already reserved the following seats:"),
                "No 'already reserved' section should render when there are no other reservations");
        assertTrue(
                sentMail.getHtml()
                        .contains(
                                "http://localhost:8080/email/seatmap?token=test-confirmation-token"));
        assertEquals(2, sentMail.getAttachments().size(), "Should attach seatmap and qrcode PNGs");
    }

    @Test
    void sendReservationConfirmation_WithExistingReservation_ListsSeparately() {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat newSeat = createTestSeat(location, "A1");
        newSeat.setSeatRow("1");
        Seat existingSeat = createTestSeat(location, "B2");
        existingSeat.id = id(1001);
        existingSeat.setSeatRow("2");

        Reservation newReservation = createTestReservation(user, event, newSeat);
        Reservation existingReservation = createTestReservation(user, event, existingSeat);
        existingReservation.id = id(10001);

        when(seatRepository.findByIdsWithAreaAndEntrance(any()))
                .thenReturn(List.of(newSeat, existingSeat));
        when(reservationRepository.findByUserAndEvent(user, event))
                .thenReturn(List.of(newReservation, existingReservation));
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-confirmation-token");
        when(emailSeatMapService.getPngImage(anyString())).thenReturn(Optional.of(new byte[0]));

        emailService.sendReservationConfirmation(user, List.of(newReservation));
        emailDispatcher.drainQueue();

        Mail sentMail = mailbox.getMailsSentTo(user.getEmail()).getFirst();
        assertTrue(sentMail.getHtml().contains("You have already reserved the following seats:"));
        assertTrue(sentMail.getHtml().contains("<li>A1 (1) - Parkett</li>"));
        assertTrue(sentMail.getHtml().contains("<li>B2 (2) - Parkett</li>"));
    }

    @Test
    void getReservationConfirmationDisplayContent_EmbedsImagesAsDataUri() {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        Seat seat = createTestSeat(location, "A1");
        List<Reservation> reservations = List.of(createTestReservation(user, event, seat));

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));
        when(reservationRepository.findByUserAndEvent(user, event)).thenReturn(reservations);
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-display-token");
        when(emailSeatMapService.getPngImage(anyString()))
                .thenReturn(Optional.of(new byte[] {1, 2, 3}));

        String displayContent =
                emailService.getReservationConfirmationDisplayContent(user, reservations);

        assertFalse(displayContent.contains("cid:seatmap-image"));
        assertFalse(displayContent.contains("cid:qrcode-image"));
        assertTrue(displayContent.contains("data:image/png;base64,"));
        assertEquals(
                "Your Reservation Confirmation", emailService.getReservationConfirmationSubject());
    }

    @Test
    void sendEventReservationsCsvToManager_Success() throws Exception {
        User manager = createTestUser();
        manager.setUsername("manager1");
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);

        byte[] csvData = "seat,row\nA1,1\n".getBytes();
        when(reservationService.exportReservationsToCsv(event.id, manager)).thenReturn(csvData);

        emailService.sendEventReservationsCsvToManager(manager, event);
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(manager.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.getFirst();
        assertEquals(
                "Reservation overview for your event:" + event.getName(), sentMail.getSubject());
        assertTrue(
                sentMail.getHtml().contains(manager.getFirstname() + " " + manager.getLastname()));
        assertTrue(sentMail.getHtml().contains(event.getName()));
        assertTrue(sentMail.getHtml().contains(event.getEventLocation().getName()));

        assertEquals(1, sentMail.getAttachments().size());
        var attachment = sentMail.getAttachments().getFirst();
        assertEquals("reservations_" + event.id + ".csv", attachment.getName());
        assertEquals("text/csv", attachment.getContentType());
        assertFalse(attachment.isInlineAttachment());
    }

    @Test
    void sendEventRescheduledNotification_Success() {
        User user = createTestUser();
        EventLocation location = createTestEventLocation();
        Event event = createTestEvent(location);
        event.setBookingDeadline(Instant.now().plusSeconds(Duration.ofHours(12).toSeconds()));
        Seat seat = createTestSeat(location, "A1");
        seat.setSeatRow("1");
        Reservation reservation = createTestReservation(user, event, seat);
        List<Reservation> reservations = List.of(reservation);

        Instant oldStartTime = event.getStartTime().minusSeconds(7200);
        Instant oldEndTime = event.getEndTime().minusSeconds(7200);
        String oldLocation = "Alte Halle";
        Instant oldBookingDeadline = event.getBookingDeadline().minusSeconds(7200);

        when(seatRepository.findByIdsWithAreaAndEntrance(any())).thenReturn(List.of(seat));
        when(emailSeatMapService.createEmailSeatMapToken(any(), any(), any()))
                .thenReturn("test-reschedule-token");
        when(emailSeatMapService.getPngImage(anyString()))
                .thenReturn(Optional.of(new byte[] {1, 2, 3}));

        emailService.sendEventRescheduledNotification(
                user,
                event,
                reservations,
                oldStartTime,
                oldEndTime,
                oldLocation,
                oldBookingDeadline,
                null);
        emailDispatcher.drainQueue();

        List<Mail> sentMails = mailbox.getMailsSentTo(user.getEmail());
        assertEquals(1, sentMails.size());

        Mail sentMail = sentMails.getFirst();
        assertEquals("Important: Your Event Schedule Has Changed", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains(event.getName()));
        assertTrue(sentMail.getHtml().contains("Important: Event Schedule Update"));
        assertTrue(sentMail.getHtml().contains("A1 (1) - Parkett"));
        assertTrue(sentMail.getHtml().contains(oldLocation));
        assertTrue(sentMail.getHtml().contains(location.getName()));
        assertEquals(2, sentMail.getAttachments().size(), "Should attach seatmap and qrcode PNGs");
    }
}
