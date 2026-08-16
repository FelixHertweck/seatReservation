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
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;

import de.felixhertweck.seatreservation.common.events.ReservationCancelledEvent;
import de.felixhertweck.seatreservation.common.events.ReservationCreatedEvent;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.email.service.notifications.EmailConfirmationNotification;
import de.felixhertweck.seatreservation.email.service.notifications.PasswordChangedNotification;
import de.felixhertweck.seatreservation.email.service.notifications.PasswordResetNotification;
import de.felixhertweck.seatreservation.email.service.notifications.TwoFactorCodeNotification;
import de.felixhertweck.seatreservation.email.service.notifications.UsernameRecoveryNotification;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.PasswordResetToken;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.utils.VerificationCodeGenerator;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Thin facade over the individual {@link EmailNotification} implementations: builds the
 * notification for a given business event and hands it off to {@link EmailSender}. The
 * reservation-related mails (confirmation, box office, update, reminder, CSV export) share
 * seat-loading/QR/entrance-info logic that lives in {@link ReservationEmailContent}.
 */
@ApplicationScoped
public class EmailService {

    @ConfigProperty(name = "email.header.confirmation", defaultValue = "Email Confirmation")
    String EMAIL_HEADER_CONFIRMATION;

    @ConfigProperty(name = "email.header.password-changed", defaultValue = "Password Changed")
    String EMAIL_HEADER_PASSWORD_CHANGED;

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

    @Inject EmailSender emailSender;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Inject ReservationService reservationService;

    @Inject ReservationEmailContent reservationEmailContent;

    @ConfigProperty(name = "email.frontend-base-url", defaultValue = "")
    String frontendBaseUrl;

    @ConfigProperty(name = "email.verification.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    @Inject
    @Location("email/email-confirmation")
    Template emailConfirmationTemplate;

    @Inject
    @Location("email/password-changed")
    Template passwordChangedTemplate;

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
        if (user == null || !EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email address provided for 2FA code.");
            return;
        }

        LOG.debugf(
                "Sending 2FA code email to User ID: %s, Username: %s", user.id, user.getUsername());

        emailSender.send(
                new TwoFactorCodeNotification(
                        user, code, EMAIL_HEADER_TWO_FACTOR, twoFactorCodeTemplate));
    }

    public void sendPasswordResetEmail(User user, PasswordResetToken passwordResetToken) {
        if (user == null || !EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for password reset.");
            return;
        }

        LOG.debugf("User ID: %s, Username: %s", user.id, user.getUsername());

        emailSender.send(
                new PasswordResetNotification(
                        user,
                        passwordResetToken,
                        frontendBaseUrl,
                        EMAIL_HEADER_PASSWORD_RESET,
                        passwordResetTemplate));
    }

    public void sendUsernameRecoveryEmail(String email, List<String> usernames) {
        if (!EmailSender.isValidAddress(email)) {
            LOG.warn("No valid email addresses provided for username recovery.");
            return;
        }

        LOG.debugf("Username recovery subject: %s", EMAIL_HEADER_USERNAME_RECOVERY);
        emailSender.send(
                new UsernameRecoveryNotification(
                        email,
                        usernames,
                        EMAIL_HEADER_USERNAME_RECOVERY,
                        usernameRecoveryTemplate));
    }

