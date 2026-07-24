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
package de.felixhertweck.seatreservation.email.service;

import java.io.IOException;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.google.zxing.WriterException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.email.queue.EmailAttachment;
import de.felixhertweck.seatreservation.email.queue.EmailMessage;
import de.felixhertweck.seatreservation.email.queue.EmailQueueService;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.QRCodeImage;
import de.felixhertweck.seatreservation.utils.VerificationCodeGenerator;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * EmailService is responsible for sending email confirmations to users. It handles both email
 * verification and reservation confirmations.
 */
@ApplicationScoped
public class EmailService {
    @ConfigProperty(name = "email.header.password-reset", defaultValue = "Password Reset Request")
    String EMAIL_HEADER_PASSWORD_RESET;

    @ConfigProperty(name = "email.header.confirmation", defaultValue = "Email Confirmation")
    String EMAIL_HEADER_CONFIRMATION;

    @ConfigProperty(name = "email.header.password-changed", defaultValue = "Password Changed")
    String EMAIL_HEADER_PASSWORD_CHANGED;

    @ConfigProperty(
            name = "email.header.reservation-confirmation",
            defaultValue = "Reservation Confirmation")
    String EMAIL_HEADER_RESERVATION_CONFIRMATION;

    @ConfigProperty(name = "email.header.reservation-update", defaultValue = "Reservation Update")
    String EMAIL_HEADER_RESERVATION_UPDATE;

    @ConfigProperty(name = "email.header.reminder", defaultValue = "Reservation Reminder")
    String EMAIL_HEADER_REMINDER;

    @ConfigProperty(
            name = "email.header.reservation-overview",
            defaultValue = "Reservation Overview")
    String EMAIL_HEADER_RESERVATION_OVERVIEW;

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject EmailQueueService emailQueueService;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject SeatRepository seatRepository;

    @Inject ReservationService reservationService;

    @Inject EmailSeatMapService emailSeatMapService;

    @ConfigProperty(name = "email.frontend-base-url", defaultValue = "")
    String frontendBaseUrl;

    @ConfigProperty(name = "email.verification.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    @ConfigProperty(name = "email.bcc-address")
    Optional<String> bccAddress;

    @ConfigProperty(name = "email.entrance-info-template")
    String entranceInfoTemplate;

    @Location("email/password-reset")
    Template passwordResetTemplate;

    @Location("email/email-confirmation")
    Template emailConfirmationTemplate;

    @Inject
    @Location("email/password-changed")
    Template passwordChangedTemplate;

    @Inject
    @Location("email/event-reminder")
    Template eventReminderTemplate;

    @Inject
    @Location("email/reservation-confirmation")
    Template reservationConfirmationTemplate;

    @Inject
    @Location("email/reservation-update-confirmation")
    Template reservationUpdateTemplate;

    @Inject
    @Location("email/manager-reservation-export")
    Template managerExportTemplate;

    /**
     * Sends an email confirmation to the specified user.
     *
     * @param user the user to whom the confirmation email will be sent
     * @param emailVerification the EmailVerification object to use for the link
     * @throws IOException if the email template cannot be read
     */
    /**
     * Sends a password reset email to the specified user.
     *
     * @param user the user to whom the password reset email will be sent
     * @param passwordResetToken the PasswordResetToken object to use for the link
     * @throws IOException if the email template cannot be read
     */
    public void sendPasswordResetEmail(User user, PasswordResetToken passwordResetToken)
            throws IOException {
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for password reset.");
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for password reset.");
            return;
        }

        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        String resetLink =
                frontendBaseUrl + "/reset-password?token=" + passwordResetToken.getToken();

        String htmlContent =
                passwordResetTemplate
                        .data("fullName", fullName(user))
                        .data("resetLink", resetLink)
                        .data(
                                "expirationTime",
                                formatDateTime(passwordResetToken.getExpirationTime()))
                        .data("currentYear", currentYear())
                        .render();

        // Queue the email for asynchronous, retried delivery
        LOG.debugf("Password reset subject: %s", EMAIL_HEADER_PASSWORD_RESET);
        enqueue(
                List.of(user.getEmail()),
                EMAIL_HEADER_PASSWORD_RESET,
                htmlContent,
                List.of(),
                false);
    }

