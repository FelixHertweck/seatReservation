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

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.common.events.EventCancelledEvent;
import de.felixhertweck.seatreservation.common.events.EventCreatedEvent;
import de.felixhertweck.seatreservation.common.events.EventDeletedEvent;
import de.felixhertweck.seatreservation.common.events.EventUpdatedEvent;
import de.felixhertweck.seatreservation.common.events.ReservationCancelledEvent;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Generates a Google Wallet "savetowallet" JWT for an EventTicketObject. The JWT is signed with
 * RS256 using the Google Service Account private key read from {@code
 * wallet.google.service-account-key-path} (a PEM-encoded RSA private key). Google validates the JWT
 * signature against the public key registered for that service account, so using the application's
 * own JWT key would always result in INVALID_SIGNATURE.
 */
@ApplicationScoped
public class GoogleWalletPassGenerator extends AbstractWalletPassGenerator {

    private static final Logger LOG = Logger.getLogger(GoogleWalletPassGenerator.class);

    @ConfigProperty(name = "wallet.google.enabled", defaultValue = "false")
    boolean googleWalletEnabled;

    public boolean isEnabled() {
        return googleWalletEnabled;
    }

    @ConfigProperty(name = "wallet.google.issuer-id", defaultValue = "3388000000001234567")
    String issuerId;

    @ConfigProperty(
            name = "wallet.google.service-account-email",
            defaultValue = "wallet-sa@project.iam.gserviceaccount.com")
    String serviceAccountEmail;

    /**
     * Path to the PEM-encoded RSA private key of the Google Service Account (the {@code
     * private_key} field from the downloaded JSON key, written to a stand-alone PEM file). Example:
     * {@code keys/google-wallet-sa.pem}.
     */
    @ConfigProperty(
            name = "wallet.google.service-account-key-path",
            defaultValue = "keys/google-wallet-sa.pem")
    String serviceAccountKeyPath;

    /**
     * Comma-separated list of allowed origins for the "Save to Google Wallet" web button. Google
     * rejects the JWT if this is missing or does not include the domain that renders the button.
     * Example: {@code https://seatreservation.example.com}
     */
    @ConfigProperty(
            name = "wallet.google.allowed-origins",
            defaultValue = "http://localhost:3000,http://localhost:8080")
    String allowedOrigins;

    /** Optional publicly accessible HTTPS URL to the logo image shown on the pass */
    @ConfigProperty(name = "wallet.google.logo-uri")
    java.util.Optional<String> logoUri;

    /** BCP-47 language tag for Google Wallet localized text fields (e.g. en-US, de-DE) */
    @ConfigProperty(name = "wallet.google.default-language", defaultValue = "en-US")
    String defaultLanguage;

    /**
     * Google Wallet class review status (e.g. UNDER_REVIEW, APPROVED, DRAFT). Defaults to
     * UNDER_REVIEW for demo/developer mode accounts. Set to APPROVED for approved production
     * accounts.
     */
    @ConfigProperty(name = "wallet.google.review-status", defaultValue = "UNDER_REVIEW")
    String reviewStatus;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public WalletProvider getProvider() {
        return WalletProvider.GOOGLE;
    }

    /** Convenience overload for a single reservation — delegates to the multi-seat list method. */
    @Override
    public WalletPassResponseDTO generatePass(WalletPassData data) {
        return generatePass(List.of(data));
    }

    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String LANGUAGE = "language";
    private static final String VALUE = "value";
    private static final String EVENT_NAME = "eventName";
    private static final String VENUE = "venue";
    private static final String ADDRESS = "address";
    private static final String DATE_TIME = "dateTime";
    private static final String START = "start";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REVIEW_STATUS = "reviewStatus";
    private static final String STATE = "state";
    private static final String STATE_EXPIRED = "EXPIRED";
    private static final String PATCH_METHOD = "PATCH";
    private static final String EVENT_CLASS_ID_FORMAT = "%s.event_%s";

    private Map<String, Object> localizedString(String text) {
        return Map.of(DEFAULT_VALUE, Map.of(LANGUAGE, defaultLanguage, VALUE, text));
    }