    public void sendEmailConfirmation(User user, EmailVerification emailVerification) {
        if (user == null || !EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for email confirmation.");
            return;
        }

        LOG.debugf("User ID: %s, Username: %s", user.id, user.getUsername());

        String verificationLink = generateVerificationLink(emailVerification.getToken());
        emailSender.send(
                new EmailConfirmationNotification(
                        user,
                        emailVerification,
                        verificationLink,
                        EMAIL_HEADER_CONFIRMATION,
                        emailConfirmationTemplate));
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
     * Sends a password changed notification email to the specified user.
     *
     * @param user the user to whom the password changed email will be sent
     */
    public void sendPasswordChangedNotification(User user) {
        if (user == null || !EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for password change notification.");
            return;
        }

        LOG.debugf("User ID: %s, Username: %s", user.id, user.getUsername());

        emailSender.send(
                new PasswordChangedNotification(
                        user, EMAIL_HEADER_PASSWORD_CHANGED, passwordChangedTemplate));
    }

    /**
     * Reacts to a reservation creation by sending the confirmation email. Observed synchronously
     * (same transaction as the reservation) so the entities are still attached and safe to
     * lazy-load. Failures are swallowed here rather than propagated, matching the previous
     * caller-side behavior of not letting a mail failure roll back the reservation.
     *
     * @param event the reservation-created notification
     */
    public void onReservationCreated(@Observes ReservationCreatedEvent event) {
        try {
            sendReservationConfirmation(event.user(), event.reservations());
        } catch (PersistenceException | IllegalStateException e) {
            LOG.error("Failed to send reservation confirmation email", e);
        }
    }

    /**
     * Reacts to a reservation cancellation by sending the update confirmation email. Observed
     * synchronously (same transaction as the cancellation) so the entities are still attached and
     * safe to lazy-load.
     *
     * @param event the reservation-cancelled notification
     */
    public void onReservationCancelled(@Observes ReservationCancelledEvent event) {
        if (event.noticeMessage() != null) {
            sendUpdateReservationConfirmation(
                    event.user(),
                    event.deletedReservations(),
                    event.activeReservations(),
                    null,
                    null,
                    null,
                    event.noticeMessage());
        } else {
            sendUpdateReservationConfirmation(
                    event.user(), event.deletedReservations(), event.activeReservations());
        }
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @return {@code true} if the email was enqueued, {@code false} if it was skipped (e.g. no
     *     valid email address or no reservations to include).
     */
    public boolean sendReservationConfirmation(User user, List<Reservation> reservations) {
        return sendReservationConfirmation(user, reservations, null);
    }

    /**
     * Sends a reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param reservations The list of reservations to include in the email.
     * @param additionalMailAddress An optional email address to override the user's email.
     * @return {@code true} if the email was enqueued, {@code false} if it was skipped (e.g. no
     *     valid email address or no reservations to include).
     */
    public boolean sendReservationConfirmation(
            User user, List<Reservation> reservations, String additionalMailAddress) {
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
     */
    public boolean sendReservationConfirmation(
            User user,
            List<Reservation> reservations,
            String additionalMailAddress,
            boolean includeExistingReservations) {
        return reservationEmailContent.sendReservationConfirmation(
                user, reservations, additionalMailAddress, includeExistingReservations);
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
        return reservationEmailContent.renderReservationConfirmationDisplay(user, reservations);
    }

    public String getReservationConfirmationSubject() {
        return reservationEmailContent.getReservationConfirmationSubject();
    }

    /**
     * Sends the dedicated "box office" confirmation email, used for both known-user and walk-in
     * guest box office reservations, in place of {@link #sendReservationConfirmation}'s normal
     * template (which assumes an account with an interactive seatmap). Always returns the rendered
     * content, regardless of whether a valid recipient address was found, so the caller can offer a
     * print copy even when no email was sent.
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
    public ReservationEmailContent.BoxOfficeConfirmationContent sendBoxOfficeConfirmation(
            User user,
            List<Reservation> reservations,
            String recipientName,
            String additionalMailAddress,
            boolean includeQrCode) {
        return reservationEmailContent.sendBoxOfficeConfirmation(
                user, reservations, recipientName, additionalMailAddress, includeQrCode);
    }

    /**
     * Sends an update reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param deletedReservations The list of deleted reservations.
     * @param activeReservations The list of active reservations.
     */
    public void sendUpdateReservationConfirmation(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations) {
        sendUpdateReservationConfirmation(user, deletedReservations, activeReservations, null);
    }

    /**
     * Sends an update reservation confirmation email to the user.
     *
     * @param user The user to whom the email will be sent.
     * @param deletedReservations The list of deleted reservations.
     * @param activeReservations The list of active reservations.
     * @param additionalMailAddress An optional email address to override the user's email.
     */
    public void sendUpdateReservationConfirmation(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations,
            String additionalMailAddress) {
        reservationEmailContent.sendUpdateReservationConfirmation(
                user, deletedReservations, activeReservations, additionalMailAddress);
    }

    /**
     * Sends a modular update reservation confirmation email to the user with custom header and
     * notice.
     *
     * @param user The user to whom the email will be sent.
     * @param deletedReservations The list of deleted reservations.
     * @param activeReservations The list of active reservations.
     * @param additionalMailAddress An optional email address to override the user's email.
     * @param customSubject Optional custom email subject.
     * @param customHeader Optional custom header banner text.
     * @param noticeMessage Optional message/reason explaining why the update happened.
     */
    public void sendUpdateReservationConfirmation(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations,
            String additionalMailAddress,
            String customSubject,
            String customHeader,
            String noticeMessage) {
        reservationEmailContent.sendUpdateReservationConfirmation(
                user,
                deletedReservations,
                activeReservations,
                additionalMailAddress,
                customSubject,
                customHeader,
                noticeMessage);
    }

    /**
     * Sends an event reminder email to the specified user.
     *
     * @param user the user to whom the reminder email will be sent
     * @param event the event for which the reminder is being sent
     * @param reservations the list of reservations made by the user for the event
     */
    public void sendEventReminder(User user, Event event, List<Reservation> reservations) {
        if (!EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for event reminder.");
            return;
        }
        reservationEmailContent.sendEventReminder(user, event, reservations);
    }

    /**
     * Sends an event rescheduled notification email to the specified user.
     *
     * @param user the user to whom the reschedule notification email will be sent
     * @param event the event that was rescheduled
     * @param reservations the list of reservations made by the user for the event
     * @param oldStartTime the previous start time
     * @param oldEndTime the previous end time
     * @param oldLocationName the previous location name
     * @param oldBookingDeadline the previous booking deadline
     * @param additionalMailAddress optional additional email address
     */
    public void sendEventRescheduledNotification(
            User user,
            Event event,
            List<Reservation> reservations,
            Instant oldStartTime,
            Instant oldEndTime,
            String oldLocationName,
            Instant oldBookingDeadline,
            String additionalMailAddress) {
        if (user == null || !EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email addresses provided for event reschedule notification.");
            return;
        }
        reservationEmailContent.sendEventRescheduledNotification(
                user,
                event,
                reservations,
                oldStartTime,
                oldEndTime,
                oldLocationName,
                oldBookingDeadline,
                additionalMailAddress);
    }

    /**
     * Sends an email to the event manager with a CSV export of all reservations for a given event.
     *
     * @param manager the manager of the event
     * @param event the event for which the reservations are to be exported
     * @throws IOException if CSV export fails
     * @throws EventNotFoundException if the event is not found
     * @throws AccessDeniedException if there are security issues during CSV export
     */
    public void sendEventReservationsCsvToManager(User manager, Event event)
            throws EventNotFoundException, AccessDeniedException, IOException {
        if (!EmailSender.isValidAddress(manager.getEmail())) {
            LOG.warn("No valid email addresses provided to send CSV export.");
            return;
        }
        LOG.debugf("Manager ID: %s, Event ID: %s", manager.id, event.id);

        byte[] csvData = reservationService.exportReservationsToCsv(event.id, manager);
        LOG.debugf(
                "Generated CSV data of size %d bytes for event ID: %s", csvData.length, event.id);

        reservationEmailContent.sendEventReservationsCsvToManager(manager, event, csvData);
    }
}