    public void sendEmailConfirmation(User user, EmailVerification emailVerification)
            throws IOException {
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for email confirmation.");
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for email confirmation.");
            return;
        }

        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        String htmlContent =
                emailConfirmationTemplate
                        .data("fullName", fullName(user))
                        .data("verificationCode", emailVerification.getToken())
                        .data(
                                "verificationLink",
                                generateVerificationLink(emailVerification.getToken()))
                        .data(
                                "expirationTime",
                                formatDateTime(emailVerification.getExpirationTime()))
                        .data("currentYear", currentYear())
                        .render();

        // Queue the email for asynchronous, retried delivery
        LOG.debugf("Email confirmation subject: %s", EMAIL_HEADER_CONFIRMATION);
        enqueue(List.of(user.getEmail()), EMAIL_HEADER_CONFIRMATION, htmlContent, List.of(), false);
    }

    /**
     * Creates a new email verification entry and returns it.
     *
     * @param user The user for whom to create the verification.
     * @return The newly created EmailVerification object.
     */
    public EmailVerification createEmailVerification(User user) {
        LOG.debugf("Creating new email verification for user ID: %d", user.id);
        String verificationCode = VerificationCodeGenerator.generate();
        Instant expirationTime = Instant.now().plusSeconds(expirationMinutes * 60);
        EmailVerification emailVerification =
                new EmailVerification(user, verificationCode, expirationTime);
        emailVerificationRepository.persist(emailVerification);
        LOG.debugf(
                "Email verification entry persisted for user ID %d with verification code",
                user.id);
        return emailVerification;
    }

    /**
     * Updates an existing email verification entry's expiration time and returns the updated entry.
     *
     * @param emailVerification The EmailVerification object to update.
     * @return The updated EmailVerification object.
     */
    public EmailVerification updateEmailVerificationExpiration(
            EmailVerification emailVerification) {
        emailVerification.setExpirationTime(Instant.now().plusSeconds(expirationMinutes * 60));
        emailVerificationRepository.persist(emailVerification);
        LOG.debugf(
                "Email verification entry ID %d expiration time updated to: %s",
                emailVerification.id, emailVerification.getExpirationTime());
        return emailVerification;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Formats an instant in the system time zone using the shared email date/time pattern.
     *
     * @param instant the instant to format
     * @return the formatted date/time string
     */
    private String formatDateTime(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
    }

    /**
     * Returns the current year as a string for the template footers.
     *
     * @return the current year
     */
    private String currentYear() {
        return Year.now().toString();
    }

    /**
     * Builds a user's display name from first and last name.
     *
     * @param user the user
     * @return the concatenated full name
     */
    private String fullName(User user) {
        return user.getFirstname() + " " + user.getLastname();
    }

    /**
     * Maps reservations to the seat views rendered by the templates.
     *
     * @param reservations the reservations to map (may be {@code null})
     * @return the seat views, in encounter order
     */
    private List<SeatView> toSeatViews(List<Reservation> reservations) {
        if (reservations == null) {
            return List.of();
        }
        return reservations.stream()
                .map(Reservation::getSeat)
                .map(
                        seat ->
                                new SeatView(
                                        seat.getSeatNumber(),
                                        seat.getSeatRow(),
                                        seat.getArea() != null ? seat.getArea().getName() : null))
                .collect(Collectors.toList());
    }

    /**
     * Generates a link to the event reservation page.
     *
     * @param eventId The ID of the event.
     * @return The complete event reservation link.
     */
    private String generateEventLink(Long eventId) {
        return frontendBaseUrl.trim() + "/reservations?id=" + eventId;
    }

    /**
     * Generates a link to the email seatmap page.
     *
     * @param token The email seatmap token.
     * @return The complete seatmap link.
     */
    private String generateSeatmapLink(String token) {
        return frontendBaseUrl.trim() + "/email/seatmap?token=" + token;
    }

    /**
     * Generates a verification link for email confirmation.
     *
     * @param verificationCode The verification code to include in the link.
     * @return The complete verification link.
     */
    private String generateVerificationLink(String verificationCode) {
        return frontendBaseUrl.trim() + "/verify?code=" + verificationCode;
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @param additionalMailAddress An optional email address to override the user's email.
     * @throws IOException If an error occurs while sending the email.
     */
    public void sendReservationConfirmation(
            User user, List<Reservation> reservations, String additionalMailAddress)
            throws IOException {
        List<String> emailAddresses = new ArrayList<>();
        if (!skipForNullOrEmptyAddress(user.getEmail())
                && !skipForLocalhostAddress(user.getEmail())) {
            emailAddresses.add(user.getEmail());
        }
        if (!skipForNullOrEmptyAddress(additionalMailAddress)
                && !skipForLocalhostAddress(additionalMailAddress)
                && !emailAddresses.contains(additionalMailAddress)) {
            emailAddresses.add(additionalMailAddress);
        }
        if (emailAddresses.isEmpty()) {
            LOG.warn("No valid email addresses provided for reservation confirmation.");
            return;
        }

        LOG.debug(
                String.format(
                        "User ID: %d, Number of reservations: %d",
                        user.id, reservations != null ? reservations.size() : 0));

        if (reservations == null || reservations.isEmpty()) {
            LOG.warnf(
                    "No reservations provided for confirmation email to user %s.", user.getEmail());
            return;
        }

        Event event = reservations.getFirst().getEvent();
        String eventName = event.getName();
        LOG.debugf("Event for reservation confirmation: %s (ID: %d)", eventName, event.id);

        // Create email seatmap token
        String seatmapToken =
                emailSeatMapService.createEmailSeatMapToken(user, event, reservations);
        String seatmapLink = generateSeatmapLink(seatmapToken);
        LOG.debugf("Created email seatmap token: %s", seatmapToken);

        // Get PNG image from EmailSeatMapService
        Optional<byte[]> pngImageOpt = emailSeatMapService.getPngImage(seatmapToken);
        byte[] pngImage = pngImageOpt.orElse(new byte[0]);
        LOG.debugf("Retrieved PNG image with size: %d bytes", pngImage.length);

        // Prepare data for seat list rendering
        List<Seat> allSeats = seatRepository.findByEventLocation(event.getEventLocation());
        List<Reservation> allUserReservationsForEvent =
                reservationRepository.findByUserAndEvent(user, event);
        LOG.debugf(
                "Retrieved %d total seats and %d user reservations for event %s.",
                allSeats.size(), allUserReservationsForEvent.size(), eventName);

        Set<Seat> newSeatNumbers =
                reservations.stream().map(Reservation::getSeat).collect(Collectors.toSet());
        LOG.debugf("New seat numbers for confirmation: %s", newSeatNumbers);

        Set<Seat> existingSeatNumbers =
                allUserReservationsForEvent.stream()
                        .map(Reservation::getSeat)
                        .collect(Collectors.toSet());
        existingSeatNumbers.removeAll(newSeatNumbers); // Keep only previously reserved seats
        LOG.debugf("Existing seat numbers (excluding new ones): %s", existingSeatNumbers);

        List<SeatView> newSeats = toSeatViews(reservations);
        List<SeatView> existingSeats =
                existingSeatNumbers.stream()
                        .map(
                                seat ->
                                        new SeatView(
                                                seat.getSeatNumber(),
                                                seat.getSeatRow(),
                                                seat.getArea() != null
                                                        ? seat.getArea().getName()
                                                        : null))
                        .collect(Collectors.toList());

        String entranceInfo = generateEntranceInfo(reservations);

        String htmlContent =
                reservationConfirmationTemplate
                        .data("userName", user.getUsername())
                        .data("fullName", fullName(user))
                        .data("eventName", eventName != null ? eventName : "")
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("eventStartTime", formatDateTime(event.getStartTime()))
                        .data("eventEndTime", formatDateTime(event.getEndTime()))
                        .data("newSeats", newSeats)
                        .data("hasExistingSeats", !existingSeats.isEmpty())
                        .data("existingSeats", existingSeats)
                        .data("entranceInfo", entranceInfo)
                        .data("eventLink", generateEventLink(event.id))
                        .data("seatmapLink", seatmapLink)
                        .data("currentYear", currentYear())
                        .render();

        String qrCodeContent = generateQrCodeContent(user, event, reservations);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        enqueue(
                emailAddresses,
                EMAIL_HEADER_RESERVATION_CONFIRMATION,
                htmlContent,
                buildImageAttachments(pngImage, qrCodeImage),
                true);
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @throws IOException If an error occurs while sending the email.
     */
    public void sendReservationConfirmation(User user, List<Reservation> reservations)
            throws IOException {
        sendReservationConfirmation(user, reservations, null);
    }

    /**
     * Sends an update reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param deletedReservations The list of deleted reservations.
     * @param activeReservations The list of active reservations.
     * @param additionalMailAddress An optional email address to override the user's email.
     * @throws IOException If an I/O error occurs while sending the email.
     */
    public void sendUpdateReservationConfirmation(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations,
            String additionalMailAddress)
            throws IOException {
        List<String> emailAddresses = new ArrayList<>();
        if (!skipForNullOrEmptyAddress(user.getEmail())
                && !skipForLocalhostAddress(user.getEmail())) {
            emailAddresses.add(user.getEmail());
        }
        if (!skipForNullOrEmptyAddress(additionalMailAddress)
                && !skipForLocalhostAddress(additionalMailAddress)
                && !emailAddresses.contains(additionalMailAddress)) {
            emailAddresses.add(additionalMailAddress);
        }
        if (emailAddresses.isEmpty()) {
            LOG.warn("No valid email addresses provided for update reservation confirmation.");
            return;
        }

        LOG.debug(
                String.format(
                        "User ID: %d, Number of reservations: %d",
                        user.id, activeReservations != null ? activeReservations.size() : 0));
        Log.debug(
                String.format(
                        "User ID: %d, Number of deleted reservations: %d",
                        user.id, deletedReservations != null ? deletedReservations.size() : 0));

        if (deletedReservations == null || deletedReservations.isEmpty()) {
            LOG.warnf("No reservations deleted to user %s.", user.getEmail());
            return;
        }

        Event event = deletedReservations.getFirst().getEvent();
        String eventName = event.getName();
        LOG.debugf("Event for reservation confirmation: %s (ID: %d)", eventName, event.id);

        // Create email seatmap token with active reservations
        String seatmapToken =
                emailSeatMapService.createEmailSeatMapToken(
                        user, event, activeReservations != null ? activeReservations : List.of());
        String seatmapLink = generateSeatmapLink(seatmapToken);
        LOG.debugf("Created email seatmap token: %s", seatmapToken);

        // Get PNG image from EmailSeatMapService
        Optional<byte[]> pngImageOpt = emailSeatMapService.getPngImage(seatmapToken);
        byte[] pngImage = pngImageOpt.orElse(new byte[0]);
        LOG.debugf("Retrieved PNG image with size: %d bytes", pngImage.length);

        // Prepare data for seat list rendering
        List<Seat> allSeats = seatRepository.findByEventLocation(event.getEventLocation());

        LOG.debugf(
                "Retrieved %d total seats and %d user reservations for event %s.",
                allSeats.size(),
                activeReservations != null ? activeReservations.size() : 0,
                eventName);

        List<SeatView> deletedSeats = toSeatViews(deletedReservations);
        List<SeatView> activeSeats = toSeatViews(activeReservations);

        String entranceInfo = generateEntranceInfo(activeReservations);

        String htmlContent =
                reservationUpdateTemplate
                        .data("userName", user.getUsername())
                        .data("fullName", fullName(user))
                        .data("eventName", eventName != null ? eventName : "")
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("eventStartTime", formatDateTime(event.getStartTime()))
                        .data("eventEndTime", formatDateTime(event.getEndTime()))
                        .data("deletedSeats", deletedSeats)
                        .data("hasActiveSeats", !activeSeats.isEmpty())
                        .data("activeSeats", activeSeats)
                        .data("entranceInfo", entranceInfo)
                        .data("eventLink", generateEventLink(event.id))
                        .data("seatmapLink", seatmapLink)
                        .data("currentYear", currentYear())
                        .render();

        String qrCodeContent = generateQrCodeContent(user, event, activeReservations);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        enqueue(
                emailAddresses,
                EMAIL_HEADER_RESERVATION_UPDATE,
                htmlContent,
                buildImageAttachments(pngImage, qrCodeImage),
                true);
    }

    /**
     * Sends an update reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param deletedReservations The list of deleted reservations.
     * @param activeReservations The list of active reservations.
     * @throws IOException If an I/O error occurs while sending the email.
     */
    public void sendUpdateReservationConfirmation(
            User user, List<Reservation> deletedReservations, List<Reservation> activeReservations)
            throws IOException {
        sendUpdateReservationConfirmation(user, deletedReservations, activeReservations, null);
    }

    /**
     * Sends a password changed notification email to the specified user.
     *
     * @param user the user to whom the password changed email will be sent
     * @throws IOException if the email template cannot be read
     */
    public void sendPasswordChangedNotification(User user) throws IOException {
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for password change notification.");
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for password change notification.");
            return;
        }

        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        String htmlContent =
                passwordChangedTemplate
                        .data("fullName", fullName(user))
                        .data("currentYear", currentYear())
                        .render();

        // Queue the email for asynchronous, retried delivery
        enqueue(
                List.of(user.getEmail()),
                EMAIL_HEADER_PASSWORD_CHANGED,
                htmlContent,
                List.of(),
                false);
    }

    /**
     * Sends an event reminder email to the specified user.
     *
     * @param user the user to whom the reminder email will be sent
     * @param event the event for which the reminder is being sent
     * @param reservations the list of reservations made by the user for the event
     */
    public void sendEventReminder(User user, Event event, List<Reservation> reservations) {
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for event reminder.");
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for event reminder.");
            return;
        }

        LOG.debugf(
                String.format(
                        "User ID: %d, Event ID: %d, Number of reservations: %d",
                        user.id, event.id, reservations.size()));

        // Create email seatmap token
        String seatmapToken =
                emailSeatMapService.createEmailSeatMapToken(user, event, reservations);
        String seatmapLink = generateSeatmapLink(seatmapToken);
        LOG.debugf("Created email seatmap token: %s", seatmapToken);

        // Get PNG image from EmailSeatMapService
        Optional<byte[]> pngImageOpt = emailSeatMapService.getPngImage(seatmapToken);
        byte[] pngImage = pngImageOpt.orElse(new byte[0]);
        LOG.debugf("Retrieved PNG image with size: %d bytes", pngImage.length);

        List<SeatView> seats = toSeatViews(reservations);
        String entranceInfo = generateEntranceInfo(reservations);

        String htmlContent =
                eventReminderTemplate
                        .data("userName", user.getUsername())
                        .data("fullName", fullName(user))
                        .data("eventName", event.getName())
                        .data(
                                "eventDate",
                                event.getStartTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toString())
                        .data(
                                "eventTime",
                                event.getStartTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalTime()
                                        .toString())
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("seats", seats)
                        .data("entranceInfo", entranceInfo)
                        .data("seatmapLink", seatmapLink)
                        .data("eventLink", generateEventLink(event.id))
                        .data("currentYear", currentYear())
                        .render();

        // Queue the email for asynchronous, retried delivery
        LOG.debugf("Event reminder subject: %s", EMAIL_HEADER_REMINDER);

        String qrCodeContent = generateQrCodeContent(user, event, reservations);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        enqueue(
                List.of(user.getEmail()),
                EMAIL_HEADER_REMINDER,
                htmlContent,
                buildImageAttachments(pngImage, qrCodeImage),
                false);
    }

    /**
     * Sends an email to the event manager with a CSV export of all reservations for a given event.
     *
     * @param manager the manager of the event
     * @param event the event for which the reservations are to be exported
     * @throws IOException if the email template cannot be read or CSV export fails
     * @throws EventNotFoundException if the event is not found
     * @throws SecurityException if there are security issues during CSV export
     */
    public void sendEventReservationsCsvToManager(User manager, Event event)
            throws EventNotFoundException, SecurityException, IOException {
        if (skipForNullOrEmptyAddress(manager.getEmail())) {
            LOG.warn("No valid email addresses provided to send CSV export.");
            return;
        }
        if (skipForLocalhostAddress(manager.getEmail())) {
            LOG.warn("No valid email addresses provided to send CSV export.");
            return;
        }
        LOG.debugf("Manager ID: %d, Event ID: %d", manager.id, event.id);

        // Generate CSV data
        byte[] csvData = reservationService.exportReservationsToCsv(event.id, manager);
        LOG.debugf(
                "Generated CSV data of size %d bytes for event ID: %d", csvData.length, event.id);

        String htmlContent =
                managerExportTemplate
                        .data("fullName", fullName(manager))
                        .data("eventName", event.getName())
                        .data(
                                "eventDate",
                                event.getStartTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toString())
                        .data(
                                "eventTime",
                                event.getStartTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalTime()
                                        .toString())
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("currentYear", currentYear())
                        .render();

        // Queue the email with the CSV attachment for asynchronous, retried delivery
        EmailAttachment csvAttachment =
                EmailAttachment.file("reservations_" + event.id + ".csv", "text/csv", csvData);

        enqueue(
                List.of(manager.getEmail()),
                EMAIL_HEADER_RESERVATION_OVERVIEW + event.getName(),
                htmlContent,
                List.of(csvAttachment),
                false);
    }

    /**
     * Checks if an email address is null or empty and should be skipped.
     *
     * @param address the email address to check
     * @return true if the address is null or empty, false otherwise
     */
    private boolean skipForNullOrEmptyAddress(String address) {
        if (address == null || address.isEmpty()) {
            LOG.debug("Skipping email sending for null or empty address.");
            return true;
        }
        return false;
    }

    /**
     * Checks if an email address is a localhost address and should be skipped.
     *
     * @param address the email address to check
     * @return true if the address ends with @localhost, false otherwise
     */
    private boolean skipForLocalhostAddress(String address) {
        if (address.endsWith("@localhost")) {
            LOG.debugf("Skipping email sending for localhost address: %s", address);
            return true;
        }
        return false;
    }

    /**
     * Builds an {@link EmailMessage} from the given content and hands it off to the email queue.
     * The Bcc address is added only when {@code includeBcc} is {@code true} and the address is
     * present, non-empty, and not already included in the recipients.
     *
     * @param recipients the To/Cc recipients (first entry becomes To, the rest Cc)
     * @param subject the email subject
     * @param htmlContent the rendered HTML body
     * @param attachments the attachments to include, if any
     * @param includeBcc whether the configured Bcc address should be added
     */
    private void enqueue(
            List<String> recipients,
            String subject,
            String htmlContent,
            List<EmailAttachment> attachments,
            boolean includeBcc) {
        EmailMessage.Builder builder =
                EmailMessage.builder().subject(subject).htmlBody(htmlContent);

        if (!recipients.isEmpty()) {
            builder.to(recipients.getFirst());
            recipients.subList(1, recipients.size()).forEach(builder::cc);
        }

        if (includeBcc) {
            bccAddress.ifPresent(
                    address -> {
                        if (!address.trim().isEmpty() && !recipients.contains(address)) {
                            builder.bcc(address);
                        }
                    });
        }

        if (attachments != null) {
            attachments.forEach(builder::attachment);
        }

        emailQueueService.enqueue(builder.build());
    }

    /**
     * Builds the list of inline image attachments (seat map and QR code) shared by the reservation
     * and reminder emails, skipping any image that could not be rendered.
     *
     * @param seatmapPng the rendered seat map PNG (may be empty)
     * @param qrCode the rendered QR code PNG (may be empty)
     * @return the non-empty inline attachments, referenced from the templates via their content-id
     */
    private List<EmailAttachment> buildImageAttachments(byte[] seatmapPng, byte[] qrCode) {
        List<EmailAttachment> attachments = new ArrayList<>();
        if (seatmapPng != null && seatmapPng.length > 0) {
            attachments.add(
                    EmailAttachment.inline(
                            "seatmap.png", "image/png", "seatmap-image", seatmapPng));
        }
        if (qrCode != null && qrCode.length > 0) {
            attachments.add(
                    EmailAttachment.inline("qrcode.png", "image/png", "qrcode-image", qrCode));
        }
        return attachments;
    }

    /**
     * Generates an entrance information text from a list of reservations. Groups seats by their
     * entrance and creates a formatted text according to the configured template.
     *
     * @param reservations the list of reservations to process
     * @return a formatted text describing which entrance to use for which seats, or an empty string
     *     if no valid entrance information is available
     */
    private String generateEntranceInfo(List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return "";
        }

        // Group seats by entrance
        var seatsByEntrance =
                reservations.stream()
                        .map(Reservation::getSeat)
                        .filter(
                                seat ->
                                        seat.getEntrance() != null
                                                && !seat.getEntrance().getName().trim().isEmpty())
                        .collect(
                                Collectors.groupingBy(
                                        seat -> seat.getEntrance().getName(),
                                        Collectors.mapping(
                                                Seat::getSeatNumber, Collectors.toList())));

        if (seatsByEntrance.isEmpty()) {
            return "";
        }

        // Build the entrance info text
        StringBuilder result = new StringBuilder();

        for (Entry<String, List<String>> entry : seatsByEntrance.entrySet()) {
            String entrance = entry.getKey();
            List<String> seatNumbers = entry.getValue();

            // Join seat numbers with comma
            String seatsText = String.join(", ", seatNumbers);

            result.append(
                    entranceInfoTemplate
                            .replace("{seats}", seatsText)
                            .replace("{entrance}", entrance));
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * Generates the content string for the QR code.
     *
     * @param user The user for whom the QR code is generated.
     * @param event The event for which the QR code is generated.
     * @param reservations The list of reservations to include in the QR code content.
     * @return The formatted QR code content string.
     */
    private String generateQrCodeContent(User user, Event event, List<Reservation> reservations) {
        StringBuilder sb = new StringBuilder();
        sb.append(user.id.toString());
        sb.append(";");
        sb.append(event.id.toString());
        sb.append(";");
        reservations.stream()
                .map(Reservation::getCheckInCode)
                .forEach(code -> sb.append(code).append(","));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Generates a QR code image as a byte array from the given content string.
     *
     * @param qrCodeContent The content to encode in the QR code.
     * @return A byte array representing the QR code image in PNG format.
     */
    private byte[] generateQrCodeImage(String qrCodeContent) {
        byte[] qrCodeImage = new byte[0];
        try {
            qrCodeImage = QRCodeImage.generateQrCodeImage(qrCodeContent, 400, 400);
            LOG.debugf("QR Code image generated with size: %d bytes", qrCodeImage.length);
        } catch (WriterException | IOException e) {
            LOG.errorf(e, "Failed to generate QR code for reservation confirmation.");
        }
        return qrCodeImage;
    }
}
