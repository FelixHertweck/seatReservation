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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.*;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.reservation.service.ReservationService;
import de.felixhertweck.seatreservation.utils.RandomUUIDString;
import de.felixhertweck.seatreservation.utils.SvgRenderer;
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

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject Mailer mailer;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject SeatRepository seatRepository;

    @Inject ReservationService reservationService;

    @ConfigProperty(name = "email.confirmation.base.url", defaultValue = "")
    String baseUrl;

    @ConfigProperty(name = "email.confirmation.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    /**
     * Sends an email confirmation to the specified user.
     *
     * @param user the user to whom the confirmation email will be sent
     * @param emailVerification the EmailVerification object to use for the link
     * @throws IOException if the email template cannot be read
     */
    public void sendEmailConfirmation(User user, EmailVerification emailVerification)
            throws IOException {
        LOG.infof("Attempting to send email confirmation to user: %s", user.getEmail());
        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        String confirmationLink =
                generateConfirmationLink(emailVerification.id, emailVerification.getToken());
        LOG.debugf("Confirmation link generated: %s", confirmationLink);

        // Read the HTML template
        String templatePath = "src/main/resources/templates/email/email-confirmation.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));
        LOG.debugf("Email confirmation template read from: %s", templatePath);

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{confirmationLink}", confirmationLink);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in email template.");

        // Create and send the email
        Mail mail =
                Mail.withHtml(user.getEmail(), "Please Confirm Your Email Address", htmlContent);

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
        String token = RandomUUIDString.generate();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        EmailVerification emailVerification = new EmailVerification(user, token, expirationTime);
        emailVerificationRepository.persist(emailVerification);
        LOG.infof(
                "Email verification entry persisted for user ID %d with ID: %d",
                user.id, emailVerification.id);
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

    private String generateConfirmationLink(Long id, String token) {
        return baseUrl + "/api/user/confirm-email" + "?id=" + id + "&token=" + token;
    }

    public void sendReservationConfirmation(User user, List<Reservation> reservations)
            throws IOException {
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

        Set<String> newSeatNumbers =
                reservations.stream()
                        .map(r -> r.getSeat().getSeatNumber())
                        .collect(Collectors.toSet());
        LOG.debugf("New seat numbers for confirmation: %s", newSeatNumbers);

        Set<String> existingSeatNumbers =
                allUserReservationsForEvent.stream()
                        .map(r -> r.getSeat().getSeatNumber())
                        .collect(Collectors.toSet());
        existingSeatNumbers.removeAll(newSeatNumbers); // Keep only previously reserved seats
        LOG.debugf("Existing seat numbers (excluding new ones): %s", existingSeatNumbers);

        String svgContent = SvgRenderer.renderSeats(allSeats, newSeatNumbers, existingSeatNumbers);
        LOG.debug("SVG content for seat map generated.");

        StringBuilder seatListHtml = new StringBuilder();
        for (Reservation reservation : reservations) {
            seatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
                    .append("</li>");
        }
        LOG.debugf("HTML list of seats generated: %s", seatListHtml.toString());

        String templatePath = "src/main/resources/templates/email/reservation-confirmation.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));
        LOG.debugf("Reservation confirmation template read from: %s", templatePath);

        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{eventName}", eventName != null ? eventName : "");
        htmlContent = htmlContent.replace("{eventLocation}", event.getEventLocation().getName());
        htmlContent = htmlContent.replace("{eventStartTime}", event.getStartTime().toString());
        htmlContent = htmlContent.replace("{eventEndTime}", event.getEndTime().toString());
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{seatMap}", svgContent);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in reservation email template.");

        Mail mail = Mail.withHtml(user.getEmail(), "Ihre Reservierungsbestätigung", htmlContent);
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
     * Sends a password changed notification email to the specified user.
     *
     * @param user the user to whom the password changed email will be sent
     * @throws IOException if the email template cannot be read
     */
    public void sendPasswordChangedNotification(User user) throws IOException {
        LOG.infof("Attempting to send password changed notification to user: %s", user.getEmail());
        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        // Read the HTML template
        String templatePath = "src/main/resources/templates/email/password-changed.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));
        LOG.debugf("Password changed notification template read from: %s", templatePath);

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in password changed email template.");

        // Create and send the email
        Mail mail = Mail.withHtml(user.getEmail(), "Ihr Passwort wurde geändert", htmlContent);

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
     * @throws IOException if the email template cannot be read
     */
    public void sendEventReminder(User user, Event event, List<Reservation> reservations)
            throws IOException {
        LOG.infof(
                "Attempting to send event reminder to user: %s for event: %s",
                user.getEmail(), event.getName());
        LOG.debugf(
                String.format(
                        "User ID: %d, Event ID: %d, Number of reservations: %d",
                        user.id, event.id, reservations.size()));

        // Read the HTML template
        String templatePath = "src/main/resources/templates/email/event-reminder.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));
        LOG.debugf("Event reminder template read from: %s", templatePath);

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
        Mail mail =
                Mail.withHtml(user.getEmail(), "Erinnerung: Ihr Event beginnt bald!", htmlContent);

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
        LOG.infof(
                "Attempting to send reservation CSV export to manager: %s for event: %s",
                manager.getEmail(), event.getName());
        LOG.debugf("Manager ID: %d, Event ID: %d", manager.id, event.id);

        // Generate CSV data
        byte[] csvData = reservationService.exportReservationsToCsv(event.id, manager);
        LOG.debugf(
                "Generated CSV data of size %d bytes for event ID: %d", csvData.length, event.id);

        // Read the HTML template (can be a generic one or a specific one for CSV export)
        String templatePath =
                "src/main/resources/templates/email/event-reminder.html"; // Reusing for simplicity,
        // can create a new one
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));
        LOG.debugf("Email template for CSV export read from: %s", templatePath);

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
                        "Reservierungsübersicht für Ihr Event: " + event.getName(),
                        htmlContent);
        mail.addAttachment(
                "reservations_" + event.id + ".csv",
                csvData,
                "text/csv"); // Korrigierte Reihenfolge der Argumente

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
}
