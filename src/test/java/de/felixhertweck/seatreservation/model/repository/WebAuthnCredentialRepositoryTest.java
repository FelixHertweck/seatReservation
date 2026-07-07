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

import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.WebAuthnCredential;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebAuthnCredentialRepositoryTest {

    @Inject WebAuthnCredentialRepository repository;

    @Inject UserRepository userRepository;

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
                        java.util.Set.of("USER"),
                        java.util.Set.of());
        userRepository.persist(user);
        return user;
    }

    private WebAuthnCredential persistCredential(User user, String credentialId) {
        WebAuthnCredential credential =
                new WebAuthnCredential(
                        credentialId,
                        user,
                        new byte[] {1, 2, 3},
                        -7L,
                        0L,
                        UUID.randomUUID(),
                        null,
                        Instant.now());
        repository.persist(credential);
        return credential;
    }

    @Test
    @TestTransaction
    void findByCredentialId_returnsMatchingCredential() {
        User user = persistUser("repo_findcred");
        persistCredential(user, "cred-abc");

        WebAuthnCredential found = repository.findByCredentialId("cred-abc");
        assertNotNull(found);
        assertEquals(user.id, found.getUser().id);
        assertNull(repository.findByCredentialId("does-not-exist"));
    }

    @Test
    @TestTransaction
    void findAllByUser_andCount_returnOnlyOwnedCredentials() {
        User user = persistUser("repo_owner");
        User other = persistUser("repo_other");
        persistCredential(user, "cred-1");
        persistCredential(user, "cred-2");
        persistCredential(other, "cred-3");

        assertEquals(2, repository.findAllByUser(user).size());
        assertEquals(2, repository.countByUser(user));
        assertEquals(1, repository.countByUser(other));
    }

    @Test
    @TestTransaction
    void deleteWithIdAndUser_onlyDeletesForOwner() {
        User user = persistUser("repo_del_owner");
        User other = persistUser("repo_del_other");
        WebAuthnCredential credential = persistCredential(user, "cred-del");

        // Wrong owner does not delete.
        assertFalse(repository.deleteWithIdAndUser(credential.id, other));
        assertEquals(1, repository.countByUser(user));

        // Correct owner deletes.
        assertTrue(repository.deleteWithIdAndUser(credential.id, user));
        assertEquals(0, repository.countByUser(user));
    }
}
