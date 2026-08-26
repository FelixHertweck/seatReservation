/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.google.zxing.WriterException;
import de.felixhertweck.seatreservation.email.queue.EmailAttachment;
import de.felixhertweck.seatreservation.email.service.notifications.BoxOfficeConfirmationNotification;
import de.felixhertweck.seatreservation.email.service.notifications.EventReminderNotification;
import de.felixhertweck.seatreservation.email.service.notifications.EventReservationsCsvNotification;
import de.felixhertweck.seatreservation.email.service.notifications.ReservationConfirmationNotification;
import de.felixhertweck.seatreservation.email.service.notifications.ReservationUpdateNotification;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.QRCodeImage;
import de.felixhertweck.seatreservation.utils.SeatComparators;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import de.felixhertweck.seatreservation.wallet.service.WalletPassService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Shared content-building logic for the reservation-related emails (confirmation, box office
 * confirmation, update, reminder) and the manager CSV export -- these all need the same
 * seat-loading, QR-code, and entrance-info pieces, plus their own Qute template. Each {@code
 * sendXxx} method renders the content and hands the result off to {@link EmailSender} as an {@link
 * EmailNotification}.
 */
@ApplicationScoped
public class ReservationEmailContent {

    private static final Logger LOG = Logger.getLogger(ReservationEmailContent.class);

    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_EVENT_NAME = "eventName";
    private static final String KEY_EVENT_LOCATION = "eventLocation";
    private static final String KEY_EVENT_START_TIME = "eventStartTime";
    private static final String KEY_EVENT_END_TIME = "eventEndTime";
    private static final String KEY_ENTRANCE_INFO = "entranceInfo";
    private static final String KEY_EVENT_LINK = "eventLink";
    private static final String KEY_SEATMAP_LINK = "seatmapLink";
    private static final String KEY_CURRENT_YEAR = "currentYear";
    private static final String KEY_SEATS = "seats";
    private static final String KEY_GOOGLE_WALLET_LINK = "googleWalletLink";
    private static final String KEY_APPLE_WALLET_LINK = "appleWalletLink";
    private static final String KEY_FRONTEND_BASE_URL = "frontendBaseUrl";

    @Inject SeatRepository seatRepository;

    @Inject ReservationRepository reservationRepository;

    @Inject EmailSeatMapService emailSeatMapService;

    @Inject EmailSender emailSender;

    @Inject WalletPassService walletPassService;

    @ConfigProperty(name = "email.frontend-base-url", defaultValue = "")
    String frontendBaseUrl;

    @ConfigProperty(
            name = "email.entrance-info-template",
            defaultValue = "Entrance: {entrance} for seats: {seats}")
    String entranceInfoTemplate;

    @ConfigProperty(
            name = "email.header.reservation-confirmation",
            defaultValue = "Reservation Confirmation")
    String reservationConfirmationSubject;

    @ConfigProperty(
            name = "email.header.boxoffice-confirmation",
            defaultValue = "Box Office Reservation Confirmation")
    String boxOfficeConfirmationSubject;

    @ConfigProperty(name = "email.header.reservation-update", defaultValue = "Reservation Update")
    String reservationUpdateSubject;

    @ConfigProperty(
            name = "email.header.event-rescheduled",
            defaultValue = "Important: Your Event Schedule Has Changed")
    String eventRescheduledSubject;

    @ConfigProperty(name = "email.header.reminder", defaultValue = "Reservation Reminder")
    String eventReminderSubject;

    @ConfigProperty(
            name = "email.header.reservation-overview",
            defaultValue = "Reservation Overview")
    String reservationOverviewSubject;

    @ConfigProperty(
            name = "email.header.event-cancelled",
            defaultValue = "Important: Event Cancelled")
    String eventCancelledSubject;

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
    @Location("email/event-rescheduled")
    Template eventRescheduledTemplate;

