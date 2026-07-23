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
package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class WebAuthnCredentialRepository
        implements PanacheRepositoryBase<WebAuthnCredential, UUID> {

    /**
     * Find a WebAuthn credential by its base64url-encoded credential id.
     *
     * @param credentialId the credential id to search for
     * @return the credential entity or null if not found
     */
    public WebAuthnCredential findByCredentialId(String credentialId) {
        return find("credentialId", credentialId).firstResult();
    }

    /**
     * Find all WebAuthn credentials belonging to a specific user.
     *
     * @param user the user whose credentials to find
     * @return a list of credentials belonging to the user
     */
    public List<WebAuthnCredential> findAllByUser(User user) {
        return list("user", user);
    }

    /**
     * Count the WebAuthn credentials belonging to a specific user.
     *
     * @param user the user whose credentials to count
     * @return the number of credentials belonging to the user
     */
    public long countByUser(User user) {
        return count("user", user);
    }

    /**
     * Delete a WebAuthn credential by its ID and owning user if it exists.
     *
     * @param id the ID of the credential to delete
     * @param user the user who owns the credential
     * @return true if the credential was deleted, false otherwise
     */
    public boolean deleteWithIdAndUser(UUID id, User user) {
        return delete("id = ?1 and user = ?2", id, user) > 0;
    }

    /**
     * Check whether a WebAuthn credential with the given ID belongs to the given user.
     *
     * @param id the ID of the credential to check
     * @param user the user who should own the credential
     * @return true if such a credential exists
     */
    public boolean existsByIdAndUser(UUID id, User user) {
        return count("id = ?1 and user = ?2", id, user) > 0;
    }
}
