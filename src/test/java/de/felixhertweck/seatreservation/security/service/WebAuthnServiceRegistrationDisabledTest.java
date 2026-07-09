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
package de.felixhertweck.seatreservation.security.service;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertThrows;

import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.security.dto.WebAuthnRegistrationStartDTO;
import de.felixhertweck.seatreservation.security.resource.AuthResourceTest;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

/**
 * Registration can only be disabled via the {@code registration.enabled} config property, which is
 * read once at bean construction time. That requires a dedicated {@link TestProfile} rather than
 * flipping a field on the injected (proxied) {@link AuthService} at runtime.
 */
@QuarkusTest
@TestProfile(AuthResourceTest.DisabledRegistrationProfile.class)
class WebAuthnServiceRegistrationDisabledTest {

    @Inject WebAuthnService webAuthnService;

    private static byte[] generateEcPublicKeyBytes() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair().getPublic().getEncoded();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void createUserWithCredential_registrationDisabled_throws() {
        WebAuthnRegistrationStartDTO registration = new WebAuthnRegistrationStartDTO();
        registration.setUsername("service_reg_disabled");
        registration.setFirstname("Ada");
        registration.setLastname("Lovelace");
        registration.setEmail("service_reg_disabled@example.com");

        byte[] credentialIdBytes = new byte[12];
        new SecureRandom().nextBytes(credentialIdBytes);
        WebAuthnCredentialRecord credentialRecord =
                WebAuthnCredentialRecord.fromRequiredPersistedData(
                        new RequiredPersistedData(
                                "service_reg_disabled",
                                Base64.getUrlEncoder()
                                        .withoutPadding()
                                        .encodeToString(credentialIdBytes),
                                UUID.randomUUID(),
                                generateEcPublicKeyBytes(),
                                -7L,
                                0L));

        assertThrows(
                RegistrationDisabledException.class,
                () ->
                        webAuthnService.createUserWithCredential(
                                registration, credentialRecord, null));
    }
}