    @Override
    public WalletPassResponseDTO generatePass(List<WalletPassData> allSeatReservations) {
        if (allSeatReservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations provided");
        }
        WalletPassData first = allSeatReservations.get(0);
        String safeEventId = first.eventId().toString().replace("-", "_");
        String fullClassId = String.format(EVENT_CLASS_ID_FORMAT, issuerId, safeEventId);

        LOG.infof(
                "Generating Google Wallet Pass for %d seat(s), event ID: %s, class ID: %s",
                allSeatReservations.size(), first.eventId(), fullClassId);

        Map<String, Object> eventTicketClass = buildEventTicketClass(fullClassId, first);
        String sharedQrPayload = buildQrCodePayload(first);
        List<Map<String, Object>> eventTicketObjects =
                buildEventTicketObjects(fullClassId, allSeatReservations, sharedQrPayload);

        LOG.infof(
                "Built Google Wallet pass payload for event ID: %s with %d ticket object(s)",
                first.eventId(), eventTicketObjects.size());

        try {
            Map<String, Object> payloadClaims = new HashMap<>();
            payloadClaims.put("iss", serviceAccountEmail);
            payloadClaims.put("aud", "google");
            payloadClaims.put("typ", "savetowallet");
            payloadClaims.put("iat", Instant.now().getEpochSecond());
            payloadClaims.put("origins", List.of(allowedOrigins.split(",")));
            payloadClaims.put(
                    "payload",
                    Map.of(
                            "eventTicketClasses",
                            List.of(eventTicketClass),
                            "eventTicketObjects",
                            eventTicketObjects));

            String jwtToken = buildSignedJwt(payloadClaims);
            String googleSaveUrl = "https://pay.google.com/gp/v/save/" + jwtToken;
            LOG.infof(
                    "Successfully generated Google Wallet JWT save URL for event ID: %s (%d"
                            + " seat(s))",
                    first.eventId(), allSeatReservations.size());
            return WalletPassResponseDTO.forGoogle(googleSaveUrl);
        } catch (Exception e) {
            LOG.errorf(e, "Error generating Google Wallet JWT for event ID %s", first.eventId());
            throw new RuntimeException("Failed to generate Google Wallet pass", e);
        }
    }

    private Map<String, Object> buildEventTicketClass(String fullClassId, WalletPassData first) {
        Map<String, Object> eventTicketClass = new HashMap<>();
        eventTicketClass.put("id", fullClassId);
        eventTicketClass.put("issuerName", "SeatReservation");
        eventTicketClass.put(REVIEW_STATUS, reviewStatus);

        if (first.eventName() != null) {
            eventTicketClass.put(EVENT_NAME, localizedString(first.eventName()));
        }

        if (first.locationName() != null || first.locationAddress() != null) {
            Map<String, Object> venueMap = new HashMap<>();
            if (first.locationName() != null) {
                venueMap.put("name", localizedString(first.locationName()));
            }
            if (first.locationAddress() != null) {
                venueMap.put(ADDRESS, localizedString(first.locationAddress()));
            }
            eventTicketClass.put(VENUE, venueMap);
        }

        if (first.startTime() != null) {
            Map<String, Object> dateTimeMap = new HashMap<>();
            dateTimeMap.put(START, first.startTime().toString());
            if (first.endTime() != null) {
                dateTimeMap.put("end", first.endTime().toString());
            }
            eventTicketClass.put(DATE_TIME, dateTimeMap);
        }

        if (logoUri.isPresent() && !logoUri.get().isBlank()) {
            eventTicketClass.put("logo", Map.of("sourceUri", Map.of("uri", logoUri.get().trim())));
        }

        return eventTicketClass;
    }

