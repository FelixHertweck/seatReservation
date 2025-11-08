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

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.VerificationCodeGenerator;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
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

    @ConfigProperty(name = "email.header.reservation-update", defaultValue = "Reservation Update")
    String EMAIL_HEADER_RESERVATION_UPDATE;

    @ConfigProperty(name = "email.header.reminder", defaultValue = "Reservation Reminder")
    String EMAIL_HEADER_REMINDER;

    @ConfigProperty(
            name = "email.header.reservation-overview",
            defaultValue = "Reservation Overview")
    String EMAIL_HEADER_RESERVATION_OVERVIEW;

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject ReactiveMailer mailer;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject SeatRepository seatRepository;

    @Inject ReservationService reservationService;

    @Inject EmailSeatMapService emailSeatMapService;

    @ConfigProperty(name = "email.frontend-base-url", defaultValue = "")
    String frontendBaseUrl;

    @ConfigProperty(name = "email.verification.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    @ConfigProperty(name = "email.content.email-confirmation")
    String emailContentEmailConfirmation;

    @ConfigProperty(name = "email.bcc-address")
    Optional<String> bccAddress;

    @ConfigProperty(name = "email.content.event-reminder")
    String emailContentEventReminder;

    @ConfigProperty(name = "email.content.password-changed")
    String emailContentPasswordChanged;

    @ConfigProperty(name = "email.content.reservation-confirmation")
    String emailContentReservationConfirmation;

    @ConfigProperty(name = "email.content.reservation-update-confirmation")
    String emailContentReservationUpdateConfirmation;

    @ConfigProperty(name = "email.entrance-info-template")
    String entranceInfoTemplate;

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

        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        String htmlContent = emailContentEmailConfirmation;

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{verificationCode}", emailVerification.getToken());
        htmlContent =
                htmlContent.replace(
                        "{verificationLink}",
                        generateVerificationLink(emailVerification.getToken()));
        htmlContent =
                htmlContent.replace(
                        "{expirationTime}",
                        emailVerification
                                .getExpirationTime()
                                .atZone(ZoneId.systemDefault())
                                .format(formatter));
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in email template.");

        // Create and send the email
        LOG.debugf("Email confirmation subject: %s", EMAIL_HEADER_CONFIRMATION);
        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_CONFIRMATION, htmlContent);

        mailer.send(mail)
                .subscribe()
                .with(
                        success ->
                                LOG.infof(
                                        "Email confirmation sent successfully to %s for user ID:"
                                                + " %d",
                                        user.getEmail(), user.id),
                        failure ->
                                LOG.errorf(
                                        failure,
                                        "Failed to send email confirmation to %s for user ID: %d",
                                        user.getEmail(),
                                        user.id));
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

        StringBuilder seatListHtml = new StringBuilder();
        for (Reservation reservation : reservations) {
            seatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
                    .append(" (")
                    .append(reservation.getSeat().getSeatRow())
                    .append(")")
                    .append("</li>");
        }
        LOG.debugf("HTML list of seats generated: %s", seatListHtml.toString());

        String htmlContent = emailContentReservationConfirmation;

        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        htmlContent = htmlContent.replace("{eventName}", eventName != null ? eventName : "");
        htmlContent = htmlContent.replace("{eventLocation}", event.getEventLocation().getName());
        htmlContent =
                htmlContent.replace(
                        "{eventStartTime}",
                        event.getStartTime().atZone(ZoneId.systemDefault()).format(formatter));
        htmlContent =
                htmlContent.replace(
                        "{eventEndTime}",
                        event.getEndTime().atZone(ZoneId.systemDefault()).format(formatter));
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{eventLink}", generateEventLink(event.id));
        htmlContent = htmlContent.replace("{seatmapLink}", seatmapLink);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

        // Add entrance information
        String entranceInfo = generateEntranceInfo(reservations);
        htmlContent = htmlContent.replace("{entranceInfo}", entranceInfo);
        LOG.debugf("Entrance information added: %s", entranceInfo);

        // Show or hide existing reservations section based on presence of existing seats
        if (existingSeatNumbers.isEmpty()) {
            htmlContent = htmlContent.replace("{existingHeaderVisible}", "hidden");
            htmlContent = htmlContent.replace("{existingSeatList}", "");
        } else {
            htmlContent = htmlContent.replace("{existingHeaderVisible}", "visible");
            StringBuilder existingSeatListHtml = new StringBuilder();
            for (Seat seat : existingSeatNumbers) {
                existingSeatListHtml
                        .append("<li>")
                        .append(seat.getSeatNumber())
                        .append(" (")
                        .append(seat.getSeatRow())
                        .append(")")
                        .append("</li>");
            }
            htmlContent =
                    htmlContent.replace("{existingSeatList}", existingSeatListHtml.toString());
        }

        LOG.debug("Placeholders replaced in reservation email template.");

        Mail mail =
                Mail.withHtml(
                        emailAddresses.getFirst(),
                        EMAIL_HEADER_RESERVATION_CONFIRMATION,
                        htmlContent);

        // Add PNG image as inline attachment
        if (pngImage.length > 0) {
            mail.addInlineAttachment("seatmap.png", pngImage, "image/png", "seatmap-image");
        }

        if (emailAddresses.size() > 1) {
            emailAddresses.subList(1, emailAddresses.size()).forEach(mail::addCc);
        }
        addBcc(mail);
        mailer.send(mail)
                .subscribe()
                .with(
                        success ->
                                LOG.infof(
                                        "Reservation confirmation email sent successfully to %s for"
                                                + " user ID: %d",
                                        user.getEmail(), user.id),
                        failure ->
                                LOG.errorf(
                                        failure,
                                        "Failed to send reservation confirmation to %s for user ID:"
                                                + " %d",
                                        user.getEmail(),
                                        user.id));
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

        Set<String> existingSeatNumbers =
                activeReservations != null
                        ? activeReservations.stream()
                                .map(r -> r.getSeat().getSeatNumber())
                                .collect(Collectors.toSet())
                        : Set.of();
        LOG.debugf("Existing seat numbers (excluding new ones): %s", existingSeatNumbers);

        StringBuilder deletedSeatListHtml = new StringBuilder();
        for (Reservation reservation : deletedReservations) {
            deletedSeatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
                    .append(" (")
                    .append(reservation.getSeat().getSeatRow())
                    .append(")")
                    .append("</li>");
        }
        LOG.debugf("HTML list of seats generated: %s", deletedSeatListHtml.toString());

        String htmlContent = emailContentReservationUpdateConfirmation;

        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        htmlContent = htmlContent.replace("{eventName}", eventName != null ? eventName : "");
        htmlContent = htmlContent.replace("{eventLocation}", event.getEventLocation().getName());
        htmlContent =
                htmlContent.replace(
                        "{eventStartTime}",
                        event.getStartTime().atZone(ZoneId.systemDefault()).format(formatter));
        htmlContent =
                htmlContent.replace(
                        "{eventEndTime}",
                        event.getEndTime().atZone(ZoneId.systemDefault()).format(formatter));

        htmlContent = htmlContent.replace("{deletedSeatList}", deletedSeatListHtml.toString());
        htmlContent = htmlContent.replace("{eventLink}", generateEventLink(event.id));
        htmlContent = htmlContent.replace("{seatmapLink}", seatmapLink);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

        // Add entrance information for active reservations
        String entranceInfo = generateEntranceInfo(activeReservations);
        htmlContent = htmlContent.replace("{entranceInfo}", entranceInfo);
        LOG.debugf("Entrance information added: %s", entranceInfo);

        if (activeReservations == null || activeReservations.isEmpty()) {
            htmlContent = htmlContent.replace("{existingHeaderVisible}", "hidden");
            htmlContent = htmlContent.replace("{activeSeatList}", "");

        } else {
            htmlContent = htmlContent.replace("{existingHeaderVisible}", "visible");
            StringBuilder activeSeatListHtml = new StringBuilder();
            for (Reservation reservation : activeReservations) {
                activeSeatListHtml
                        .append("<li>")
                        .append(reservation.getSeat().getSeatNumber())
                        .append(" (")
                        .append(reservation.getSeat().getSeatRow())
                        .append(" ")
                        .append(")")
                        .append("</li>");
            }
            LOG.debugf("HTML list of seats generated: %s", activeSeatListHtml.toString());
            htmlContent = htmlContent.replace("{activeSeatList}", activeSeatListHtml.toString());
        }

        LOG.debug("Placeholders replaced in reservation email template.");

        Mail mail =
                Mail.withHtml(
                        emailAddresses.getFirst(), EMAIL_HEADER_RESERVATION_UPDATE, htmlContent);

        // Add PNG image as inline attachment
        if (pngImage.length > 0) {
            mail.addInlineAttachment("seatmap.png", pngImage, "image/png", "seatmap-image");
        }

        if (emailAddresses.size() > 1) {
            emailAddresses.subList(1, emailAddresses.size()).forEach(mail::addCc);
        }
        addBcc(mail);
        mailer.send(mail)
                .subscribe()
                .with(
                        success ->
                                LOG.infof(
                                        "Reservation update confirmation email sent successfully to"
                                                + " %s for user ID: %d",
                                        user.getEmail(), user.id),
                        failure ->
                                LOG.errorf(
                                        failure,
                                        "Failed to send reservation update confirmation to %s for"
                                                + " user ID: %d",
                                        user.getEmail(),
                                        user.id));
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

        String htmlContent = emailContentPasswordChanged;

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in password changed email template.");

        // Create and send the email
        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_PASSWORD_CHANGED, htmlContent);

        mailer.send(mail)
                .subscribe()
                .with(
                        success ->
                                LOG.infof(
                                        "Password changed notification sent successfully to %s for"
                                                + " user ID: %d",
                                        user.getEmail(), user.id),
                        failure ->
                                LOG.errorf(
                                        failure,
                                        "Failed to send password changed notification to %s for"
                                                + " user ID: %d",
                                        user.getEmail(),
                                        user.id));
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

        List<Seat> reservedSeats =
                reservations.stream().map(Reservation::getSeat).collect(Collectors.toList());

        String htmlContent = emailContentEventReminder;

        // Prepare seat list HTML
        StringBuilder seatListHtml = new StringBuilder();
        for (Seat seat : reservedSeats) {
            seatListHtml
                    .append("<li>")
                    .append(seat.getSeatNumber())
                    .append(" (")
                    .append(seat.getSeatRow())
                    .append(")")
                    .append("</li>");
        }
        LOG.debugf("HTML list of seats generated: %s", seatListHtml.toString());

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{eventName}", event.getName());
        htmlContent =
                htmlContent.replace(
                        "{eventDate}",
                        event.getStartTime()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString());
        htmlContent =
                htmlContent.replace(
                        "{eventTime}",
                        event.getStartTime()
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .toString());
        htmlContent = htmlContent.replace("{eventLocation}", event.getEventLocation().getName());
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{seatmapLink}", seatmapLink);
        htmlContent = htmlContent.replace("{eventLink}", generateEventLink(event.id));
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

        // Add entrance information
        String entranceInfo = generateEntranceInfo(reservations);
        htmlContent = htmlContent.replace("{entranceInfo}", entranceInfo);
        LOG.debugf("Entrance information added: %s", entranceInfo);

        LOG.debug("Placeholders replaced in event reminder email template.");

        // Create and send the email
        LOG.debugf("Event reminder subject: %s", EMAIL_HEADER_REMINDER);
        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_REMINDER, htmlContent);

        // Add PNG image as inline attachment
        if (pngImage.length > 0) {
            mail.addInlineAttachment("seatmap.png", pngImage, "image/png", "seatmap-image");
        }

        mailer.send(mail)
                .subscribe()
                .with(
                        success ->
                                LOG.infof(
                                        "Event reminder sent successfully to %s for user ID: %d,"
                                                + " event ID: %d",
                                        user.getEmail(), user.id, event.id),
                        failure ->
                                LOG.errorf(
                                        failure,
                                        "Failed to send event reminder to %s for user ID: %d, event"
                                                + " ID: %d",
                                        user.getEmail(),
                                        user.id,
                                        event.id));
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

        String htmlContent = emailContentEventReminder;

        // Replace placeholders
        htmlContent =
                htmlContent.replace(
                        "{userName}", manager.getFirstname() + " " + manager.getLastname());
        htmlContent = htmlContent.replace("{eventName}", event.getName());
        htmlContent =
                htmlContent.replace(
                        "{eventDate}",
                        event.getStartTime()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString());
        htmlContent =
                htmlContent.replace(
                        "{eventTime}",
                        event.getStartTime()
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .toString());
        htmlContent = htmlContent.replace("{eventLocation}", event.getEventLocation().getName());
        htmlContent =
                htmlContent.replace(
                        "{seatList}",
                        "Bitte finden Sie die Reservierungsdetails im Anhang."); // Placeholder for
        // seat list
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in CSV export email template.");

        // Create and send the email with CSV attachment
        Mail mail =
                Mail.withHtml(
                        manager.getEmail(),
                        EMAIL_HEADER_RESERVATION_OVERVIEW + event.getName(),
                        htmlContent);
        mail.addAttachment("reservations_" + event.id + ".csv", csvData, "text/csv");

        mailer.send(mail)
                .subscribe()
                .with(
                        success ->
                                LOG.infof(
                                        "Reservation CSV export email sent successfully to %s for"
                                                + " manager ID: %d, event ID: %d",
                                        manager.getEmail(), manager.id, event.id),
                        failure ->
                                LOG.errorf(
                                        failure,
                                        "Failed to send reservation CSV export email to %s for"
                                                + " manager ID: %d, event ID: %d",
                                        manager.getEmail(),
                                        manager.id,
                                        event.id));
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
     * Adds a Bcc (blind carbon copy) address to the specified mail if the address is present,
     * non-empty, and not already included in the email recipients.
     *
     * @param mail the Mail object to which the Bcc address will be added
     */
    private void addBcc(Mail mail) {
        bccAddress.ifPresent(
                address -> {
                    if (!address.trim().isEmpty()
                            && !mail.getTo().contains(address)
                            && !mail.getCc().contains(address)) {
                        mail.addBcc(address);
                    }
                });
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
                                                && !seat.getEntrance().trim().isEmpty())
                        .collect(
                                Collectors.groupingBy(
                                        Seat::getEntrance,
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
}
