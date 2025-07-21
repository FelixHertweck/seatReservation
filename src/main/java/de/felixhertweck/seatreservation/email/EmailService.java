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
        String confirmationLink = createConfirmationLink(user);

        // Read the HTML template
        String templatePath = "src/main/resources/templates/email/email-confirmation.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));

        // Replace placeholders with actual values
        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{confirmationLink}", confirmationLink);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

        // Create and send the email
        Mail mail =
                Mail.withHtml(user.getEmail(), "Please Confirm Your Email Address", htmlContent);

        LOG.infof("Sending email confirmation to %s", user.getEmail());
        mailer.send(mail);
    }

    private String createConfirmationLink(User user) {
        String token = RandomUUIDString.generate();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        EmailVerification emailVerification = new EmailVerification(user, token, expirationTime);

        // persist and get id of entry with emailVerificationRepository
        emailVerificationRepository.persist(emailVerification);

        return baseUrl
                + "/api/user/confirm-email"
                + "?id="
                + emailVerification.id
                + "&token="
                + token;
    }

    public void sendReservationConfirmation(User user, List<Reservation> reservations)
            throws IOException {
        if (reservations == null || reservations.isEmpty()) {
            return;
        }

        Event event = reservations.getFirst().getEvent();
        String eventName = event.getName();

        // Prepare data for SVG rendering
        List<Seat> allSeats = seatRepository.findByEventLocation(event.getEventLocation());
        List<Reservation> allUserReservationsForEvent =
                reservationRepository.findByUserAndEvent(user, event);

        Set<String> newSeatNumbers =
                reservations.stream()
                        .map(r -> r.getSeat().getSeatNumber())
                        .collect(Collectors.toSet());

        Set<String> existingSeatNumbers =
                allUserReservationsForEvent.stream()
                        .map(r -> r.getSeat().getSeatNumber())
                        .collect(Collectors.toSet());
        existingSeatNumbers.removeAll(newSeatNumbers); // Keep only previously reserved seats

        String svgContent = SvgRenderer.renderSeats(allSeats, newSeatNumbers, existingSeatNumbers);

        StringBuilder seatListHtml = new StringBuilder();
        for (Reservation reservation : reservations) {
            seatListHtml
                    .append("<li>")
                    .append(reservation.getSeat().getSeatNumber())
                    .append("</li>");
        }

        String templatePath = "src/main/resources/templates/email/reservation-confirmation.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(templatePath)));

        htmlContent =
                htmlContent.replace("{userName}", user.getFirstname() + " " + user.getLastname());
        htmlContent = htmlContent.replace("{eventName}", eventName != null ? eventName : "");
        htmlContent = htmlContent.replace("{seatList}", seatListHtml.toString());
        htmlContent = htmlContent.replace("{seatMap}", svgContent);
        htmlContent = htmlContent.replace("{currentYear}", Year.now().toString());

        Mail mail = Mail.withHtml(user.getEmail(), "Ihre Reservierungsbestätigung", htmlContent);
        LOG.infof("Sending reservation confirmation to %s", user.getEmail());
        mailer.send(mail);
    }
}