    private List<Map<String, Object>> buildEventTicketObjects(
            String fullClassId, List<WalletPassData> reservations, String sharedQrPayload) {
        List<Map<String, Object>> eventTicketObjects = new ArrayList<>();
        for (WalletPassData data : reservations) {
            String safeReservationId = data.reservationId().toString().replace("-", "_");
            String objectId = String.format("%s.reservation_%s", issuerId, safeReservationId);

            Map<String, Object> obj = new HashMap<>();
            obj.put("id", objectId);
            obj.put("classId", fullClassId);
            obj.put(STATE, "ACTIVE");

            if (data.seatName() != null || data.rowName() != null || data.sectionName() != null) {
                Map<String, Object> seatInfoMap = new HashMap<>();
                if (data.seatName() != null) {
                    seatInfoMap.put("seat", localizedString(data.seatName()));
                }
                if (data.rowName() != null) {
                    seatInfoMap.put("row", localizedString(data.rowName()));
                }
                if (data.sectionName() != null) {
                    seatInfoMap.put("section", localizedString(data.sectionName()));
                }
                obj.put("seatInfo", seatInfoMap);
            }

            if (data.userName() != null) {
                obj.put("ticketHolderName", data.userName());
            }

            obj.put("barcode", Map.of("type", "QR_CODE", VALUE, sharedQrPayload));
            eventTicketObjects.add(obj);
        }
        return eventTicketObjects;
    }

    /**
     * Asynchronously observes event creation notifications. If Google Wallet pass generation is
     * enabled, immediately creates the corresponding EventTicketClass on the Google Wallet REST
     * API.
     */
    public void onEventCreated(@ObservesAsync EventCreatedEvent event) {
        if (!isEnabled()) {
            LOG.debugf(
                    "Google Wallet is disabled (wallet.google.enabled=false). Skipping"
                            + " EventCreatedEvent for event ID: %s",
                    event.eventId());
            return;
        }
        LOG.infof(
                "Observed EventCreatedEvent for event ID: %s ('%s'). Creating Google Wallet"
                        + " EventTicketClass...",
                event.eventId(), event.eventName());
        try {
            String safeEventId = event.eventId().toString().replace("-", "_");
            String fullClassId = String.format(EVENT_CLASS_ID_FORMAT, issuerId, safeEventId);
            String accessToken = fetchAccessToken();
            insertEventTicketClass(
                    fullClassId,
                    event.eventName(),
                    event.locationName(),
                    event.locationAddress(),
                    event.startTime(),
                    event.endTime(),
                    accessToken);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(
                    e,
                    "Failed to create Google Wallet EventTicketClass for event ID %s (best-effort)",
                    event.eventId());
        }
    }

    /**
     * Asynchronously observes event update notifications. If Google Wallet pass generation is
     * enabled, patches the corresponding EventTicketClass on the Google Wallet REST API so existing
     * user tickets automatically show the updated event details.
     */
    public void onEventUpdated(@ObservesAsync EventUpdatedEvent event) {
        if (!isEnabled()) {
            LOG.debugf(
                    "Google Wallet is disabled (wallet.google.enabled=false). Skipping"
                            + " EventUpdatedEvent for event ID: %s",
                    event.eventId());
            return;
        }
        LOG.infof(
                "Observed EventUpdatedEvent for event ID: %s ('%s'). Updating Google Wallet"
                        + " EventTicketClass...",
                event.eventId(), event.eventName());
        try {
            patchEventTicketClass(event);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(
                    e,
                    "Failed to patch Google Wallet EventTicketClass for event ID %s (best-effort)",
                    event.eventId());
        }
    }

    /**
     * Reacts to a single reservation (or several) being cancelled by expiring the corresponding
     * Google Wallet EventTicketObject(s) asynchronously, so a pass already saved by the user no
     * longer shows as valid.
     */
    public void onReservationCancelled(@Observes ReservationCancelledEvent event) {
        if (!isEnabled()) {
            LOG.debugf(
                    "Google Wallet is disabled (wallet.google.enabled=false). Skipping"
                            + " ReservationCancelledEvent");
            return;
        }
        CompletableFuture.runAsync(
                () -> {
                    List<Reservation> reservations = event.deletedReservations();
                    int count = reservations != null ? reservations.size() : 0;
                    LOG.infof(
                            "Observed ReservationCancelledEvent with %d reservation(s) to expire in"
                                    + " Google Wallet",
                            count);
                    if (reservations != null) {
                        for (Reservation reservation : reservations) {
                            expireEventTicketObjectBestEffort(reservation.id);
                        }
                    }
                });
    }

