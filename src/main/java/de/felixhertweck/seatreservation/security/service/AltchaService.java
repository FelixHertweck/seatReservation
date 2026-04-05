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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AltchaService {

    private static final Logger LOG = Logger.getLogger(AltchaService.class);
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "altcha.hmac-key", defaultValue = "secret_altcha_key")
    String hmacKey;

    public Map<String, Object> createChallenge() {
        try {
            byte[] saltBytes = new byte[12];
            random.nextBytes(saltBytes);
            String salt = Hex.encodeHexString(saltBytes);

            int number = random.nextInt(10000) + 1000;
            String challenge =
                    Hex.encodeHexString(sha256((salt + number).getBytes(StandardCharsets.UTF_8)));
            String signature = createHmac(challenge);

            return Map.of(
                    "algorithm", "SHA-256",
                    "challenge", challenge,
                    "salt", salt,
                    "signature", signature);
        } catch (Exception e) {
            LOG.error("Failed to create Altcha challenge", e);
            throw new RuntimeException("Could not create Altcha challenge", e);
        }
    }

    public boolean verifyPayload(String altchaPayloadBase64) {
        if (altchaPayloadBase64 == null || altchaPayloadBase64.isBlank()) {
            return false;
        }

        try {
            String payloadJson =
                    new String(
                            Base64.getDecoder().decode(altchaPayloadBase64),
                            StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            String algorithm = (String) payload.get("algorithm");
            String challenge = (String) payload.get("challenge");
            Integer number = (Integer) payload.get("number");
            String salt = (String) payload.get("salt");
            String signature = (String) payload.get("signature");

            if (!"SHA-256".equals(algorithm)) {
                return false;
            }

            // Verify the signature
            String expectedSignature = createHmac(challenge);
            if (!expectedSignature.equals(signature)) {
                return false;
            }

            // Verify the challenge
            String expectedChallenge =
                    Hex.encodeHexString(sha256((salt + number).getBytes(StandardCharsets.UTF_8)));
            if (!expectedChallenge.equals(challenge)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            LOG.warn("Failed to verify Altcha payload", e);
            return false;
        }
    }

    private byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    private String createHmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec =
                new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        return Hex.encodeHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
