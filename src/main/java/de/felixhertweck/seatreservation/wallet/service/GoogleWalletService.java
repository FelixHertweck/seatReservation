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

package de.felixhertweck.seatreservation.wallet.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GoogleWalletService {

    private static final Logger LOG = Logger.getLogger(GoogleWalletService.class);

    @Inject EmailSeatMapTokenRepository tokenRepository;

    @ConfigProperty(name = "email.google.wallet.issuer-id")
    String issuerId;

    @ConfigProperty(name = "email.google.wallet.key.location")
    String keyLocation;

    @ConfigProperty(name = "email.google.wallet.service-account-email")
    String serviceAccountEmail;

    @Inject ObjectMapper objectMapper;

    public String createGoogleWalletJwt(String token) {
        EmailSeatMapToken seatMapToken =
                tokenRepository
                        .findByToken(token)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (seatMapToken.getExpirationTime() != null
                && seatMapToken.getExpirationTime().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        Event event = seatMapToken.getEvent();
        User user = seatMapToken.getUser();

        List<Reservation> reservations =
                seatMapToken.getUser().getReservations().stream()
                        .filter(r -> r.getEvent().equals(event))
                        .collect(Collectors.toList());

        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations found for this token");
        }

        try {
            return generateJwt(user, event, reservations);
        } catch (Exception e) {
            LOG.error("Failed to generate Google Wallet JWT", e);
            throw new RuntimeException("Failed to generate Google Wallet JWT", e);
        }
    }

    private String generateJwt(User user, Event event, List<Reservation> reservations)
            throws Exception {
        PrivateKey privateKey = loadPrivateKey(keyLocation);

        String classId = issuerId + ".event-" + event.id;
        String objectId =
                issuerId + "." + user.id + "-" + event.id + "-" + System.currentTimeMillis();

        // Construct the EventTicketClass
        Map<String, Object> eventTicketClass =
                Map.of(
                        "id",
                        classId,
                        "issuerName",
                        "Seat Reservation System",
                        "eventName",
                        Map.of(
                                "defaultValue",
                                Map.of("language", "en-US", "value", event.getName())),
                        "reviewStatus",
                        "UNDER_REVIEW" // Required for testing without approval
                        );
        // Add location if available
        if (event.getEventLocation() != null) {
            // We can't easily map the full location structure without more data,
            // but we can put the name in the class or object.
            // For simplicity, let's keep the class minimal and put details in the
            // object/modules.
        }

        // Construct the EventTicketObject
        Map<String, Object> eventTicketObject =
                Map.of(
                        "id",
                        objectId,
                        "classId",
                        classId,
                        "state",
                        "ACTIVE",
                        "heroImage",
                        Map.of("sourceUri", Map.of("uri", "https://via.placeholder.com/300x100")),
                        "textModulesData",
                        List.of(
                                Map.of(
                                        "header",
                                        "Location",
                                        "body",
                                        event.getEventLocation().getName()),
                                Map.of(
                                        "header",
                                        "Start Time",
                                        "body",
                                        event.getStartTime().toString()),
                                Map.of(
                                        "header",
                                        "Seats",
                                        "body",
                                        reservations.stream()
                                                .map(r -> r.getSeat().getSeatNumber())
                                                .collect(Collectors.joining(", ")))),
                        "barcode",
                        Map.of("type", "QR_CODE", "value", "CHECKIN:" + user.id + ":" + event.id),
                        "hexBackgroundColor",
                        "#4285f4");

        Map<String, Object> payload =
                Map.of(
                        "iss",
                        serviceAccountEmail,
                        "aud",
                        "google",
                        "typ",
                        "savetowallet",
                        "iat",
                        Instant.now().getEpochSecond(),
                        "origins",
                        List.of(),
                        "payload",
                        Map.of(
                                "eventTicketClasses", List.of(eventTicketClass),
                                "eventTicketObjects", List.of(eventTicketObject)));

        JwtClaimsBuilder builder = Jwt.claims();
        payload.forEach(builder::claim);

        return builder.jws().sign(privateKey);
    }

    private PrivateKey loadPrivateKey(String filename) throws Exception {
        String keyContent = Files.readString(Path.of(filename));
        keyContent =
                keyContent
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}
