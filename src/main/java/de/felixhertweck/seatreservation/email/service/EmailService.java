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
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.google.zxing.WriterException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.email.queue.EmailAttachment;
import de.felixhertweck.seatreservation.email.queue.EmailMessage;
import de.felixhertweck.seatreservation.email.queue.EmailQueueService;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.EmailPriority;
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
    @ConfigProperty(name = "email.header.confirmation", defaultValue = "Email Confirmation")
    String EMAIL_HEADER_CONFIRMATION;

    @ConfigProperty(name = "email.header.password-changed", defaultValue = "Password Changed")
    String EMAIL_HEADER_PASSWORD_CHANGED;

    @ConfigProperty(
            name = "email.header.reservation-confirmation",
            defaultValue = "Reservation Confirmation")
    String EMAIL_HEADER_RESERVATION_CONFIRMATION;

    @ConfigProperty(
            name = "email.header.boxoffice-confirmation",
            defaultValue = "Box Office Reservation Confirmation")
    String EMAIL_HEADER_BOXOFFICE_CONFIRMATION;

    @ConfigProperty(name = "email.header.reservation-update", defaultValue = "Reservation Update")
    String EMAIL_HEADER_RESERVATION_UPDATE;

    @ConfigProperty(name = "email.header.reminder", defaultValue = "Reservation Reminder")
    String EMAIL_HEADER_REMINDER;

    @ConfigProperty(
            name = "email.header.reservation-overview",
            defaultValue = "Reservation Overview")
    String EMAIL_HEADER_RESERVATION_OVERVIEW;

    @ConfigProperty(name = "email.header.password-reset", defaultValue = "Password Reset Request")
    String EMAIL_HEADER_PASSWORD_RESET;

    @ConfigProperty(
            name = "email.header.username-recovery",
            defaultValue = "Username Recovery Request")
    String EMAIL_HEADER_USERNAME_RECOVERY;

    @ConfigProperty(
            name = "email.header.two-factor",
            defaultValue = "Two-Factor Authentication Code")
    String EMAIL_HEADER_TWO_FACTOR;

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

    @Inject
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
    @Location("email/boxoffice-reservation-confirmation")
    Template boxOfficeConfirmationTemplate;

    @Inject
    @Location("email/reservation-update-confirmation")
    Template reservationUpdateTemplate;

    @Inject
    @Location("email/manager-reservation-export")
    Template managerExportTemplate;

    @Inject
    @Location("email/password-reset")
    Template passwordResetTemplate;

    @Inject
    @Location("email/username-recovery")
    Template usernameRecoveryTemplate;

    @Inject
    @Location("email/two-factor-code")
    Template twoFactorCodeTemplate;

    /**
     * Sends a two-factor authentication code email to the specified user.
     *
     * @param user the user to send the code to
     * @param code the 2FA verification code
     */
    public void sendTwoFactorCode(User user, String code) {
        if (skipForNullOrEmptyAddress(user.getEmail())
                || skipForLocalhostAddress(user.getEmail())) {
            LOG.warn("No valid email address provided for 2FA code.");
            return;
        }

        LOG.debugf(
                "Sending 2FA code email to User ID: %s, Username: %s", user.id, user.getUsername());

        String htmlContent =
                twoFactorCodeTemplate
                        .data("fullName", fullName(user))
                        .data("code", code)
                        .data("currentYear", currentYear())
                        .render();

        enqueue(List.of(user.getEmail()), EMAIL_HEADER_TWO_FACTOR, htmlContent, List.of(), false);
    }

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

        LOG.debugf("User ID: %s, Username: %s", user.id, user.getUsername());

        String resetLink =
                frontendBaseUrl.trim() + "/reset-password?token=" + passwordResetToken.getToken();

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

    /**
     * Sends an email listing every username associated with the given email address.
     *
     * @param email the email address to send the recovery message to
     * @param usernames the usernames associated with this email address
     * @throws IOException if the email template cannot be read
     */
    public void sendUsernameRecoveryEmail(String email, List<String> usernames) throws IOException {
        if (skipForNullOrEmptyAddress(email)) {
            LOG.warn("No valid email addresses provided for username recovery.");
            return;
        }
        if (skipForLocalhostAddress(email)) {
            LOG.warn("No valid email addresses provided for username recovery.");
            return;
        }

        String htmlContent =
                usernameRecoveryTemplate
                        .data("usernames", usernames)
                        .data("currentYear", currentYear())
                        .render();

        // Queue the email for asynchronous, retried delivery
        LOG.debugf("Username recovery subject: %s", EMAIL_HEADER_USERNAME_RECOVERY);
        enqueue(List.of(email), EMAIL_HEADER_USERNAME_RECOVERY, htmlContent, List.of(), false);
    }

    /**
     * Sends an email confirmation to the specified user.
     *
     * @param user the user to whom the confirmation email will be sent
     * @param emailVerification the EmailVerification object to use for the link
     * @throws IOException if the email template cannot be read
     */
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

        LOG.debugf("User ID: %s, Username: %s", user.id, user.getUsername());

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
        LOG.debugf("Creating new email verification for user ID: %s", user.id);
        String verificationCode = VerificationCodeGenerator.generate();
        Instant expirationTime = Instant.now().plusSeconds(expirationMinutes * 60);
        EmailVerification emailVerification =
                new EmailVerification(user, verificationCode, expirationTime);
        emailVerificationRepository.persist(emailVerification);
        LOG.debugf(
                "Email verification entry persisted for user ID %s with verification code",
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
                "Email verification entry ID %s expiration time updated to: %s",
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
     * Bulk-loads the seats (with area and entrance pre-fetched) referenced by the given reservation
     * lists, keyed by seat ID. Avoids triggering one lazy-load query per reservation when the
     * seat's number, row, area, or entrance is read afterwards.
     *
     * @param reservationLists the reservation lists whose seats should be loaded (entries may be
     *     {@code null})
     * @return the referenced seats, keyed by seat ID
     */
    @SafeVarargs
    private final Map<UUID, Seat> loadSeatsForReservations(List<Reservation>... reservationLists) {
        Set<UUID> seatIds = new HashSet<>();
        for (List<Reservation> reservations : reservationLists) {
            if (reservations != null) {
                reservations.forEach(r -> seatIds.add(r.getSeat().getId()));
            }
        }
        return seatRepository.findByIdsWithAreaAndEntrance(seatIds).stream()
                .collect(Collectors.toMap(s -> s.id, s -> s));
    }

    /**
     * Maps reservations to the seat views rendered by the templates.
     *
     * @param reservations the reservations to map (may be {@code null})
     * @param seatById the pre-loaded seats referenced by the reservations, keyed by seat ID
     * @return the seat views, in encounter order
     */
    private List<SeatView> toSeatViews(List<Reservation> reservations, Map<UUID, Seat> seatById) {
        if (reservations == null) {
            return List.of();
        }
        return reservations.stream()
                .map(r -> seatById.get(r.getSeat().getId()))
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
    private String generateEventLink(UUID eventId) {
        return frontendBaseUrl.trim() + "/events/reservations?eventId=" + eventId;
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
     * The rendered reservation confirmation, shared by {@link #sendReservationConfirmation} and
     * {@link #getReservationConfirmationDisplayContent} -- {@code htmlContent} still references the
     * seatmap/QR images via {@code cid:} placeholders, which the email path resolves through MIME
     * attachments and the display path resolves by inlining them as {@code data:} URIs.
     *
     * @param htmlContent the rendered HTML, referencing images via {@code cid:seatmap-image} and
     *     {@code cid:qrcode-image}
     * @param seatmapPng the rendered seat map PNG (may be empty)
     * @param qrCodeImage the QR code PNG (may be empty)
     */
    private record ReservationConfirmationContent(
            String htmlContent, byte[] seatmapPng, byte[] qrCodeImage) {}

    /**
     * Renders the reservation confirmation email content shared by the queued email and the
     * on-screen display view.
     *
     * @param user The user to whom the reservations belong.
     * @param reservations The list of reservations to include (must be non-null and non-empty).
     * @param includeExistingReservations when {@code false}, the "already reserved" seat list is
     *     not computed from {@code user}'s other reservations for the event. Needed for the box
     *     office's shared guest account, where every walk-in reservation is stored under the same
     *     {@code User} row -- without this, one guest's confirmation would list every other guest's
     *     box-office seats for the same event as "already reserved".
     * @return the rendered content, with images still referenced via {@code cid:} placeholders
     */
    private ReservationConfirmationContent renderReservationConfirmation(
            User user, List<Reservation> reservations, boolean includeExistingReservations) {
        Event event = reservations.getFirst().getEvent();
        String eventName = event.getName();
        LOG.debugf("Event for reservation confirmation: %s (ID: %s)", eventName, event.id);

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
        List<Reservation> allUserReservationsForEvent =
                includeExistingReservations
                        ? reservationRepository.findByUserAndEvent(user, event)
                        : reservations;
        LOG.debugf(
                "Retrieved %d user reservations for event %s.",
                allUserReservationsForEvent.size(), eventName);

        Map<UUID, Seat> seatById =
                loadSeatsForReservations(reservations, allUserReservationsForEvent);

        Set<UUID> newSeatIds =
                reservations.stream().map(r -> r.getSeat().getId()).collect(Collectors.toSet());
        LOG.debugf("New seat ids for confirmation: %s", newSeatIds);

        Set<UUID> existingSeatIds =
                allUserReservationsForEvent.stream()
                        .map(r -> r.getSeat().getId())
                        .collect(Collectors.toSet());
        existingSeatIds.removeAll(newSeatIds); // Keep only previously reserved seats
        LOG.debugf("Existing seat ids (excluding new ones): %s", existingSeatIds);

        List<SeatView> newSeats = toSeatViews(reservations, seatById);
        List<SeatView> existingSeats =
                existingSeatIds.stream()
                        .map(seatById::get)
                        .map(
                                seat ->
                                        new SeatView(
                                                seat.getSeatNumber(),
                                                seat.getSeatRow(),
                                                seat.getArea() != null
                                                        ? seat.getArea().getName()
                                                        : null))
                        .collect(Collectors.toList());

        String entranceInfo = generateEntranceInfo(reservations, seatById);

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

        CheckInToken token = reservations.getFirst().getCheckInToken();
        String qrCodeContent = generateQrCodeContent(user, event, token);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        return new ReservationConfirmationContent(htmlContent, pngImage, qrCodeImage);
    }

    /**
     * Replaces a {@code cid:} image placeholder with an inline {@code data:} URI, or with an empty
     * string when the image could not be rendered.
     *
     * @param htmlContent the HTML containing the placeholder
     * @param cidPlaceholder the {@code cid:...} placeholder to replace
     * @param image the image bytes to embed (may be empty)
     * @return the HTML with the placeholder resolved
     */
    private String embedImageAsDataUri(String htmlContent, String cidPlaceholder, byte[] image) {
        if (image != null && image.length > 0) {
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
            return htmlContent.replace(cidPlaceholder, dataUri);
        }
        return htmlContent.replace(cidPlaceholder, "");
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @param additionalMailAddress An optional email address to override the user's email.
     * @return {@code true} if the email was enqueued, {@code false} if it was skipped (e.g. no
     *     valid email address or no reservations to include).
     * @throws IOException If an error occurs while sending the email.
     */
    public boolean sendReservationConfirmation(
            User user, List<Reservation> reservations, String additionalMailAddress)
            throws IOException {
        return sendReservationConfirmation(user, reservations, additionalMailAddress, true);
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @param additionalMailAddress An optional email address to override the user's email.
     * @param includeExistingReservations when {@code false}, the "already reserved" seat list is
     *     not computed from {@code user}'s other reservations for the event. Needed for the box
     *     office's shared guest account, where every walk-in reservation is stored under the same
     *     {@code User} row -- without this, one guest's confirmation would list every other guest's
     *     box-office seats for the same event as "already reserved".
     * @return {@code true} if the email was enqueued, {@code false} if it was skipped (e.g. no
     *     valid email address or no reservations to include).
     * @throws IOException If an error occurs while sending the email.
     */
    public boolean sendReservationConfirmation(
            User user,
            List<Reservation> reservations,
            String additionalMailAddress,
            boolean includeExistingReservations)
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
            return false;
        }

        LOG.debug(
                String.format(
                        "User ID: %s, Number of reservations: %d",
                        user.id, reservations != null ? reservations.size() : 0));

        if (reservations == null || reservations.isEmpty()) {
            LOG.warnf(
                    "No reservations provided for confirmation email to user %s.", user.getEmail());
            return false;
        }

        ReservationConfirmationContent content =
                renderReservationConfirmation(user, reservations, includeExistingReservations);

        enqueue(
                emailAddresses,
                EMAIL_HEADER_RESERVATION_CONFIRMATION,
                content.htmlContent(),
                buildImageAttachments(content.seatmapPng(), content.qrCodeImage()),
                true);
        return true;
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @return {@code true} if the email was enqueued, {@code false} if it was skipped (e.g. no
     *     valid email address or no reservations to include).
     * @throws IOException If an error occurs while sending the email.
     */
    public boolean sendReservationConfirmation(User user, List<Reservation> reservations)
            throws IOException {
        return sendReservationConfirmation(user, reservations, null);
    }

    /**
     * Renders the reservation confirmation email HTML with inline base64 image URIs for on-screen
     * or print display.
     *
     * @param user The user to whom the reservation belongs.
     * @param reservations The list of reservations for the user.
     * @return The rendered HTML content with embedded data URIs.
     */
    public String getReservationConfirmationDisplayContent(
            User user, List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return "";
        }

        ReservationConfirmationContent content =
                renderReservationConfirmation(user, reservations, true);

        String htmlContent =
                embedImageAsDataUri(
                        content.htmlContent(), "cid:seatmap-image", content.seatmapPng());
        htmlContent = embedImageAsDataUri(htmlContent, "cid:qrcode-image", content.qrCodeImage());

        return htmlContent;
    }

    public String getReservationConfirmationSubject() {
        return EMAIL_HEADER_RESERVATION_CONFIRMATION;
    }

    /**
     * The rendered box office confirmation, returned by {@link #sendBoxOfficeConfirmation} so the
     * caller can also offer a print copy -- {@code displayHtml} embeds the QR code as a {@code
     * data:} URI instead of the {@code cid:} reference {@code emailHtml} uses, since an API
     * response has no MIME attachment channel to resolve a content-id against.
     *
     * @param emailHtml the HTML queued for the email, referencing the QR via {@code
     *     cid:qrcode-image}
     * @param displayHtml the same content with the QR embedded as a {@code data:} URI, for
     *     on-screen/print display
     * @param qrCodeImage the QR code PNG bytes, or an empty array when no QR code was requested
     */
    public record BoxOfficeConfirmationContent(
            String emailHtml, String displayHtml, byte[] qrCodeImage) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BoxOfficeConfirmationContent that)) {
                return false;
            }
            return Objects.equals(emailHtml, that.emailHtml)
                    && Objects.equals(displayHtml, that.displayHtml)
                    && Arrays.equals(qrCodeImage, that.qrCodeImage);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(emailHtml, displayHtml) + Arrays.hashCode(qrCodeImage);
        }

        @Override
        public String toString() {
            return "BoxOfficeConfirmationContent{"
                    + "emailHtml='"
                    + emailHtml
                    + '\''
                    + ", displayHtml='"
                    + displayHtml
                    + '\''
                    + ", qrCodeImage="
                    + Arrays.toString(qrCodeImage)
                    + '}';
        }
    }

    /**
     * Renders the dedicated "box office" confirmation used for both known-user and walk-in guest
     * box office reservations, in place of {@link #sendReservationConfirmation}'s normal template
     * (which assumes an account with an interactive seatmap). Unlike the normal confirmation, this
     * template has no "already reserved"/seatmap-link section and optionally omits the QR code
     * entirely.
     *
     * @param recipientName the name to greet in the email (the target user's full name, or a
     *     walk-in guest's typed-in name)
     * @param event the event the reservations belong to
     * @param reservations the newly created box office reservations
     * @param includeQrCode whether to generate and embed a check-in QR code; {@code false} when the
     *     reservation was created already checked-in, since there is nothing left to scan
     */
    private BoxOfficeConfirmationContent renderBoxOfficeConfirmation(
            String recipientName,
            Event event,
            List<Reservation> reservations,
            boolean includeQrCode) {
        Map<UUID, Seat> seatById = loadSeatsForReservations(reservations);
        List<SeatView> seats = toSeatViews(reservations, seatById);
        String entranceInfo = generateEntranceInfo(reservations, seatById);

        byte[] qrCodeImage = new byte[0];
        String qrCodeDataUri = null;
        if (includeQrCode && reservations != null && !reservations.isEmpty()) {
            User qrOwner = reservations.getFirst().getUser();
            CheckInToken token = reservations.getFirst().getCheckInToken();
            String qrCodeContent = generateQrCodeContent(qrOwner, event, token);
            qrCodeImage = generateQrCodeImage(qrCodeContent);
            if (qrCodeImage.length > 0) {
                qrCodeDataUri =
                        "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeImage);
            }
        }
        boolean showQrCode = includeQrCode && qrCodeImage.length > 0;

        String emailHtml =
                boxOfficeConfirmationTemplate
                        .data("recipientName", recipientName)
                        .data("eventName", event.getName() != null ? event.getName() : "")
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("eventStartTime", formatDateTime(event.getStartTime()))
                        .data("eventEndTime", formatDateTime(event.getEndTime()))
                        .data("seats", seats)
                        .data("entranceInfo", entranceInfo)
                        .data("showQrCode", showQrCode)
                        .data("qrCodeImageSrc", "cid:qrcode-image")
                        .data("currentYear", currentYear())
                        .render();

        String displayHtml =
                boxOfficeConfirmationTemplate
                        .data("recipientName", recipientName)
                        .data("eventName", event.getName() != null ? event.getName() : "")
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("eventStartTime", formatDateTime(event.getStartTime()))
                        .data("eventEndTime", formatDateTime(event.getEndTime()))
                        .data("seats", seats)
                        .data("entranceInfo", entranceInfo)
                        .data("showQrCode", showQrCode)
                        .data("qrCodeImageSrc", qrCodeDataUri)
                        .data("currentYear", currentYear())
                        .render();

        return new BoxOfficeConfirmationContent(emailHtml, displayHtml, qrCodeImage);
    }

    /**
     * Sends the dedicated box office confirmation email and always returns the rendered content,
     * regardless of whether a valid recipient address was found, so the caller can offer a print
     * copy even when no email was sent.
     *
     * @param user the reservation owner (the target user for a known-user booking, or the shared
     *     {@code boxoffice} system user for a guest booking)
     * @param reservations the newly created box office reservations
     * @param recipientName the name to greet in the email
     * @param additionalMailAddress an optional email address to use in addition to (or instead of)
     *     {@code user}'s own address
     * @param includeQrCode whether to generate and embed a check-in QR code
     * @return the rendered confirmation content
     */
    public BoxOfficeConfirmationContent sendBoxOfficeConfirmation(
            User user,
            List<Reservation> reservations,
            String recipientName,
            String additionalMailAddress,
            boolean includeQrCode) {
        if (reservations == null || reservations.isEmpty()) {
            LOG.warn("No reservations provided for box office confirmation.");
            return new BoxOfficeConfirmationContent("", "", new byte[0]);
        }

        Event event = reservations.getFirst().getEvent();
        BoxOfficeConfirmationContent content =
                renderBoxOfficeConfirmation(recipientName, event, reservations, includeQrCode);

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
            LOG.warn("No valid email addresses provided for box office confirmation.");
            return content;
        }

        List<EmailAttachment> attachments =
                content.qrCodeImage().length > 0
                        ? List.of(
                                EmailAttachment.inline(
                                        "qrcode.png",
                                        "image/png",
                                        "qrcode-image",
                                        content.qrCodeImage()))
                        : List.of();

        enqueue(
                emailAddresses,
                EMAIL_HEADER_BOXOFFICE_CONFIRMATION,
                content.emailHtml(),
                attachments,
                true);

        return content;
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
                        "User ID: %s, Number of reservations: %d",
                        user.id, activeReservations != null ? activeReservations.size() : 0));
        Log.debug(
                String.format(
                        "User ID: %s, Number of deleted reservations: %d",
                        user.id, deletedReservations != null ? deletedReservations.size() : 0));

        if (deletedReservations == null || deletedReservations.isEmpty()) {
            LOG.warnf("No reservations deleted to user %s.", user.getEmail());
            return;
        }

        Event event = deletedReservations.getFirst().getEvent();
        String eventName = event.getName();
        LOG.debugf("Event for reservation confirmation: %s (ID: %s)", eventName, event.id);

        boolean hasActiveSeats = activeReservations != null && !activeReservations.isEmpty();

        // Create email seatmap token with active reservations if there are active seats
        String seatmapToken =
                hasActiveSeats
                        ? emailSeatMapService.createEmailSeatMapToken(
                                user, event, activeReservations)
                        : null;
        String seatmapLink = seatmapToken != null ? generateSeatmapLink(seatmapToken) : "";
        LOG.debugf("Created email seatmap token: %s", seatmapToken);

        // Get PNG image from EmailSeatMapService
        byte[] pngImage =
                seatmapToken != null
                        ? emailSeatMapService.getPngImage(seatmapToken).orElse(new byte[0])
                        : new byte[0];
        LOG.debugf("Retrieved PNG image with size: %d bytes", pngImage.length);

        // Prepare data for seat list rendering
        LOG.debugf(
                "Retrieved %d user reservations for event %s.",
                hasActiveSeats ? activeReservations.size() : 0, eventName);

        Map<UUID, Seat> seatById =
                loadSeatsForReservations(deletedReservations, activeReservations);

        List<SeatView> deletedSeats = toSeatViews(deletedReservations, seatById);
        List<SeatView> activeSeats =
                hasActiveSeats ? toSeatViews(activeReservations, seatById) : List.of();

        String entranceInfo =
                hasActiveSeats ? generateEntranceInfo(activeReservations, seatById) : "";

        String htmlContent =
                reservationUpdateTemplate
                        .data("userName", user.getUsername())
                        .data("fullName", fullName(user))
                        .data("eventName", eventName != null ? eventName : "")
                        .data("eventLocation", event.getEventLocation().getName())
                        .data("eventStartTime", formatDateTime(event.getStartTime()))
                        .data("eventEndTime", formatDateTime(event.getEndTime()))
                        .data("deletedSeats", deletedSeats)
                        .data("hasActiveSeats", hasActiveSeats)
                        .data("activeSeats", activeSeats)
                        .data("entranceInfo", entranceInfo)
                        .data("eventLink", generateEventLink(event.id))
                        .data("seatmapLink", seatmapLink)
                        .data("currentYear", currentYear())
                        .render();

        byte[] qrCodeImage = new byte[0];
        if (hasActiveSeats) {
            CheckInToken token = activeReservations.getFirst().getCheckInToken();
            String qrCodeContent = generateQrCodeContent(user, event, token);
            qrCodeImage = generateQrCodeImage(qrCodeContent);
        }

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

        LOG.debugf("User ID: %s, Username: %s", user.id, user.getUsername());

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
                        "User ID: %s, Event ID: %s, Number of reservations: %d",
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

        Map<UUID, Seat> seatById = loadSeatsForReservations(reservations);
        List<SeatView> seats = toSeatViews(reservations, seatById);
        String entranceInfo = generateEntranceInfo(reservations, seatById);

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

        CheckInToken token =
                reservations != null && !reservations.isEmpty()
                        ? reservations.getFirst().getCheckInToken()
                        : null;
        String qrCodeContent = generateQrCodeContent(user, event, token);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        enqueue(
                List.of(user.getEmail()),
                EMAIL_HEADER_REMINDER,
                htmlContent,
                buildImageAttachments(pngImage, qrCodeImage),
                false,
                EmailPriority.BULK);
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
        LOG.debugf("Manager ID: %s, Event ID: %s", manager.id, event.id);

        // Generate CSV data
        byte[] csvData = reservationService.exportReservationsToCsv(event.id, manager);
        LOG.debugf(
                "Generated CSV data of size %d bytes for event ID: %s", csvData.length, event.id);

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
                false,
                EmailPriority.BULK);
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
     * Builds an {@link EmailMessage} from the given content and hands it off to the email queue
     * with default {@link EmailPriority#TRANSACTIONAL} priority.
     */
    private void enqueue(
            List<String> recipients,
            String subject,
            String htmlContent,
            List<EmailAttachment> attachments,
            boolean includeBcc) {
        enqueue(
                recipients,
                subject,
                htmlContent,
                attachments,
                includeBcc,
                EmailPriority.TRANSACTIONAL);
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
     * @param priority the priority of the email
     */
    private void enqueue(
            List<String> recipients,
            String subject,
            String htmlContent,
            List<EmailAttachment> attachments,
            boolean includeBcc,
            EmailPriority priority) {
        EmailMessage.Builder builder =
                EmailMessage.builder().subject(subject).htmlBody(htmlContent).priority(priority);

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
     * @param seatById the pre-loaded seats referenced by the reservations, keyed by seat ID
     * @return a formatted text describing which entrance to use for which seats, or an empty string
     *     if no valid entrance information is available
     */
    private String generateEntranceInfo(List<Reservation> reservations, Map<UUID, Seat> seatById) {
        if (reservations == null || reservations.isEmpty()) {
            return "";
        }

        // Group seats by entrance
        var seatsByEntrance =
                reservations.stream()
                        .map(r -> seatById.get(r.getSeat().getId()))
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
     * @param token The check-in token.
     * @return The formatted QR code content string.
     */
    private String generateQrCodeContent(User user, Event event, CheckInToken token) {
        if (token == null || token.getToken() == null) {
            return "";
        }
        return user.id.toString() + ";" + event.id.toString() + ";" + token.getToken();
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
