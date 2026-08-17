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
package de.felixhertweck.seatreservation.notification.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.model.entity.UserPushSubscription;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/** Service for Web Push notification management and VAPID key handling. */
@ApplicationScoped
public class WebPushService {

    private static final Logger LOG = Logger.getLogger(WebPushService.class);

    /** Push services reject VAPID JWTs whose expiry is more than 24h out; 12h leaves margin. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private static final long TTL_SECONDS = Duration.ofDays(1).toSeconds();

    @Inject
    @ConfigProperty(name = "seatreservation.vapid.public-key")
    Optional<String> configuredPublicKey;

    @Inject
    @ConfigProperty(name = "seatreservation.vapid.private-key")
    Optional<String> configuredPrivateKey;

    @Inject
    @ConfigProperty(
            name = "seatreservation.vapid.subject",
            defaultValue = "mailto:admin@example.com")
    String vapidSubject;

    @Inject ObjectMapper objectMapper;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    private String publicKeyBase64Url;
    private PrivateKey vapidPrivateKey;

    @PostConstruct
    void initKeys() {
        if (configuredPublicKey.isPresent()
                && !configuredPublicKey.get().isBlank()
                && configuredPrivateKey.isPresent()
                && !configuredPrivateKey.get().isBlank()) {
            try {
                this.publicKeyBase64Url = configuredPublicKey.get().trim();
                byte[] rawPrivateKey =
                        Base64.getUrlDecoder().decode(configuredPrivateKey.get().trim());
                this.vapidPrivateKey = WebPushCrypto.decodePrivateKey(rawPrivateKey);
                LOG.info("VAPID keys loaded from configuration.");
                return;
            } catch (Exception e) {
                LOG.error(
                        "Configured VAPID keys are invalid; falling back to an ephemeral key pair",
                        e);
            }
        }
        generateEphemeralKeys();
    }

    private void generateEphemeralKeys() {
        try {
            KeyPair keyPair = WebPushCrypto.generateKeyPair();
            this.vapidPrivateKey = keyPair.getPrivate();
            this.publicKeyBase64Url =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    WebPushCrypto.encodeUncompressedPoint(
                                            (ECPublicKey) keyPair.getPublic()));
            LOG.warn(
                    "Auto-generated an ephemeral VAPID key pair for Web Push notifications. This"
                            + " key is lost on restart, invalidating every existing browser"
                            + " subscription - configure seatreservation.vapid.public-key and"
                            + " seatreservation.vapid.private-key for a stable identity.");
        } catch (GeneralSecurityException e) {
            LOG.error(
                    "Failed to generate a VAPID key pair; Web Push notifications are disabled", e);
            this.publicKeyBase64Url = "";
            this.vapidPrivateKey = null;
        }
    }

    /** Returns the server's Base64URL-encoded VAPID Public Key. */
    public String getPublicKey() {
        return publicKeyBase64Url;
    }

    /**
     * Sends a push notification payload to a user's registered browser push subscription.
     *
     * @return the push service's HTTP status code, or -1 if the request could not be sent at all
     *     (e.g. Web Push isn't configured, or the subscription's keys don't decode).
     */
    public int sendPushNotification(
            UserPushSubscription subscription, String title, String message, String actionUrl) {
        if (subscription == null || subscription.getEndpoint() == null) {
            return -1;
        }
        if (vapidPrivateKey == null || publicKeyBase64Url == null || publicKeyBase64Url.isBlank()) {
            LOG.warn("Web Push is not configured; skipping push notification.");
            return -1;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("message", message);
            if (actionUrl != null) {
                payload.put("actionUrl", actionUrl);
            }
            byte[] plaintext = objectMapper.writeValueAsBytes(payload);

            byte[] userPublicKeyRaw = Base64.getUrlDecoder().decode(subscription.getP256dh());
            byte[] authSecret = Base64.getUrlDecoder().decode(subscription.getAuth());
            byte[] body = WebPushCrypto.encrypt(plaintext, userPublicKeyRaw, authSecret);

            String authorization =
                    WebPushCrypto.buildVapidAuthorizationHeader(
                            objectMapper,
                            subscription.getEndpoint(),
                            vapidPrivateKey,
                            publicKeyBase64Url,
                            vapidSubject);

            HttpRequest request =
                    HttpRequest.newBuilder(URI.create(subscription.getEndpoint()))
                            .timeout(REQUEST_TIMEOUT)
                            .header("Content-Type", "application/octet-stream")
                            .header("Content-Encoding", "aes128gcm")
                            .header("TTL", String.valueOf(TTL_SECONDS))
                            .header("Authorization", authorization)
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                            .build();

            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                LOG.debugf("Push notification delivered to %s", maskEndpoint(subscription));
            } else {
                LOG.warnf(
                        "Push service rejected notification to %s: HTTP %d",
                        maskEndpoint(subscription), status);
            }
            return status;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            LOG.error("Failed to send Web Push notification to " + maskEndpoint(subscription), e);
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error(
                    "Interrupted while sending Web Push notification to "
                            + maskEndpoint(subscription),
                    e);
            return -1;
        }
    }

    /**
     * Returns a truncated form of the subscription's endpoint safe for logging. The full endpoint
     * URL is itself a bearer credential (whoever holds it can be targeted for push delivery
     * disruption or, per the browser push spec, requires no further auth to receive pushes on it),
     * so full endpoints are never written to logs.
     */
    private static String maskEndpoint(UserPushSubscription subscription) {
        String endpoint = subscription.getEndpoint();
        if (endpoint == null) {
            return "(none)";
        }
        int hostEnd = endpoint.indexOf('/', endpoint.indexOf("://") + 3);
        return (hostEnd > 0 ? endpoint.substring(0, hostEnd) : endpoint) + "/…";
    }
}
