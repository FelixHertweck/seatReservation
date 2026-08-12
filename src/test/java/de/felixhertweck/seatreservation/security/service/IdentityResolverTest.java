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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import org.junit.jupiter.api.Test;

public class IdentityResolverTest {

    @Test
    void testLocalCredentialIdentityResolver_ByEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        User expectedUser = new User();
        expectedUser.setEmail("local@example.com");

        when(userRepository.findByEmail("local@example.com")).thenReturn(expectedUser);

        LocalCredentialIdentityResolver resolver = new LocalCredentialIdentityResolver();
        resolver.userRepository = userRepository;

        assertEquals("local", resolver.providerId());

        ExternalIdentity identity =
                new ExternalIdentity(
                        "local", "subject1", "local@example.com", true, "Name", "John", "Doe");
        User resolved = resolver.resolveOrProvision(identity);

        assertNotNull(resolved);
        assertEquals("local@example.com", resolved.getEmail());
    }

    @Test
    void testLocalCredentialIdentityResolver_ByUsername() {
        UserRepository userRepository = mock(UserRepository.class);
        User expectedUser = new User();
        expectedUser.setUsername("user123");

        when(userRepository.findByUsername("user123")).thenReturn(expectedUser);

        LocalCredentialIdentityResolver resolver = new LocalCredentialIdentityResolver();
        resolver.userRepository = userRepository;

        ExternalIdentity identity =
                new ExternalIdentity("local", "user123", null, false, "Name", null, null);
        User resolved = resolver.resolveOrProvision(identity);

        assertNotNull(resolved);
        assertEquals("user123", resolved.getUsername());
    }

    @Test
    void testOidcIdentityResolver_ResolveExisting() {
        UserRepository userRepository = mock(UserRepository.class);
        User existingUser = new User();
        existingUser.setEmail("oidc@example.com");

        when(userRepository.findByEmail("oidc@example.com")).thenReturn(existingUser);

        OidcIdentityResolver resolver = new OidcIdentityResolver();
        resolver.userRepository = userRepository;

        assertEquals("oidc", resolver.providerId());

        ExternalIdentity identity =
                new ExternalIdentity(
                        "oidc", "sub-123", "oidc@example.com", true, "Jane Doe", "Jane", "Doe");
        User resolved = resolver.resolveOrProvision(identity);

        assertNotNull(resolved);
        assertEquals("oidc@example.com", resolved.getEmail());
    }

    @Test
    void testOidcIdentityResolver_RefusesLinkingUnverifiedEmailToExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        User existingUser = new User();
        existingUser.setEmail("victim@example.com");

        when(userRepository.findByEmail("victim@example.com")).thenReturn(existingUser);

        OidcIdentityResolver resolver = new OidcIdentityResolver();
        resolver.userRepository = userRepository;

        ExternalIdentity identity =
                new ExternalIdentity(
                        "oidc",
                        "sub-attacker",
                        "victim@example.com",
                        false,
                        "Attacker",
                        "Attacker",
                        null);
        User resolved = resolver.resolveOrProvision(identity);

        assertNull(resolved);
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void testOidcIdentityResolver_ProvisionNewUser() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);

        OidcIdentityResolver resolver = new OidcIdentityResolver();
        resolver.userRepository = userRepository;

        ExternalIdentity identity =
                new ExternalIdentity(
                        "oidc",
                        "sub-999",
                        "new@example.com",
                        true,
                        "Alice Smith",
                        "Alice",
                        "Smith");
        User provisioned = resolver.resolveOrProvision(identity);

        assertNotNull(provisioned);
        assertEquals("sub-999", provisioned.getUsername());
        assertEquals("new@example.com", provisioned.getEmail());
        assertEquals("Alice", provisioned.getFirstname());
        assertEquals("Smith", provisioned.getLastname());
        assertTrue(provisioned.isEmailVerified());
        verify(userRepository).persist(any(User.class));
    }

    @Test
    void testOidcIdentityResolver_ProvisionsNewUserAsUnverified_WhenProviderDidNotAssertEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail("unverified@example.com")).thenReturn(null);

        OidcIdentityResolver resolver = new OidcIdentityResolver();
        resolver.userRepository = userRepository;

        ExternalIdentity identity =
                new ExternalIdentity(
                        "oidc", "sub-1000", "unverified@example.com", false, "Bob", "Bob", null);
        User provisioned = resolver.resolveOrProvision(identity);

        assertNotNull(provisioned);
        assertFalse(provisioned.isEmailVerified());
        verify(userRepository).persist(any(User.class));
    }
}
