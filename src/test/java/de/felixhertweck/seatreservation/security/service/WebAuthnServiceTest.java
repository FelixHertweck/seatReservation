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
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.model.repository.WebAuthnCredentialRepository;
import de.felixhertweck.seatreservation.security.exceptions.LastCredentialException;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebAuthnServiceTest {

    @Inject WebAuthnService webAuthnService;

    @Inject UserRepository userRepository;

    @Inject WebAuthnCredentialRepository webAuthnCredentialRepository;

    private static byte[] generateEcPublicKeyBytes() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair().getPublic().getEncoded();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Base64url-encoded, matching the format {@code WebAuthnCredentialRecord} expects. */
    private static String randomCredentialId() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static WebAuthnCredentialRecord newRecord(String username) {
        return WebAuthnCredentialRecord.fromRequiredPersistedData(
                new RequiredPersistedData(
                        username,
                        randomCredentialId(),
                        UUID.randomUUID(),
                        generateEcPublicKeyBytes(),
                        -7L,
                        0L));
    }

    private User persistUser(String username, String passwordHash) {
        User user =
                new User(
                        username,
                        username + "@example.com",
                        true,
                        false,
                        passwordHash,
                        null,
                        "First",
                        "Last",
                        Set.of("USER"),
                        Set.of());
        userRepository.persist(user);
        return user;
    }

    private WebAuthnCredential persistCredential(User user) {
        WebAuthnCredential credential =
                new WebAuthnCredential(
                        randomCredentialId(),
                        user,
                        generateEcPublicKeyBytes(),
                        -7L,
                        0L,
                        UUID.randomUUID(),
                        null,
                        Instant.now());
        webAuthnCredentialRepository.persist(credential);
        return credential;
    }

    @Test
    @TestTransaction
    void renameCredential_unknownId_returnsFalse() {
        User user = persistUser("service_rename_unknown", "hash");

        assertFalse(webAuthnService.renameCredential(user, -1L, "New label"));
    }

    @Test
    @TestTransaction
    void renameCredential_wrongOwner_returnsFalse() {
        User owner = persistUser("service_rename_owner", "hash");
        User other = persistUser("service_rename_other", "hash");
        WebAuthnCredential credential = persistCredential(owner);

        assertFalse(webAuthnService.renameCredential(other, credential.id, "Stolen label"));
    }

    @Test
    @TestTransaction
    void deleteCredential_unknownId_returnsFalse() {
        User user = persistUser("service_delete_unknown", "hash");
        persistCredential(user); // keep the account above the "last credential" threshold

        assertFalse(webAuthnService.deleteCredential(user, -1L));
    }

    @Test
    @TestTransaction
    void deleteCredential_lastCredentialWithoutPassword_throws() {
        User user = persistUser("service_delete_last_nopw", null);
        WebAuthnCredential credential = persistCredential(user);

        assertThrows(
                LastCredentialException.class,
                () -> webAuthnService.deleteCredential(user, credential.id));
    }

    @Test
    @TestTransaction
    void deleteCredential_lastCredentialWithPassword_succeeds() {
        User user = persistUser("service_delete_last_pw", "hash");
        WebAuthnCredential credential = persistCredential(user);

        assertTrue(webAuthnService.deleteCredential(user, credential.id));
        assertEquals(0, webAuthnCredentialRepository.countByUser(user));
    }

    @Test
    @TestTransaction
    void addCredentialToUser_blankLabel_isStoredAsNull() {
        User user = persistUser("service_blank_label", "hash");
        WebAuthnCredentialRecord credentialRecord = newRecord("service_blank_label");

        webAuthnService.addCredentialToUser(user, credentialRecord, "   ");

        WebAuthnCredential stored = webAuthnCredentialRepository.findAllByUser(user).get(0);
        assertNull(stored.getLabel());
    }

    @Test
    @TestTransaction
    void addCredentialToUser_labelWithWhitespace_isTrimmed() {
        User user = persistUser("service_trim_label", "hash");
        WebAuthnCredentialRecord credentialRecord = newRecord("service_trim_label");

        webAuthnService.addCredentialToUser(user, credentialRecord, "  My Laptop  ");

        WebAuthnCredential stored = webAuthnCredentialRepository.findAllByUser(user).get(0);
        assertEquals("My Laptop", stored.getLabel());
    }
}
