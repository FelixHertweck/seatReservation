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
package de.felixhertweck.seatreservation.security.dto;

import java.time.Instant;

import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import io.quarkus.runtime.annotations.RegisterForReflection;

/** Management view of a registered passkey. Never exposes the public key or credential id. */
@RegisterForReflection
public class WebAuthnCredentialDTO {

    private Long id;
    private String label;
    private Instant createdAt;
    private Instant lastUsedAt;

    public WebAuthnCredentialDTO() {}

    public WebAuthnCredentialDTO(WebAuthnCredential credential) {
        this.id = credential.id;
        this.label = credential.getLabel();
        this.createdAt = credential.getCreatedAt();
        this.lastUsedAt = credential.getLastUsedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
