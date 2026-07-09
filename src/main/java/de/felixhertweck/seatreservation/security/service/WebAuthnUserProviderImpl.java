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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.model.repository.WebAuthnCredentialRepository;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

/**
 * Bridges the Quarkus WebAuthn extension to our persistence layer. The extension calls this
 * provider to look up and store passkey credentials; we translate between its {@link
 * WebAuthnCredentialRecord} and our {@link WebAuthnCredential} entity.
 *
 * <p>The passkey ceremony is always driven from our blocking REST endpoints in {@code
 * WebAuthnResource}, so these methods run on a worker thread where blocking JDBC/Panache access is
 * allowed. The work is therefore performed synchronously under {@link Transactional} and the result
 * is wrapped in an already-resolved {@link Uni}.
 */
@ApplicationScoped
public class WebAuthnUserProviderImpl implements WebAuthnUserProvider {

    private static final Logger LOG = Logger.getLogger(WebAuthnUserProviderImpl.class);

    @Inject UserRepository userRepository;

    @Inject WebAuthnCredentialRepository webAuthnCredentialRepository;

    @Override
    @Transactional
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String username) {
        User user = userRepository.findByUsername(username);
        List<WebAuthnCredentialRecord> records =
                user == null
                        ? List.of()
                        : webAuthnCredentialRepository.findAllByUser(user).stream()
                                .map(WebAuthnUserProviderImpl::toRecord)
                                .toList();
        return Uni.createFrom().item(records);
    }

    @Override
    @Transactional
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        WebAuthnCredential credential =
                webAuthnCredentialRepository.findByCredentialId(credentialId);
        return Uni.createFrom().item(credential == null ? null : toRecord(credential));
    }

    @Override
    @Transactional
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        RequiredPersistedData data = credentialRecord.getRequiredPersistedData();
        User user = userRepository.findByUsername(data.username());
        if (user == null) {
            throw new IllegalStateException(
                    "Cannot store passkey for unknown user: " + data.username());
        }
        WebAuthnCredential credential =
                new WebAuthnCredential(
                        data.credentialId(),
                        user,
                        data.publicKey(),
                        data.publicKeyAlgorithm(),
                        data.counter(),
                        data.aaguid(),
                        null,
                        Instant.now());
        webAuthnCredentialRepository.persist(credential);
        LOG.debugf("Stored passkey for user ID: %d", user.id);
        return Uni.createFrom().voidItem();
    }

    @Override
    @Transactional
    public Uni<Void> update(String credentialId, long counter) {
        WebAuthnCredential credential =
                webAuthnCredentialRepository.findByCredentialId(credentialId);
        if (credential != null) {
            credential.setCounter(counter);
            credential.setLastUsedAt(Instant.now());
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    @Transactional
    public Set<String> getRoles(String username) {
        User user = userRepository.findByUsername(username);
        return user == null ? Set.of() : Set.copyOf(user.getRoles());
    }

    private static WebAuthnCredentialRecord toRecord(WebAuthnCredential credential) {
        return WebAuthnCredentialRecord.fromRequiredPersistedData(
                new RequiredPersistedData(
                        credential.getUser().getUsername(),
                        credential.getCredentialId(),
                        credential.getAaguid(),
                        credential.getPublicKey(),
                        credential.getPublicKeyAlgorithm(),
                        credential.getCounter()));
    }
}
