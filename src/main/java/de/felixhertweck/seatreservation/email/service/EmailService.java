package de.felixhertweck.seatreservation.email.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

/** Service for sending emails. */
@ApplicationScoped
public class EmailService {

    @Inject Mailer mailer;

    /**
     * Sends a hello world email to the specified recipient.
     *
     * @param recipient the email address of the recipient
     */
    public void sendHelloWorldEmail(String recipient) {
        Mail mail =
                Mail.withText(
                        recipient,
                        "Hello from Seat Reservation System",
                        "Hello World! This is a test email from the Seat Reservation System.");

        mailer.send(mail);
    }
}