    /**
     * Reacts to an event being cancelled by expiring all Google Wallet EventTicketObjects for the
     * event.
     */
    public void onEventCancelled(@ObservesAsync EventCancelledEvent event) {
        if (!isEnabled()) {
            LOG.debugf(
                    "Google Wallet is disabled (wallet.google.enabled=false). Skipping"
                            + " EventCancelledEvent for event ID: %s",
                    event.eventId());
            return;
        }
        String safeEventId = event.eventId().toString().replace("-", "_");
        String fullClassId = String.format(EVENT_CLASS_ID_FORMAT, issuerId, safeEventId);

        List<Reservation> reservations = event.cancelledReservations();
        int count = reservations != null ? reservations.size() : 0;
        LOG.infof(
                "Observed EventCancelledEvent for event ID: %s ('%s') with %d reservation(s) to"
                        + " expire in Google Wallet (class ID: %s)",
                event.eventId(), event.eventName(), count, fullClassId);

        try {
            String accessToken = fetchAccessToken();
            expireAllObjectsForClass(fullClassId, accessToken);
            if (reservations != null) {
                for (Reservation reservation : reservations) {
                    expireEventTicketObjectBestEffort(reservation.id);
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(
                    e,
                    "Failed to expire Google Wallet passes for cancelled event ID %s (best-effort)",
                    event.eventId());
        }
    }

    /**
     * Reacts to an event being deleted by expiring all Google Wallet EventTicketObjects for the
     * event.
     */
    public void onEventDeleted(@ObservesAsync EventDeletedEvent event) {
        if (!isEnabled()) {
            LOG.debugf(
                    "Google Wallet is disabled (wallet.google.enabled=false). Skipping"
                            + " EventDeletedEvent for event ID: %s",
                    event.eventId());
            return;
        }
        String safeEventId = event.eventId().toString().replace("-", "_");
        String fullClassId = String.format(EVENT_CLASS_ID_FORMAT, issuerId, safeEventId);

        List<UUID> reservationIds = event.reservationIds();
        int count = reservationIds != null ? reservationIds.size() : 0;
        LOG.infof(
                "Observed EventDeletedEvent for event ID: %s with %d reservation ID(s) to expire"
                        + " in Google Wallet (class ID: %s)",
                event.eventId(), count, fullClassId);

        try {
            String accessToken = fetchAccessToken();
            expireAllObjectsForClass(fullClassId, accessToken);
            if (reservationIds != null) {
                for (UUID reservationId : reservationIds) {
                    expireEventTicketObjectBestEffort(reservationId);
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(
                    e,
                    "Failed to expire Google Wallet passes for deleted event ID %s (best-effort)",
                    event.eventId());
        }
    }

    private void expireEventTicketObjectBestEffort(UUID reservationId) {
        if (reservationId == null) {
            LOG.warn("Cannot expire Google Wallet object: reservation ID is null");
            return;
        }
        try {
            expireEventTicketObject(reservationId);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(
                    e,
                    "Failed to expire Google Wallet EventTicketObject for reservation ID %s"
                            + " (best-effort)",
                    reservationId);
        }
    }

    /**
     * Queries Google Wallet REST API for all EventTicketObject instances belonging to this class
     * ID, and sets their state to EXPIRED.
     */
    private void expireAllObjectsForClass(String fullClassId, String accessToken) {
        try {
            String listUrl =
                    "https://walletobjects.googleapis.com/walletobjects/v1/eventTicketObject?classId="
                            + java.net.URLEncoder.encode(fullClassId, StandardCharsets.UTF_8);

            HttpRequest listRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(listUrl))
                            .header(AUTH_HEADER, BEARER_PREFIX + accessToken)
                            .GET()
                            .build();

            HttpResponse<String> listResponse =
                    httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());

            if (listResponse.statusCode() >= 200 && listResponse.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(listResponse.body());
                JsonNode resources = root.get("resources");
                if (resources != null && resources.isArray()) {
                    LOG.infof(
                            "Found %d Google Wallet ticket object(s) to expire for class ID %s",
                            resources.size(), fullClassId);
                    for (JsonNode resource : resources) {
                        String objectId = resource.has("id") ? resource.get("id").asText() : null;
                        String state = resource.has(STATE) ? resource.get(STATE).asText() : null;
                        if (objectId != null && !STATE_EXPIRED.equalsIgnoreCase(state)) {
                            expireEventTicketObjectById(objectId, accessToken);
                        }
                    }
                } else {
                    LOG.debugf(
                            "No Google Wallet ticket objects found for class ID %s", fullClassId);
                }
            } else if (listResponse.statusCode() == 404) {
                LOG.debugf(
                        "Class ID %s not found on Google Wallet (HTTP 404) during object query",
                        fullClassId);
            } else {
                LOG.warnf(
                        "Google Wallet API returned unexpected status %d when listing objects for"
                                + " class ID %s: %s",
                        listResponse.statusCode(),
                        fullClassId,
                        sanitizeResponseBody(listResponse.body()));
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(
                    e,
                    "Failed to list/expire Google Wallet objects for class ID %s (best-effort)",
                    fullClassId);
        }
    }

    private void expireEventTicketObjectById(String objectId, String accessToken) {
        try {
            String objectUrl =
                    "https://walletobjects.googleapis.com/walletobjects/v1/eventTicketObject/"
                            + objectId;

            String patchBody = objectMapper.writeValueAsString(Map.of(STATE, STATE_EXPIRED));
            HttpRequest patchRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(objectUrl))
                            .header(AUTH_HEADER, BEARER_PREFIX + accessToken)
                            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
                            .method(PATCH_METHOD, HttpRequest.BodyPublishers.ofString(patchBody))
                            .build();

            HttpResponse<String> patchResponse =
                    httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());

            if (patchResponse.statusCode() >= 200 && patchResponse.statusCode() < 300) {
                LOG.debugf(
                        "Successfully expired Google Wallet EventTicketObject for object ID %s"
                                + " (HTTP %d)",
                        objectId, patchResponse.statusCode());
            } else if (patchResponse.statusCode() == 404) {
                LOG.debugf("Google Wallet EventTicketObject %s not found (HTTP 404)", objectId);
            } else {
                LOG.warnf(
                        "Google Wallet API returned unexpected status %d when expiring object ID"
                                + " %s: %s",
                        patchResponse.statusCode(),
                        objectId,
                        sanitizeResponseBody(patchResponse.body()));
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf(e, "Failed to expire Google Wallet object ID %s (best-effort)", objectId);
        }
    }

    /**
     * Patches the Google Wallet EventTicketObject for the given reservation to {@code state:
     * EXPIRED}. Google Wallet has no delete endpoint for objects — expiring the state is the
     * documented way to revoke a pass a user may already have saved. A 404 response means no pass
     * was ever saved for this reservation, which is the common case and not an error.
     */
    private void expireEventTicketObject(UUID reservationId) throws Exception {
        String safeReservationId = reservationId.toString().replace("-", "_");
        String objectId = String.format("%s.reservation_%s", issuerId, safeReservationId);

        LOG.debugf(
                "Attempting to expire Google Wallet EventTicketObject for object ID: %s"
                        + " (reservation ID: %s)",
                objectId, reservationId);

        String accessToken = fetchAccessToken();
        expireEventTicketObjectById(objectId, accessToken);
    }

    /**
     * Upserts the Google Wallet EventTicketClass for the given event: first attempts a PATCH
     * (partial update). If the class does not exist yet on Google's API (404), falls back to a POST
     * (insert) with the full class payload so that the update is not silently lost.
     */
    private void patchEventTicketClass(EventUpdatedEvent event) throws Exception {
        String safeEventId = event.eventId().toString().replace("-", "_");
        String fullClassId = String.format(EVENT_CLASS_ID_FORMAT, issuerId, safeEventId);

        LOG.infof(
                "Patching Google Wallet EventTicketClass for class ID: %s (event ID: %s)",
                fullClassId, event.eventId());

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put(REVIEW_STATUS, reviewStatus);
        if (event.eventName() != null) {
            updatePayload.put(EVENT_NAME, localizedString(event.eventName()));
        }

        if (event.locationName() != null || event.locationAddress() != null) {
            Map<String, Object> venueMap = new HashMap<>();
            if (event.locationName() != null) {
                venueMap.put("name", localizedString(event.locationName()));
            }
            if (event.locationAddress() != null) {
                venueMap.put(ADDRESS, localizedString(event.locationAddress()));
            }
            updatePayload.put(VENUE, venueMap);
        }

        if (event.startTime() != null) {
            Map<String, Object> dateTimeMap = new HashMap<>();
            dateTimeMap.put(START, event.startTime().toString());
            if (event.endTime() != null) {
                dateTimeMap.put("end", event.endTime().toString());
            }
            updatePayload.put(DATE_TIME, dateTimeMap);
        }

        String accessToken = fetchAccessToken();
        String classUrl =
                "https://walletobjects.googleapis.com/walletobjects/v1/eventTicketClass/"
                        + fullClassId;

        String patchBody = objectMapper.writeValueAsString(updatePayload);
        HttpRequest patchRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(classUrl))
                        .header(AUTH_HEADER, BEARER_PREFIX + accessToken)
                        .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
                        .method(PATCH_METHOD, HttpRequest.BodyPublishers.ofString(patchBody))
                        .build();

        HttpResponse<String> patchResponse =
                httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());

        if (patchResponse.statusCode() >= 200 && patchResponse.statusCode() < 300) {
            LOG.infof(
                    "Successfully patched Google Wallet EventTicketClass for class ID %s (HTTP %d)",
                    fullClassId, patchResponse.statusCode());
        } else if (patchResponse.statusCode() == 404) {
            // Class does not exist yet on Google's API — insert it now so the update is not lost.
            LOG.infof(
                    "Google Wallet EventTicketClass %s not found (HTTP 404). Inserting it now...",
                    fullClassId);
            insertEventTicketClass(
                    fullClassId,
                    event.eventName(),
                    event.locationName(),
                    event.locationAddress(),
                    event.startTime(),
                    event.endTime(),
                    accessToken);
        } else {
            LOG.warnf(
                    "Google Wallet API returned unexpected status %d when patching class ID %s:"
                            + " %s",
                    patchResponse.statusCode(),
                    fullClassId,
                    sanitizeResponseBody(patchResponse.body()));
        }
    }