    @Inject
    @Location("email/event-reminder")
    Template eventReminderTemplate;

    @Inject
    @Location("email/manager-reservation-export")
    Template managerExportTemplate;

    @Inject
    @Location("email/event-cancelled")
    Template eventCancelledTemplate;

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    public Map<UUID, Seat> loadSeatsForReservations(
            List<Reservation> reservations, List<Reservation> additionalReservations) {
        Set<UUID> seatIds = new HashSet<>();
        if (reservations != null) {
            reservations.stream()
                    .map(r -> r.getSeat() != null ? r.getSeat().getId() : null)
                    .filter(Objects::nonNull)
                    .forEach(seatIds::add);
        }
        if (additionalReservations != null) {
            additionalReservations.stream()
                    .map(r -> r.getSeat() != null ? r.getSeat().getId() : null)
                    .filter(Objects::nonNull)
                    .forEach(seatIds::add);
        }

        List<Seat> seats = seatRepository.findByIdsWithAreaAndEntrance(seatIds);
        return seats.stream().collect(Collectors.toMap(Seat::getId, s -> s));
    }

    public List<SeatView> toSeatViews(List<Reservation> reservations, Map<UUID, Seat> seatById) {
        if (reservations == null) {
            return List.of();
        }
        return reservations.stream()
                .map(r -> r.getSeat() != null ? seatById.get(r.getSeat().getId()) : null)
                .filter(Objects::nonNull)
                .map(
                        seat ->
                                new SeatView(
                                        seat.getSeatNumber(),
                                        seat.getSeatRow(),
                                        seat.getArea() != null ? seat.getArea().getName() : null))
                .sorted(SeatComparators.SEAT_VIEW_COMPARATOR)
                .toList();
    }

    public String generateEntranceInfo(List<Reservation> reservations, Map<UUID, Seat> seatById) {
        if (reservations == null || reservations.isEmpty()) {
            return "";
        }

        var seatsByEntrance =
                reservations.stream()
                        .map(r -> r.getSeat() != null ? seatById.get(r.getSeat().getId()) : null)
                        .filter(
                                seat ->
                                        seat != null
                                                && seat.getEntrance() != null
                                                && !seat.getEntrance().getName().trim().isEmpty())
                        .collect(
                                Collectors.groupingBy(
                                        seat -> seat.getEntrance().getName(),
                                        Collectors.mapping(
                                                Seat::getSeatNumber, Collectors.toList())));

        if (seatsByEntrance.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        List<String> sortedEntranceNames = new ArrayList<>(seatsByEntrance.keySet());
        sortedEntranceNames.sort(SeatComparators.ALPHANUMERIC_COMPARATOR);
        for (String entranceName : sortedEntranceNames) {
            List<String> seatNumbers = new ArrayList<>(seatsByEntrance.get(entranceName));
            seatNumbers.sort(SeatComparators.ALPHANUMERIC_COMPARATOR);
            String seatList = String.join(", ", seatNumbers);
            String line =
                    entranceInfoTemplate
                            .replace("{entrance}", entranceName)
                            .replace("{seats}", seatList);
            result.append(line).append("\n");
        }
        return result.toString().trim();
    }

    /**
     * Generates the QR code payload for a check-in token. Format is fixed at {@code
     * userId;eventId;token} -- the check-in scanner (webapp/components/checkin/qr-code-scanner.tsx)
     * parses on that exact separator, so it can't be changed without a matching frontend change.
     */
    public String generateQrCodeContent(User user, Event event, CheckInToken token) {
        if (token == null || token.getToken() == null) {
            return "";
        }
        return user.id.toString() + ";" + event.id.toString() + ";" + token.getToken();
    }

    public byte[] generateQrCodeImage(String content) {
        try {
            return QRCodeImage.generateQrCodeImage(content, 400, 400);
        } catch (WriterException | IOException e) {
            LOG.error("Failed to generate QR code for reservation email", e);
            return new byte[0];
        }
    }

    public List<EmailAttachment> buildInlineAttachments(byte[] seatmapPng, byte[] qrCode) {
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

    public static String fullName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getFirstname() != null && user.getLastname() != null) {
            return user.getFirstname() + " " + user.getLastname();
        }
        return user.getUsername() != null ? user.getUsername() : "";
    }

