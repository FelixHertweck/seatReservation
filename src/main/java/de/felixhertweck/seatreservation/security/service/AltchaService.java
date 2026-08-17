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
package de.felixhertweck.seatreservation.security.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.security.exceptions.AltchaVerificationException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.altcha.altcha.v1.Altcha;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AltchaService {

    private static final Logger LOG = Logger.getLogger(AltchaService.class);
    private static final String REDIS_KEY_PREFIX = "altcha:used:";

    @ConfigProperty(name = "altcha.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "altcha.hmac-key")
    String hmacKey;

    @ConfigProperty(name = "altcha.max-number", defaultValue = "100000")
    long maxNumber;

    @ConfigProperty(name = "altcha.expires-seconds", defaultValue = "300")
    long expiresSeconds;

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public AltchaService(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key(String.class);
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a new ALTCHA challenge.
     *
     * @return the generated ALTCHA Challenge object
     */
    public Altcha.Challenge createChallenge() {
        try {
            Altcha.ChallengeOptions options =
                    new Altcha.ChallengeOptions()
                            .hmacKey(hmacKey)
                            .maxNumber(maxNumber)
                            .expiresInSeconds((int) expiresSeconds);
            return Altcha.createChallenge(options);
        } catch (Exception e) {
            LOG.error("Failed to generate ALTCHA challenge", e);
            throw new IllegalStateException("Could not generate CAPTCHA challenge", e);
        }
    }

    /**
     * Verifies the provided ALTCHA payload's proof-of-work solution, without consuming it. Useful
     * to gate a preliminary step of a multi-step flow (e.g. WebAuthn registration options) on a
     * valid solution while leaving the final, sensitive step to perform replay-protected
     * consumption via {@link #verifyAndConsume(String)} with the same payload.
     *
     * @param altchaPayload the base64-encoded ALTCHA solution payload from the client
     * @throws AltchaVerificationException if verification fails
     */
    public void verify(String altchaPayload) {
        if (!enabled) {
            LOG.debug("ALTCHA verification is disabled, skipping check");
            return;
        }

        if (altchaPayload == null || altchaPayload.isBlank()) {
            LOG.warn("ALTCHA verification failed: payload is missing or blank");
            throw new AltchaVerificationException("Missing CAPTCHA verification payload");
        }

        boolean valid;
        try {
            valid = Altcha.verifySolution(altchaPayload, hmacKey, true);
        } catch (Exception e) {
            LOG.warnf("ALTCHA verifySolution threw exception: %s", e.getMessage());
            throw new AltchaVerificationException("Invalid CAPTCHA solution format", e);
        }

        if (!valid) {
            LOG.warn("ALTCHA verification failed: invalid solution or expired");
            throw new AltchaVerificationException("Invalid or expired CAPTCHA solution");
        }
    }

    /**
     * Verifies the provided ALTCHA payload and ensures it has not been used previously (replay
     * protection).
     *
     * @param altchaPayload the base64-encoded ALTCHA solution payload from the client
     * @throws AltchaVerificationException if verification fails or token is replayed
     */
    public void verifyAndConsume(String altchaPayload) {
        verify(altchaPayload);
        if (!enabled) {
            return;
        }

        // Extract signature for replay prevention
        String signature = extractSignature(altchaPayload);
        if (signature == null || signature.isBlank()) {
            LOG.warn("ALTCHA payload signature is missing");
            throw new AltchaVerificationException("Invalid CAPTCHA payload signature");
        }

        // Atomic check-and-set in Redis (NX = only set if key does not exist)
        String redisKey = REDIS_KEY_PREFIX + signature;
        boolean wasSet = valueCommands.setnx(redisKey, "1");

        if (!wasSet) {
            LOG.warnf("ALTCHA replay detected for signature: %s", signature);
            throw new AltchaVerificationException("CAPTCHA solution has already been used");
        }

        keyCommands.expire(redisKey, Duration.ofSeconds(expiresSeconds));
    }

    private String extractSignature(String base64Payload) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Payload.trim());
            JsonNode root = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            if (root.hasNonNull("signature")) {
                return root.get("signature").asText();
            }
        } catch (Exception e) {
            LOG.warnf("Failed to decode ALTCHA payload JSON: %s", e.getMessage());
        }
        return null;
    }
}
