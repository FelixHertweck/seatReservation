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
package de.felixhertweck.seatreservation.model.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * A WebAuthn (passkey) credential registered by a user. Mirrors the fields the Quarkus WebAuthn
 * extension needs to persist ({@code WebAuthnCredentialRecord.RequiredPersistedData}), plus
 * user-facing metadata for credential management.
 */
@Entity
@Table(name = "webauthn_credentials")
public class WebAuthnCredential extends PanacheEntity {

    /** Base64url-encoded credential id, unique across all users. */
    @Column(nullable = false, unique = true, length = 1024)
    private String credentialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Stored as bytea (small COSE public key); not a @Lob so it can be read outside a transaction.
    @Column(nullable = false)
    private byte[] publicKey;

    private long publicKeyAlgorithm;

    /** Signature counter, updated on every successful assertion. */
    private long counter;

    private UUID aaguid;

    /** Optional user-facing name for this passkey. */
    private String label;

    private Instant createdAt;

    private Instant lastUsedAt;

    public WebAuthnCredential() {}

    public WebAuthnCredential(
            String credentialId,
            User user,
            byte[] publicKey,
            long publicKeyAlgorithm,
            long counter,
            UUID aaguid,
            String label,
            Instant createdAt) {
        this.credentialId = credentialId;
        this.user = user;
        this.publicKey = publicKey;
        this.publicKeyAlgorithm = publicKeyAlgorithm;
        this.counter = counter;
        this.aaguid = aaguid;
        this.label = label;
        this.createdAt = createdAt;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public long getPublicKeyAlgorithm() {
        return publicKeyAlgorithm;
    }

    public void setPublicKeyAlgorithm(long publicKeyAlgorithm) {
        this.publicKeyAlgorithm = publicKeyAlgorithm;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public UUID getAaguid() {
        return aaguid;
    }

    public void setAaguid(UUID aaguid) {
        this.aaguid = aaguid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
