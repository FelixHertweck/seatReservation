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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import de.felixhertweck.seatreservation.utils.SvgRenderer;
import de.felixhertweck.seatreservation.utils.SvgToPngConverter;
import org.apache.batik.transcoder.TranscoderException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailSeatMapService {

    private static final Logger LOG = Logger.getLogger(EmailSeatMapService.class);

    @Inject EmailSeatMapTokenRepository tokenRepository;

    @ConfigProperty(name = "email.seatmap.token.expiration.days", defaultValue = "30")
    long tokenExpirationDays;

    /**
     * Creates and persists a new EmailSeatMapToken for the given user, event, and new reservations.
     *
     * @param user the user
     * @param event the event
     * @param newReservations the list of new reservations
     * @return the generated token string
     */
    @Transactional
    public String createEmailSeatMapToken(
            User user, Event event, List<Reservation> newReservations) {
        // Generate unique token
        String token = java.util.UUID.randomUUID().toString();

        // Extract seat numbers from new reservations
        Set<String> newReservedSeatNumbers =
                newReservations.stream()
                        .map(Reservation::getSeat)
                        .map(Seat::getSeatNumber)
                        .collect(java.util.stream.Collectors.toSet());

        // Calculate expiration time
        Instant now = Instant.now();
        Instant expirationTime = now.plus(tokenExpirationDays, java.time.temporal.ChronoUnit.DAYS);

        // Create and persist token
        EmailSeatMapToken emailSeatMapToken =
                new EmailSeatMapToken(
                        user, event, token, expirationTime, now, newReservedSeatNumbers);
        tokenRepository.persist(emailSeatMapToken);

        LOG.debugf("Created EmailSeatMapToken: %s", token);
        return token;
    }

    /**
     * Returns PNG image for the given token (for email embedding). Token is validated against
     * database. If token not found or expired, Optional.empty() is returned.
     *
     * @param token the email seat map token
     * @return an Optional containing the PNG image as a byte array, or empty if the token is
     *     invalid or expired
     * @see #getSvgImage(String)
     */
    public Optional<byte[]> getPngImage(String token) {
        Optional<String> svg = getSvgImage(token);
        if (!svg.isPresent()) return Optional.empty();

        try {
            byte[] png = SvgToPngConverter.convertSvgToPng(svg.get());
            return Optional.of(png);
        } catch (IOException | TranscoderException e) {
            LOG.error("Failed to convert SVG to PNG", e);
            return Optional.empty();
        }
    }

    /**
     * Returns SVG image for the given token (for email embedding). Token is validated against
     * database. If token not found or expired, Optional.empty() is returned.
     *
     * @param token the email seat map token
     * @return an Optional containing the SVG image as a string, or empty if the token is invalid or
     *     expired
     */
    public Optional<String> getSvgImage(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        Optional<EmailSeatMapToken> emailSeatMapTokenOptional = tokenRepository.findByToken(token);
        if (emailSeatMapTokenOptional.isEmpty()) {
            LOG.debugf("No EmailSeatMapToken found for token %s", token);
            return Optional.empty();
        }

        EmailSeatMapToken emailSeatMapToken = emailSeatMapTokenOptional.get();
        if (emailSeatMapToken.getExpirationTime() != null
                && emailSeatMapToken.getExpirationTime().isBefore(Instant.now())) {
            LOG.debugf("EmailSeatMapToken expired for token %s", token);
            return Optional.empty();
        }

        // Build SVG from event seats and reservations
        Event event = emailSeatMapToken.getEvent();
        if (event == null || event.getEventLocation() == null) {
            LOG.warnf("Token %s has no event or event location", token);
            return Optional.empty();
        }

        List<Seat> allSeats = event.getEventLocation().getSeats();
        Set<String> newReservedSeatNumbers = emailSeatMapToken.getNewReservedSeatNumbers();
        Set<String> existingReservedSeatNumbers =
                emailSeatMapToken.getUser().getReservations().stream()
                        .filter(r -> r.getEvent().equals(event))
                        .map(Reservation::getSeat)
                        .map(Seat::getSeatNumber)
                        .collect(java.util.stream.Collectors.toSet());
        Collection<EventLocationMarker> markers = event.getEventLocation().getMarkers();

        return Optional.of(
                SvgRenderer.renderSeats(
                        allSeats, newReservedSeatNumbers, existingReservedSeatNumbers, markers));
    }
}