    /**
     * Inserts a new EventTicketClass via POST when it does not yet exist on Google's Wallet API.
     */
    private void insertEventTicketClass(
            String fullClassId,
            String eventName,
            String locationName,
            String locationAddress,
            Instant startTime,
            Instant endTime,
            String accessToken)
            throws Exception {
        Map<String, Object> insertPayload = new HashMap<>();
        insertPayload.put("id", fullClassId);
        insertPayload.put("issuerName", "SeatReservation");
        insertPayload.put(REVIEW_STATUS, reviewStatus);

        if (eventName != null) {
            insertPayload.put(EVENT_NAME, localizedString(eventName));
        }

        if (locationName != null || locationAddress != null) {
            Map<String, Object> venueMap = new HashMap<>();
            if (locationName != null) {
                venueMap.put("name", localizedString(locationName));
            }
            if (locationAddress != null) {
                venueMap.put(ADDRESS, localizedString(locationAddress));
            }
            insertPayload.put(VENUE, venueMap);
        }

        if (startTime != null) {
            Map<String, Object> dateTimeMap = new HashMap<>();
            dateTimeMap.put(START, startTime.toString());
            if (endTime != null) {
                dateTimeMap.put("end", endTime.toString());
            }
            insertPayload.put(DATE_TIME, dateTimeMap);
        }

        if (logoUri.isPresent() && !logoUri.get().isBlank()) {
            insertPayload.put("logo", Map.of("sourceUri", Map.of("uri", logoUri.get().trim())));
        }

        String insertUrl = "https://walletobjects.googleapis.com/walletobjects/v1/eventTicketClass";
        String insertBody = objectMapper.writeValueAsString(insertPayload);
        LOG.infof("Inserting Google Wallet EventTicketClass for class ID: %s", fullClassId);
        HttpRequest insertRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(insertUrl))
                        .header(AUTH_HEADER, BEARER_PREFIX + accessToken)
                        .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
                        .POST(HttpRequest.BodyPublishers.ofString(insertBody))
                        .build();