    public static String formatDateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public static String currentYear() {
        return String.valueOf(Year.now(ZoneId.systemDefault()).getValue());
    }

    private String generateEventLink(UUID eventId) {
        return frontendBaseUrl.trim() + "/events/reservations?eventId=" + eventId;
    }

    private String generateSeatmapLink(String token) {
        return frontendBaseUrl.trim() + "/email/seatmap?token=" + token;
    }

    private String generateAppleWalletLink(String token) {
        if (!walletPassService.isAppleWalletEnabled()) return null;
        if (token == null || token.isBlank()) return null;
        return frontendBaseUrl.trim() + "/api/email/wallet/apple?token=" + token;
    }

    private String generateGoogleWalletLink(User user, Reservation reservation) {
        if (!walletPassService.isGoogleWalletEnabled()) return null;
        if (user == null || reservation == null || reservation.id == null) return null;
        try {
            WalletPassResponseDTO dto =
                    walletPassService.generatePass(reservation.id, user, WalletProvider.GOOGLE);
            return dto != null ? dto.url() : null;
        } catch (Exception e) {
            LOG.debugf("Could not generate Google Wallet link for email: %s", e.getMessage());
            return null;
        }
    }

    private String embedImageAsDataUri(String htmlContent, String cidPlaceholder, byte[] image) {
        if (image != null && image.length > 0) {
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
            return htmlContent.replace(cidPlaceholder, dataUri);
        }
        return htmlContent.replace(cidPlaceholder, "");
    }

    // ---------------------------------------------------------------------
    // Reservation confirmation
    // ---------------------------------------------------------------------

