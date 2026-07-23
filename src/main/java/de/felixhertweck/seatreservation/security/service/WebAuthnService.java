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
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import de.felixhertweck.seatreservation.model.repository.LoginAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.model.repository.WebAuthnCredentialRepository;
import de.felixhertweck.seatreservation.security.dto.WebAuthnCredentialDTO;
import de.felixhertweck.seatreservation.security.dto.WebAuthnRegistrationStartDTO;
import de.felixhertweck.seatreservation.security.dto.WebAuthnStatusDTO;
import de.felixhertweck.seatreservation.security.exceptions.LastCredentialException;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import org.jboss.logging.Logger;

/**
 * Persistence-facing orchestration for the passkey (WebAuthn) flows. The cryptographic ceremony
 * itself is handled by the Quarkus WebAuthn extension in {@code WebAuthnResource}; this service
 * owns everything that touches our own entities (user creation, credential storage, credential
 * management).
 */
@ApplicationScoped
public class WebAuthnService {

    private static final Logger LOG = Logger.getLogger(WebAuthnService.class);

    @Inject WebAuthnCredentialRepository webAuthnCredentialRepository;

    @Inject UserRepository userRepository;

    @Inject UserService userService;

    @Inject AuthService authService;

    @Inject LoginAttemptRepository loginAttemptRepository;

    /**
     * @return whether the given user has at least one registered passkey
     */
    public boolean hasPasskey(User user) {
        return webAuthnCredentialRepository.countByUser(user) > 0;
    }

    /**
     * @return the number of passkeys registered for the given user
     */
    public long countCredentials(User user) {
        return webAuthnCredentialRepository.countByUser(user);
    }

    /**
     * @return the authentication methods available to the user
     */
    public WebAuthnStatusDTO getStatus(User user) {
        return new WebAuthnStatusDTO(hasPasskey(user), user.getPasswordHash() != null);
    }

    /**
     * @return the user's registered passkeys as management DTOs
     */
    public List<WebAuthnCredentialDTO> listCredentials(User user) {
        return webAuthnCredentialRepository.findAllByUser(user).stream()
                .map(WebAuthnCredentialDTO::new)
                .toList();
    }

    /**
     * Deletes one of the user's passkeys.
     *
     * @param user the owner
     * @param credentialId the entity id of the passkey to delete
     * @return true if a passkey was deleted, false if none matched
     * @throws LastCredentialException if this would remove the last login method of a passkey-only
     *     account
     */
    @Transactional
    public boolean deleteCredential(User user, UUID credentialId) {
        if (!webAuthnCredentialRepository.existsByIdAndUser(credentialId, user)) {
            return false;
        }
        boolean hasPassword = user.getPasswordHash() != null;
        if (!hasPassword && webAuthnCredentialRepository.countByUser(user) <= 1) {
            throw new LastCredentialException(
                    "Cannot delete the last passkey of an account that has no password. Set a"
                            + " password first.");
        }
        boolean deleted = webAuthnCredentialRepository.deleteWithIdAndUser(credentialId, user);
        if (deleted) {
            LOG.infof("Deleted passkey %s for user ID: %s", credentialId, user.id);
        }
        return deleted;
    }

    /**
     * Persists a freshly registered passkey for an existing user.
     *
     * @param user the owner
     * @param record the verified credential produced by the WebAuthn ceremony
     * @param label an optional user-facing name for the passkey (a sensible default)
     */
    @Transactional
    public void addCredentialToUser(User user, WebAuthnCredentialRecord record, String label) {
        persistCredential(user, record, label);
        LOG.infof("Registered new passkey for user ID: %s", user.id);
    }

    /**
     * Renames one of the user's passkeys.
     *
     * @param user the owner
     * @param credentialId the entity id of the passkey to rename
     * @param label the new label
     * @return true if a passkey was renamed, false if none matched the user
     */
    @Transactional
    public boolean renameCredential(User user, UUID credentialId, String label) {
        WebAuthnCredential credential = webAuthnCredentialRepository.findById(credentialId);
        if (credential == null || !credential.getUser().id.equals(user.id)) {
            return false;
        }
        credential.setLabel(label);
        LOG.infof("Renamed passkey %s for user ID: %s", credentialId, user.id);
        return true;
    }

    /**
     * Creates a brand-new account from a passkey registration and persists its first credential,
     * all in a single transaction. The passkey is the account's only credential; no password is
     * set.
     *
     * @param registration the account details (username, name and email required)
     * @param record the verified credential produced by the WebAuthn ceremony
     * @param label an optional user-facing name for the passkey (a sensible default)
     * @return the newly created user
     */
    @Transactional
    public User createUserWithCredential(
            WebAuthnRegistrationStartDTO registration,
            WebAuthnCredentialRecord record,
            String label)
            throws DuplicateUserException, InvalidUserException, RegistrationDisabledException {
        if (!authService.isRegistrationEnabled()) {
            throw new RegistrationDisabledException("User registration is currently disabled");
        }

        UserCreationDTO userCreationDTO =
                new UserCreationDTO(
                        registration.getUsername(),
                        registration.getEmail(),
                        null,
                        registration.getFirstname(),
                        registration.getLastname(),
                        Set.of());

        userService.createUser(userCreationDTO, Set.of(Roles.USER), true, true);

        User user = userRepository.findByUsername(registration.getUsername());
        if (user == null) {
            throw new InvalidUserException(
                    "User not found after creation: " + registration.getUsername());
        }

        persistCredential(user, record, label);
        LOG.infof("Created passkey account for user ID: %s", user.id);
        return user;
    }

    /** Records a successful passkey login for audit parity with password logins. */
    @Transactional
    public void recordSuccessfulLogin(User user) {
        loginAttemptRepository.recordAttempt(user, true);
    }

    private void persistCredential(User user, WebAuthnCredentialRecord record, String label) {
        RequiredPersistedData data = record.getRequiredPersistedData();
        String trimmedLabel = label != null && !label.isBlank() ? label.trim() : null;
        WebAuthnCredential credential =
                new WebAuthnCredential(
                        data.credentialId(),
                        user,
                        data.publicKey(),
                        data.publicKeyAlgorithm(),
                        data.counter(),
                        data.aaguid(),
                        trimmedLabel,
                        Instant.now());
        webAuthnCredentialRepository.persist(credential);
    }
}