        HttpResponse<String> insertResponse =
                httpClient.send(insertRequest, HttpResponse.BodyHandlers.ofString());
        if (insertResponse.statusCode() >= 200 && insertResponse.statusCode() < 300) {
            LOG.infof(
                    "Successfully inserted Google Wallet EventTicketClass for class ID %s (HTTP"
                            + " %d)",
                    fullClassId, insertResponse.statusCode());
        } else if (insertResponse.statusCode() == 409) {
            LOG.infof("Google Wallet EventTicketClass %s already exists (HTTP 409).", fullClassId);
        } else {
            LOG.warnf(
                    "Google Wallet API returned unexpected status %d when inserting class ID %s:"
                            + " %s",
                    insertResponse.statusCode(),
                    fullClassId,
                    sanitizeResponseBody(insertResponse.body()));
        }
    }

    private String sanitizeResponseBody(String body) {
        if (body == null) {
            return "";
        }
        if (body.contains("<!DOCTYPE html>") || body.contains("<html")) {
            String clean = body.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            return clean.length() > 300 ? clean.substring(0, 300) + "..." : clean;
        }
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    /** Requests an OAuth2 access token for the Google Service Account via jwt-bearer assertion. */
    private String fetchAccessToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", serviceAccountEmail);
        claims.put("sub", serviceAccountEmail);
        claims.put("aud", "https://oauth2.googleapis.com/token");
        claims.put("scope", "https://www.googleapis.com/auth/wallet_object.issuer");
        claims.put("iat", now);
        claims.put("exp", now + 3600);

        LOG.debugf(
                "Fetching Google OAuth2 access token for service account: %s", serviceAccountEmail);
        String jwtAssertion = buildSignedJwt(claims);

        String formBody =
                "grant_type="
                        + java.net.URLEncoder.encode(
                                        "urn:ietf:params:oauth:grant-type:jwt-bearer",
                                        StandardCharsets.UTF_8)
                                .concat("&assertion=")
                                .concat(
                                        java.net.URLEncoder.encode(
                                                jwtAssertion, StandardCharsets.UTF_8));

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            LOG.warnf(
                    "Google OAuth2 token request failed with status %d for service account %s: %s",
                    response.statusCode(),
                    serviceAccountEmail,
                    sanitizeResponseBody(response.body()));
            throw new IllegalStateException(
                    "OAuth2 token request failed with status "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.get("access_token").asText();
    }

    /**
     * Builds a minimal RS256 JWT manually so we can supply an arbitrary private key rather than
     * being tied to the application's SmallRye JWT signing key.
     */
    private String buildSignedJwt(Map<String, Object> claims) throws Exception {
        PrivateKey privateKey = loadServiceAccountKey();

        // Header
        String header =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                objectMapper
                                        .writeValueAsBytes(Map.of("alg", "RS256", "typ", "JWT"))
                                        .clone());

        // Payload
        String payload =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(objectMapper.writeValueAsBytes(claims));

        String signingInput = header + "." + payload;

        // Sign with RS256 / SHA256withRSA
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signer.sign();
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        return signingInput + "." + signature;
    }

    /** Reads the Service Account private key from the configured PEM file. */
    private PrivateKey loadServiceAccountKey() {
        String pemContent;
        try {
            pemContent = Files.readString(Path.of(serviceAccountKeyPath));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read Google Service Account key from " + serviceAccountKeyPath, e);
        }
        try (PEMParser parser = new PEMParser(new StringReader(pemContent))) {
            Object parsed = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (parsed instanceof PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            } else if (parsed instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }
            throw new IllegalStateException(
                    "Unsupported PEM object type in Google SA key file: "
                            + (parsed == null ? "null" : parsed.getClass().getName()));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to parse Google Service Account key from " + serviceAccountKeyPath, e);
        }
    }
}