    private record ReservationConfirmationContent(
            String htmlContent, byte[] seatmapPng, byte[] qrCodeImage) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o
                    instanceof
                    ReservationConfirmationContent(
                            String otherHtmlContent,
                            byte[] otherSeatmapPng,
                            byte[] otherQrCode))) {
                return false;
            }
            return Objects.equals(htmlContent, otherHtmlContent)
                    && Arrays.equals(seatmapPng, otherSeatmapPng)
                    && Arrays.equals(qrCodeImage, otherQrCode);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(htmlContent)
                    + Arrays.hashCode(seatmapPng)
                    + Arrays.hashCode(qrCodeImage);
        }

        @Override
        public String toString() {
            return "ReservationConfirmationContent{"
                    + "htmlContent='"
                    + htmlContent
                    + '\''
                    + ", seatmapPng="
                    + Arrays.toString(seatmapPng)
                    + ", qrCodeImage="
                    + Arrays.toString(qrCodeImage)
                    + '}';
        }
    }

    private ReservationConfirmationContent renderReservationConfirmation(
            User user, List<Reservation> reservations, boolean includeExistingReservations) {
        Event event = reservations.getFirst().getEvent();
        String eventName = event.getName();

        String seatmapToken =
                emailSeatMapService.createEmailSeatMapToken(user, event, reservations);
        String seatmapLink = generateSeatmapLink(seatmapToken);
        byte[] pngImage = emailSeatMapService.getPngImage(seatmapToken).orElse(new byte[0]);

        List<Reservation> allUserReservationsForEvent =
                includeExistingReservations
                        ? reservationRepository.findByUserAndEvent(user, event)
                        : reservations;

        Map<UUID, Seat> seatById =
                loadSeatsForReservations(reservations, allUserReservationsForEvent);

        Set<UUID> newSeatIds =
                reservations.stream().map(r -> r.getSeat().getId()).collect(Collectors.toSet());
        Set<UUID> existingSeatIds =
                allUserReservationsForEvent.stream()
                        .map(r -> r.getSeat().getId())
                        .collect(Collectors.toSet());
        existingSeatIds.removeAll(newSeatIds);

        List<SeatView> newSeats = toSeatViews(reservations, seatById);
        List<SeatView> existingSeats =
                existingSeatIds.stream()
                        .map(seatById::get)
                        .filter(Objects::nonNull)
                        .map(
                                seat ->
                                        new SeatView(
                                                seat.getSeatNumber(),
                                                seat.getSeatRow(),
                                                seat.getArea() != null
                                                        ? seat.getArea().getName()
                                                        : null))
                        .sorted(SeatComparators.SEAT_VIEW_COMPARATOR)
                        .toList();

        String entranceInfo = generateEntranceInfo(reservations, seatById);

        String googleWalletLink = generateGoogleWalletLink(user, reservations.getFirst());
        String appleWalletLink = generateAppleWalletLink(seatmapToken);

        String htmlContent =
                reservationConfirmationTemplate
                        .data(KEY_USER_NAME, user.getUsername())
                        .data(KEY_FULL_NAME, fullName(user))
                        .data(KEY_EVENT_NAME, eventName != null ? eventName : "")
                        .data(KEY_EVENT_LOCATION, event.getEventLocation().getName())
                        .data(KEY_EVENT_START_TIME, formatDateTime(event.getStartTime()))
                        .data(KEY_EVENT_END_TIME, formatDateTime(event.getEndTime()))
                        .data("newSeats", newSeats)
                        .data("hasExistingSeats", !existingSeats.isEmpty())
                        .data("existingSeats", existingSeats)
                        .data(KEY_ENTRANCE_INFO, entranceInfo)
                        .data(KEY_EVENT_LINK, generateEventLink(event.id))
                        .data(KEY_SEATMAP_LINK, seatmapLink)
                        .data(KEY_GOOGLE_WALLET_LINK, googleWalletLink)
                        .data(KEY_APPLE_WALLET_LINK, appleWalletLink)
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        CheckInToken token = reservations.getFirst().getCheckInToken();
        String qrCodeContent = generateQrCodeContent(user, event, token);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        return new ReservationConfirmationContent(htmlContent, pngImage, qrCodeImage);
    }

    public String getReservationConfirmationSubject() {
        return reservationConfirmationSubject;
    }

    public boolean sendReservationConfirmation(
            User user,
            List<Reservation> reservations,
            String additionalMailAddress,
            boolean includeExistingReservations) {
        if (reservations == null || reservations.isEmpty()) {
            LOG.warnf(
                    "No reservations provided for confirmation email to user %s.",
                    user != null ? user.getEmail() : null);
            return false;
        }

        ReservationConfirmationContent content =
                renderReservationConfirmation(user, reservations, includeExistingReservations);

        return emailSender.send(
                new ReservationConfirmationNotification(
                        user,
                        additionalMailAddress,
                        reservationConfirmationSubject,
                        content.htmlContent(),
                        content.seatmapPng(),
                        content.qrCodeImage()));
    }

    public String renderReservationConfirmationDisplay(User user, List<Reservation> reservations) {
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

    // ---------------------------------------------------------------------
    // Box office confirmation
    // ---------------------------------------------------------------------

    /**
     * The rendered box office confirmation, returned regardless of whether a valid recipient
     * address was found, so the caller can offer a print copy even when no email was sent.
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
            if (!(o
                    instanceof
                    BoxOfficeConfirmationContent(
                            String otherEmailHtml,
                            String otherDisplayHtml,
                            byte[] otherQrCode))) {
                return false;
            }
            return Objects.equals(emailHtml, otherEmailHtml)
                    && Objects.equals(displayHtml, otherDisplayHtml)
                    && Arrays.equals(qrCodeImage, otherQrCode);
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

    private BoxOfficeConfirmationContent renderBoxOfficeConfirmation(
            String recipientName,
            Event event,
            List<Reservation> reservations,
            boolean includeQrCode) {
        Map<UUID, Seat> seatById = loadSeatsForReservations(reservations, null);
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

        User qrOwner =
                (reservations != null && !reservations.isEmpty())
                        ? reservations.getFirst().getUser()
                        : null;
        String seatmapToken =
                (qrOwner != null
                                && event != null
                                && reservations != null
                                && !reservations.isEmpty())
                        ? emailSeatMapService.createEmailSeatMapToken(qrOwner, event, reservations)
                        : null;
        String googleWalletLink =
                (qrOwner != null && reservations != null && !reservations.isEmpty())
                        ? generateGoogleWalletLink(qrOwner, reservations.getFirst())
                        : null;
        String appleWalletLink = generateAppleWalletLink(seatmapToken);

        String emailHtml =
                boxOfficeConfirmationTemplate
                        .data("recipientName", recipientName)
                        .data(KEY_EVENT_NAME, event.getName() != null ? event.getName() : "")
                        .data(KEY_EVENT_LOCATION, event.getEventLocation().getName())
                        .data(KEY_EVENT_START_TIME, formatDateTime(event.getStartTime()))
                        .data(KEY_EVENT_END_TIME, formatDateTime(event.getEndTime()))
                        .data(KEY_SEATS, seats)
                        .data(KEY_ENTRANCE_INFO, entranceInfo)
                        .data("showQrCode", showQrCode)
                        .data("qrCodeImageSrc", "cid:qrcode-image")
                        .data(KEY_GOOGLE_WALLET_LINK, googleWalletLink)
                        .data(KEY_APPLE_WALLET_LINK, appleWalletLink)
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        String displayHtml =
                boxOfficeConfirmationTemplate
                        .data("recipientName", recipientName)
                        .data(KEY_EVENT_NAME, event.getName() != null ? event.getName() : "")
                        .data(KEY_EVENT_LOCATION, event.getEventLocation().getName())
                        .data(KEY_EVENT_START_TIME, formatDateTime(event.getStartTime()))
                        .data(KEY_EVENT_END_TIME, formatDateTime(event.getEndTime()))
                        .data(KEY_SEATS, seats)
                        .data(KEY_ENTRANCE_INFO, entranceInfo)
                        .data("showQrCode", showQrCode)
                        .data("qrCodeImageSrc", qrCodeDataUri)
                        .data(KEY_GOOGLE_WALLET_LINK, googleWalletLink)
                        .data(KEY_APPLE_WALLET_LINK, appleWalletLink)
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        return new BoxOfficeConfirmationContent(emailHtml, displayHtml, qrCodeImage);
    }

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

        emailSender.send(
                new BoxOfficeConfirmationNotification(
                        user,
                        additionalMailAddress,
                        boxOfficeConfirmationSubject,
                        content.emailHtml(),
                        content.qrCodeImage()));

        return content;
    }

    // ---------------------------------------------------------------------
    // Update confirmation
    // ---------------------------------------------------------------------

    public void sendUpdateReservationConfirmation(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations,
            String additionalMailAddress) {
        sendUpdateReservationConfirmation(
                user,
                deletedReservations,
                activeReservations,
                additionalMailAddress,
                reservationUpdateSubject,
                null,
                null);
    }

    public void sendUpdateReservationConfirmation(
            User user,
            List<Reservation> deletedReservations,
            List<Reservation> activeReservations,
            String additionalMailAddress,
            String customSubject,
            String customHeader,
            String noticeMessage) {
        if ((deletedReservations == null || deletedReservations.isEmpty())
                && (activeReservations == null || activeReservations.isEmpty())) {
            LOG.warnf(
                    "No reservations provided for update email to user %s.",
                    user != null ? user.getEmail() : null);
            return;
        }

        Event event =
                (deletedReservations != null && !deletedReservations.isEmpty())
                        ? deletedReservations.getFirst().getEvent()
                        : activeReservations.getFirst().getEvent();
        String eventName = event.getName();

        boolean hasActiveSeats = activeReservations != null && !activeReservations.isEmpty();

        String seatmapToken =
                hasActiveSeats
                        ? emailSeatMapService.createEmailSeatMapToken(
                                user, event, activeReservations)
                        : null;
        String seatmapLink = seatmapToken != null ? generateSeatmapLink(seatmapToken) : "";
        byte[] pngImage =
                seatmapToken != null
                        ? emailSeatMapService.getPngImage(seatmapToken).orElse(new byte[0])
                        : new byte[0];

        Map<UUID, Seat> seatById =
                loadSeatsForReservations(deletedReservations, activeReservations);

        List<SeatView> deletedSeats =
                deletedReservations != null
                        ? toSeatViews(deletedReservations, seatById)
                        : List.of();
        List<SeatView> activeSeats =
                hasActiveSeats ? toSeatViews(activeReservations, seatById) : List.of();

        String entranceInfo =
                hasActiveSeats ? generateEntranceInfo(activeReservations, seatById) : "";

        String googleWalletLink =
                hasActiveSeats
                        ? generateGoogleWalletLink(user, activeReservations.getFirst())
                        : null;
        String appleWalletLink = generateAppleWalletLink(seatmapToken);

        String subject = customSubject != null ? customSubject : reservationUpdateSubject;
        String header =
                customHeader != null ? customHeader : "Your reservation update confirmation";

        String htmlContent =
                reservationUpdateTemplate
                        .data(KEY_USER_NAME, user.getUsername())
                        .data(KEY_FULL_NAME, fullName(user))
                        .data("emailTitle", subject)
                        .data("emailHeader", header)
                        .data("noticeMessage", noticeMessage)
                        .data(KEY_EVENT_NAME, eventName != null ? eventName : "")
                        .data(
                                KEY_EVENT_LOCATION,
                                event.getEventLocation() != null
                                        ? event.getEventLocation().getName()
                                        : "")
                        .data(KEY_EVENT_START_TIME, formatDateTime(event.getStartTime()))
                        .data(KEY_EVENT_END_TIME, formatDateTime(event.getEndTime()))
                        .data("deletedSeats", deletedSeats)
                        .data("hasActiveSeats", hasActiveSeats)
                        .data("activeSeats", activeSeats)
                        .data(KEY_ENTRANCE_INFO, entranceInfo)
                        .data(KEY_EVENT_LINK, generateEventLink(event.id))
                        .data(KEY_SEATMAP_LINK, seatmapLink)
                        .data(KEY_GOOGLE_WALLET_LINK, googleWalletLink)
                        .data(KEY_APPLE_WALLET_LINK, appleWalletLink)
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        byte[] qrCodeImage = new byte[0];
        if (hasActiveSeats) {
            CheckInToken token = activeReservations.getFirst().getCheckInToken();
            String qrCodeContent = generateQrCodeContent(user, event, token);
            qrCodeImage = generateQrCodeImage(qrCodeContent);
        }

        emailSender.send(
                new ReservationUpdateNotification(
                        user, additionalMailAddress, subject, htmlContent, pngImage, qrCodeImage));
    }

    // ---------------------------------------------------------------------
    // Event rescheduled
    // ---------------------------------------------------------------------

    public void sendEventRescheduledNotification(
            User user,
            Event event,
            List<Reservation> reservations,
            Instant oldStartTime,
            Instant oldEndTime,
            String oldLocationName,
            Instant oldBookingDeadline,
            String additionalMailAddress) {
        if (reservations == null || reservations.isEmpty()) {
            LOG.warnf(
                    "No reservations provided for reschedule notification to user %s.",
                    user != null ? user.getEmail() : null);
            return;
        }

        String eventName = event.getName();

        String seatmapToken =
                emailSeatMapService.createEmailSeatMapToken(user, event, reservations);
        String seatmapLink = seatmapToken != null ? generateSeatmapLink(seatmapToken) : "";
        byte[] pngImage =
                seatmapToken != null
                        ? emailSeatMapService.getPngImage(seatmapToken).orElse(new byte[0])
                        : new byte[0];

        Map<UUID, Seat> seatById = loadSeatsForReservations(reservations, null);
        List<SeatView> seats = toSeatViews(reservations, seatById);
        String entranceInfo = generateEntranceInfo(reservations, seatById);

        String googleWalletLink = generateGoogleWalletLink(user, reservations.getFirst());
        String appleWalletLink = generateAppleWalletLink(seatmapToken);

        boolean startTimeChanged = !Objects.equals(oldStartTime, event.getStartTime());
        boolean endTimeChanged = !Objects.equals(oldEndTime, event.getEndTime());
        String newLocationName =
                event.getEventLocation() != null ? event.getEventLocation().getName() : "";
        boolean locationChanged = !Objects.equals(oldLocationName, newLocationName);
        boolean bookingDeadlineChanged =
                !Objects.equals(oldBookingDeadline, event.getBookingDeadline());

        String htmlContent =
                eventRescheduledTemplate
                        .data(KEY_USER_NAME, user.getUsername())
                        .data(KEY_FULL_NAME, fullName(user))
                        .data(KEY_EVENT_NAME, eventName != null ? eventName : "")
                        .data(KEY_EVENT_LOCATION, newLocationName)
                        .data(KEY_EVENT_START_TIME, formatDateTime(event.getStartTime()))
                        .data(KEY_EVENT_END_TIME, formatDateTime(event.getEndTime()))
                        .data("oldStartTime", formatDateTime(oldStartTime))
                        .data("newStartTime", formatDateTime(event.getStartTime()))
                        .data("startTimeChanged", startTimeChanged)
                        .data("oldEndTime", formatDateTime(oldEndTime))
                        .data("newEndTime", formatDateTime(event.getEndTime()))
                        .data("endTimeChanged", endTimeChanged)
                        .data("oldLocation", oldLocationName != null ? oldLocationName : "")
                        .data("newLocation", newLocationName)
                        .data("locationChanged", locationChanged)
                        .data("oldBookingDeadline", formatDateTime(oldBookingDeadline))
                        .data("newBookingDeadline", formatDateTime(event.getBookingDeadline()))
                        .data("bookingDeadlineChanged", bookingDeadlineChanged)
                        .data(KEY_SEATS, seats)
                        .data(KEY_ENTRANCE_INFO, entranceInfo)
                        .data(KEY_EVENT_LINK, generateEventLink(event.id))
                        .data(KEY_SEATMAP_LINK, seatmapLink)
                        .data(KEY_GOOGLE_WALLET_LINK, googleWalletLink)
                        .data(KEY_APPLE_WALLET_LINK, appleWalletLink)
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        CheckInToken token = reservations.getFirst().getCheckInToken();
        String qrCodeContent = generateQrCodeContent(user, event, token);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        emailSender.send(
                new ReservationUpdateNotification(
                        user,
                        additionalMailAddress,
                        eventRescheduledSubject,
                        htmlContent,
                        pngImage,
                        qrCodeImage));
    }

    // ---------------------------------------------------------------------
    // Event cancelled
    // ---------------------------------------------------------------------

    public void sendEventCancelledNotification(
            User user, de.felixhertweck.seatreservation.common.events.EventCancelledEvent event) {
        if (user == null || !EmailSender.isValidAddress(user.getEmail())) {
            LOG.warn("No valid email address provided for event cancellation notification.");
            return;
        }

        String htmlContent =
                eventCancelledTemplate
                        .data(KEY_USER_NAME, user.getUsername())
                        .data(KEY_FULL_NAME, fullName(user))
                        .data(KEY_EVENT_NAME, event.eventName() != null ? event.eventName() : "")
                        .data(
                                KEY_EVENT_LOCATION,
                                event.locationName() != null ? event.locationName() : "")
                        .data(KEY_EVENT_START_TIME, formatDateTime(event.startTime()))
                        .data(KEY_EVENT_END_TIME, formatDateTime(event.endTime()))
                        .data(
                                "cancellationReason",
                                event.cancellationReason() != null
                                        ? event.cancellationReason()
                                        : "")
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        emailSender.send(
                new de.felixhertweck.seatreservation.email.service.notifications
                        .EventCancelledNotification(user, eventCancelledSubject, htmlContent));
    }

    // ---------------------------------------------------------------------
    // Event reminder
    // ---------------------------------------------------------------------

    public void sendEventReminder(User user, Event event, List<Reservation> reservations) {
        String seatmapToken =
                emailSeatMapService.createEmailSeatMapToken(user, event, reservations);
        String seatmapLink = generateSeatmapLink(seatmapToken);
        byte[] pngImage = emailSeatMapService.getPngImage(seatmapToken).orElse(new byte[0]);

        Map<UUID, Seat> seatById = loadSeatsForReservations(reservations, null);
        List<SeatView> seats = toSeatViews(reservations, seatById);
        String entranceInfo = generateEntranceInfo(reservations, seatById);

        String googleWalletLink =
                (reservations != null && !reservations.isEmpty())
                        ? generateGoogleWalletLink(user, reservations.getFirst())
                        : null;
        String appleWalletLink = generateAppleWalletLink(seatmapToken);

        String htmlContent =
                eventReminderTemplate
                        .data(KEY_USER_NAME, user.getUsername())
                        .data(KEY_FULL_NAME, fullName(user))
                        .data(KEY_EVENT_NAME, event.getName())
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
                        .data(KEY_EVENT_LOCATION, event.getEventLocation().getName())
                        .data(KEY_SEATS, seats)
                        .data(KEY_ENTRANCE_INFO, entranceInfo)
                        .data(KEY_SEATMAP_LINK, seatmapLink)
                        .data(KEY_EVENT_LINK, generateEventLink(event.id))
                        .data(KEY_GOOGLE_WALLET_LINK, googleWalletLink)
                        .data(KEY_APPLE_WALLET_LINK, appleWalletLink)
                        .data(KEY_FRONTEND_BASE_URL, frontendBaseUrl.trim())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        CheckInToken token =
                reservations != null && !reservations.isEmpty()
                        ? reservations.getFirst().getCheckInToken()
                        : null;
        String qrCodeContent = generateQrCodeContent(user, event, token);
        byte[] qrCodeImage = generateQrCodeImage(qrCodeContent);

        emailSender.send(
                new EventReminderNotification(
                        user, eventReminderSubject, htmlContent, pngImage, qrCodeImage));
    }

    // ---------------------------------------------------------------------
    // Manager CSV export
    // ---------------------------------------------------------------------

    public void sendEventReservationsCsvToManager(User manager, Event event, byte[] csvData) {
        String htmlContent =
                managerExportTemplate
                        .data(KEY_FULL_NAME, fullName(manager))
                        .data(KEY_EVENT_NAME, event.getName())
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
                        .data(KEY_EVENT_LOCATION, event.getEventLocation().getName())
                        .data(KEY_CURRENT_YEAR, currentYear())
                        .render();

        EmailAttachment csvAttachment =
                EmailAttachment.file("reservations_" + event.id + ".csv", "text/csv", csvData);

        emailSender.send(
                new EventReservationsCsvNotification(
                        manager,
                        reservationOverviewSubject.trim() + event.getName(),
                        htmlContent,
                        csvAttachment));
    }
}
