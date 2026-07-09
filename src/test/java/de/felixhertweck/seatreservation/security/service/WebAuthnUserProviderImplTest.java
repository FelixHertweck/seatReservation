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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.model.repository.WebAuthnCredentialRepository;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebAuthnUserProviderImplTest {

    @Inject WebAuthnUserProviderImpl provider;

    @Inject UserRepository userRepository;

    @Inject WebAuthnCredentialRepository webAuthnCredentialRepository;

    /**
     * {@link WebAuthnCredentialRecord#fromRequiredPersistedData} parses the public key bytes as a
     * real X.509-encoded EC key, so tests need a structurally valid key rather than arbitrary
     * bytes.
     */
    private static byte[] generateEcPublicKeyBytes() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair().getPublic().getEncoded();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@link WebAuthnCredentialRecord#fromRequiredPersistedData} base64url-decodes the credential
     * ID, so it must be valid base64url rather than an arbitrary label.
     */
    private static String randomCredentialId() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private User persistUser(String username) {
        User user =
                new User(
                        username,
                        username + "@example.com",
                        true,
                        false,
                        null,
                        null,
                        "First",
                        "Last",
                        Set.of("USER"),
                        Set.of());
        userRepository.persist(user);
        return user;
    }

    private WebAuthnCredential persistCredential(User user, String credentialId) {
        WebAuthnCredential credential =
                new WebAuthnCredential(
                        credentialId,
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
    void findByUsername_knownUser_returnsRecords() {
        User user = persistUser("provider_known");
        persistCredential(user, randomCredentialId());
        persistCredential(user, randomCredentialId());

        List<WebAuthnCredentialRecord> records =
                provider.findByUsername("provider_known").await().indefinitely();

        assertEquals(2, records.size());
    }

    @Test
    @TestTransaction
    void findByUsername_unknownUser_returnsEmptyList() {
        List<WebAuthnCredentialRecord> records =
                provider.findByUsername("does-not-exist").await().indefinitely();

        assertTrue(records.isEmpty());
    }

    @Test
    @TestTransaction
    void findByCredentialId_knownCredential_returnsRecord() {
        User user = persistUser("provider_findcred");
        String credentialId = randomCredentialId();
        persistCredential(user, credentialId);

        WebAuthnCredentialRecord credentialRecord =
                provider.findByCredentialId(credentialId).await().indefinitely();

        assertEquals("provider_findcred", credentialRecord.getRequiredPersistedData().username());
    }

    @Test
    @TestTransaction
    void findByCredentialId_unknownCredential_returnsNull() {
        WebAuthnCredentialRecord credentialRecord =
                provider.findByCredentialId("does-not-exist").await().indefinitely();

        assertNull(credentialRecord);
    }

    @Test
    @TestTransaction
    void store_knownUser_persistsCredential() {
        User user = persistUser("provider_store");
        String credentialId = randomCredentialId();
        WebAuthnCredentialRecord credentialRecord =
                WebAuthnCredentialRecord.fromRequiredPersistedData(
                        new RequiredPersistedData(
                                "provider_store",
                                credentialId,
                                UUID.randomUUID(),
                                generateEcPublicKeyBytes(),
                                -7L,
                                0L));

        provider.store(credentialRecord).await().indefinitely();

        WebAuthnCredential stored = webAuthnCredentialRepository.findByCredentialId(credentialId);
        assertEquals(user.id, stored.getUser().id);
    }

    @Test
    @TestTransaction
    void store_unknownUser_throwsIllegalStateException() {
        WebAuthnCredentialRecord credentialRecord =
                WebAuthnCredentialRecord.fromRequiredPersistedData(
                        new RequiredPersistedData(
                                "does-not-exist",
                                randomCredentialId(),
                                UUID.randomUUID(),
                                generateEcPublicKeyBytes(),
                                -7L,
                                0L));

        assertThrows(IllegalStateException.class, () -> provider.store(credentialRecord));
    }

    @Test
    @TestTransaction
    void update_knownCredential_updatesCounterAndLastUsedAt() {
        User user = persistUser("provider_update");
        String credentialId = randomCredentialId();
        WebAuthnCredential credential = persistCredential(user, credentialId);
        assertEquals(0L, credential.getCounter());

        provider.update(credentialId, 42L).await().indefinitely();

        WebAuthnCredential updated = webAuthnCredentialRepository.findByCredentialId(credentialId);
        assertEquals(42L, updated.getCounter());
        assertNotNull(updated.getLastUsedAt());
    }

    @Test
    @TestTransaction
    void update_unknownCredential_isNoOp() {
        provider.update("does-not-exist", 42L).await().indefinitely();

        assertNull(webAuthnCredentialRepository.findByCredentialId("does-not-exist"));
    }

    @Test
    @TestTransaction
    void getRoles_knownUser_returnsRoles() {
        persistUser("provider_roles");

        Set<String> roles = provider.getRoles("provider_roles");

        assertEquals(Set.of("USER"), roles);
    }

    @Test
    @TestTransaction
    void getRoles_unknownUser_returnsEmptySet() {
        Set<String> roles = provider.getRoles("does-not-exist");

        assertTrue(roles.isEmpty());
    }
}
