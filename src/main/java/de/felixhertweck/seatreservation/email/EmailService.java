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
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.SvgRenderer;
import de.felixhertweck.seatreservation.utils.VerificationCodeGenerator;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
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

    @Inject Mailer mailer;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject SeatRepository seatRepository;

    @Inject ReservationService reservationService;

    @ConfigProperty(name = "email.backend-base-url", defaultValue = "")
    String backendBaseUrl;

    @ConfigProperty(name = "email.frontend-base-url", defaultValue = "")
    String frontendBaseUrl;

    @ConfigProperty(name = "email.verification.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    @ConfigProperty(name = "email.content.email-confirmation")
    String emailContentEmailConfirmation;

    @ConfigProperty(name = "email.content.event-reminder")
    String emailContentEventReminder;

    @ConfigProperty(name = "email.content.password-changed")
    String emailContentPasswordChanged;

    @ConfigProperty(name = "email.content.reservation-confirmation")
    String emailContentReservationConfirmation;

    @ConfigProperty(name = "email.content.reservation-update-confirmation")
    String emailContentReservationUpdateConfirmation;

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
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            return;
        }

        LOG.infof("Attempting to send email confirmation to user: %s", user.getEmail());
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
                        emailVerification.getExpirationTime().format(formatter));
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in email template.");

        // Create and send the email
        LOG.debugf("Email confirmation subject: %s", EMAIL_HEADER_CONFIRMATION);
        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_CONFIRMATION, htmlContent);

        try {
            mailer.send(mail);
            LOG.infof(
                    "Email confirmation sent successfully to %s for user ID: %d",
                    user.getEmail(), user.id);
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to send email confirmation to %s for user ID: %d",
                    user.getEmail(),
                    user.id);
        }
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
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        EmailVerification emailVerification =
                new EmailVerification(user, verificationCode, expirationTime);
        emailVerificationRepository.persist(emailVerification);
        LOG.infof(
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
        emailVerification.setExpirationTime(LocalDateTime.now().plusMinutes(expirationMinutes));
        emailVerificationRepository.persist(emailVerification);
        LOG.infof(
                "Email verification entry ID %d expiration time updated to: %s",
                emailVerification.id, emailVerification.getExpirationTime());
        return emailVerification;
    }

    private String generateEventLink(Long eventId) {
        return frontendBaseUrl.trim() + "/events?id=" + eventId;
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
     * @throws IOException If an error occurs while sending the email.
     */
    public void sendReservationConfirmation(User user, List<Reservation> reservations)
            throws IOException {
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            return;
        }

        LOG.infof("Attempting to send reservation confirmation to user: %s", user.getEmail());
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

        // Prepare data for SVG rendering
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

        String svgContent =
                SvgRenderer.renderSeats(
                        allSeats,
                        newSeatNumbers.stream()
                                .map(Seat::getSeatNumber)
                                .collect(Collectors.toSet()),
                        existingSeatNumbers.stream()
                                .map(Seat::getSeatNumber)
                                .collect(Collectors.toSet()),
                        event.getEventLocation().getMarkers());
        LOG.debug("SVG content for seat map generated.");

        StringBuilder seatListHtml = new StringBuilder();
        for (Reservation reservation : reservations) {
            seatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
                    .append("(")
                    .append(reservation.getSeat().getSeatRow())
                    .append(" )")
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
                htmlContent.replace("{eventStartTime}", event.getStartTime().format(formatter));
        htmlContent = htmlContent.replace("{eventEndTime}", event.getEndTime().format(formatter));
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{eventLink}", generateEventLink(event.id));
        htmlContent = htmlContent.replace("{seatMap}", svgContent);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

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
                        .append("(Reihe: ")
                        .append(seat.getSeatRow())
                        .append(" )")
                        .append("</li>");
            }
            htmlContent =
                    htmlContent.replace("{existingSeatList}", existingSeatListHtml.toString());
        }

        LOG.debug("Placeholders replaced in reservation email template.");

        Mail mail =
                Mail.withHtml(user.getEmail(), EMAIL_HEADER_RESERVATION_CONFIRMATION, htmlContent);
        try {
            mailer.send(mail);
            LOG.infof(
                    "Reservation confirmation email sent successfully to %s for user ID: %d",
                    user.getEmail(), user.id);
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to send reservation confirmation to %s for user ID: %d",
                    user.getEmail(),
                    user.id);
        }
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
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            return;
        }

        LOG.infof(
                "Attempting to send update reservation confirmation to user: %s", user.getEmail());
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

        // Prepare data for SVG rendering
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

        String svgContent =
                SvgRenderer.renderSeats(
                        allSeats,
                        Set.of(),
                        existingSeatNumbers,
                        event.getEventLocation().getMarkers());
        LOG.debug("SVG content for seat map generated.");

        StringBuilder deletedSeatListHtml = new StringBuilder();
        for (Reservation reservation : deletedReservations) {
            deletedSeatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
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
                htmlContent.replace("{eventStartTime}", event.getStartTime().format(formatter));
        htmlContent = htmlContent.replace("{eventEndTime}", event.getEndTime().format(formatter));

        htmlContent = htmlContent.replace("{deletedSeatList}", deletedSeatListHtml.toString());
        htmlContent = htmlContent.replace("{eventLink}", generateEventLink(event.id));
        htmlContent = htmlContent.replace("{seatMap}", svgContent);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

        if (activeReservations.isEmpty()) {
            htmlContent = htmlContent.replace("{existingHeaderVisible}", "hidden");
            htmlContent = htmlContent.replace("{activeSeatList}", "");

        } else {
            htmlContent = htmlContent.replace("{existingHeaderVisible}", "visible");
            StringBuilder activeSeatListHtml = new StringBuilder();
            for (Reservation reservation : activeReservations) {
                activeSeatListHtml
                        .append("<li>")
                        .append(reservation.getSeat().getSeatNumber())
                        .append("</li>");
            }
            LOG.debugf("HTML list of seats generated: %s", activeSeatListHtml.toString());
            htmlContent = htmlContent.replace("{activeSeatList}", activeSeatListHtml.toString());
        }

        LOG.debug("Placeholders replaced in reservation email template.");

        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_RESERVATION_UPDATE, htmlContent);
        try {
            mailer.send(mail);
            LOG.infof(
                    "Reservation update confirmation email sent successfully to %s for user ID: %d",
                    user.getEmail(), user.id);
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to send reservation update confirmation to %s for user ID: %d",
                    user.getEmail(),
                    user.id);
        }
    }

    /**
     * Sends a password changed notification email to the specified user.
     *
     * @param user the user to whom the password changed email will be sent
     * @throws IOException if the email template cannot be read
     */
    public void sendPasswordChangedNotification(User user) throws IOException {
        if (skipForNullOrEmptyAddress(user.getEmail())) {
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            return;
        }

        LOG.infof("Attempting to send password changed notification to user: %s", user.getEmail());
        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        String htmlContent = emailContentPasswordChanged;

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in password changed email template.");

        // Create and send the email
        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_PASSWORD_CHANGED, htmlContent);

        try {
            mailer.send(mail);
            LOG.infof(
                    "Password changed notification sent successfully to %s for user ID: %d",
                    user.getEmail(), user.id);
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to send password changed notification to %s for user ID: %d",
                    user.getEmail(),
                    user.id);
        }
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
            return;
        }
        if (skipForLocalhostAddress(user.getEmail())) {
            return;
        }

        LOG.infof(
                "Attempting to send event reminder to user: %s for event: %s",
                user.getEmail(), event.getName());
        LOG.debugf(
                String.format(
                        "User ID: %d, Event ID: %d, Number of reservations: %d",
                        user.id, event.id, reservations.size()));

        String htmlContent = emailContentEventReminder;

        // Prepare seat list HTML
        StringBuilder seatListHtml = new StringBuilder();
        for (Reservation reservation : reservations) {
            seatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
                    .append("</li>");
        }
        LOG.debugf("HTML list of seats generated: %s", seatListHtml.toString());

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{eventName}", event.getName());
        htmlContent =
                htmlContent.replace("{eventDate}", event.getStartTime().toLocalDate().toString());
        htmlContent =
                htmlContent.replace("{eventTime}", event.getStartTime().toLocalTime().toString());
        htmlContent = htmlContent.replace("{eventLocation}", event.getEventLocation().getName());
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in event reminder email template.");

        // Create and send the email
        LOG.debugf("Event reminder subject: %s", EMAIL_HEADER_REMINDER);
        Mail mail = Mail.withHtml(user.getEmail(), EMAIL_HEADER_REMINDER, htmlContent);

        try {
            mailer.send(mail);
            LOG.infof(
                    "Event reminder sent successfully to %s for user ID: %d, event ID: %d",
                    user.getEmail(), user.id, event.id);
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to send event reminder to %s for user ID: %d, event ID: %d",
                    user.getEmail(),
                    user.id,
                    event.id);
        }
    }

    /**
     * Sends an email to the event manager with a CSV export of all reservations for a given event.
     *
     * @param manager the manager of the event
     * @param event the event for which the reservations are to be exported
     * @throws IOException if the email template cannot be read or CSV export fails
     */
    public void sendEventReservationsCsvToManager(User manager, Event event) throws IOException {
        if (skipForNullOrEmptyAddress(manager.getEmail())) {
            return;
        }
        if (skipForLocalhostAddress(manager.getEmail())) {
            return;
        }
        LOG.infof(
                "Attempting to send reservation CSV export to manager: %s for event: %s",
                manager.getEmail(), event.getName());
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
                htmlContent.replace("{eventDate}", event.getStartTime().toLocalDate().toString());
        htmlContent =
                htmlContent.replace("{eventTime}", event.getStartTime().toLocalTime().toString());
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

        try {
            mailer.send(mail);
            LOG.infof(
                    "Reservation CSV export email sent successfully to %s for manager ID: %d, event"
                            + " ID: %d",
                    manager.getEmail(), manager.id, event.id);
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to send reservation CSV export email to %s for manager ID: %d, event"
                            + " ID: %d",
                    manager.getEmail(),
                    manager.id,
                    event.id);
        }
    }

    private boolean skipForNullOrEmptyAddress(String address) {
        if (address == null || address.isEmpty()) {
            Log.warn("Skipping email sending for null or empty address.");
            return true;
        }
        return false;
    }

    private boolean skipForLocalhostAddress(String address) {
        if (address.endsWith("@localhost")) {
            LOG.infof("Skipping email sending for localhost address: %s", address);
            return true;
        }
        return false;
    }
}
