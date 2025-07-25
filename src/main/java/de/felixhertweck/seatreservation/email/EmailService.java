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

    @ConfigProperty(name = "email.confirmation.base.url", defaultValue = "")
    String baseUrl;

    @ConfigProperty(name = "email.confirmation.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    /**
     * Sends an email confirmation to the specified user.
     *
     * @param user the user to whom the confirmation email will be sent
     * @throws IOException if the email template cannot be read
     */
    public void sendEmailConfirmation(User user) throws IOException {
        LOG.infof("Attempting to send email confirmation to user: %s", user.getEmail());
        LOG.debugf("User ID: %d, Username: %s", user.id, user.getUsername());

        String confirmationLink = createConfirmationLink(user);
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

        mailer.send(mail);
        LOG.infof(
                "Email confirmation sent successfully to %s for user ID: %d",
                user.getEmail(), user.id);
    }

    private String createConfirmationLink(User user) {
        LOG.debugf("Creating confirmation link for user ID: %d", user.id);
        String token = RandomUUIDString.generate();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        EmailVerification emailVerification = new EmailVerification(user, token, expirationTime);
        LOG.debugf("Generated token: %s, Expiration time: %s", token, expirationTime);

        // persist and get id of entry with emailVerificationRepository
        emailVerificationRepository.persist(emailVerification);
        LOG.infof(
                "Email verification entry persisted for user ID %d with ID: %d",
                user.id, emailVerification.id);

        return baseUrl
                + "/api/user/confirm-email"
                + "?id="
                + emailVerification.id
                + "&token="
                + token;
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
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{seatMap}", svgContent);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());
        LOG.debug("Placeholders replaced in reservation email template.");

        Mail mail = Mail.withHtml(user.getEmail(), "Ihre Reservierungsbestätigung", htmlContent);
        mailer.send(mail);
        LOG.infof(
                "Reservation confirmation email sent successfully to %s for user ID: %d",
                user.getEmail(), user.id);
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

        mailer.send(mail);
        LOG.infof(
                "Password changed notification sent successfully to %s for user ID: %d",
                user.getEmail(), user.id);
    }
}
