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
package de.felixhertweck.seatreservation.utils;

import java.security.Principal;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class UserSecurityContext {

    @Inject SecurityIdentity securityIdentity;

    @Inject JsonWebToken jsonWebToken;

    @Inject UserRepository userRepository;

    /**
     * Retrieves the current authenticated user based on the security context.
     *
     * @return The current User entity.
     * @throws UserNotFoundException If the current user cannot be found in the database.
     */
    public User getCurrentUser() throws UserNotFoundException {
        Principal principal = securityIdentity.getPrincipal();
        User currentUser = userRepository.findByUsername(principal.getName());
        if (currentUser == null) {
            throw new UserNotFoundException("Current user not found.");
        }
        return currentUser;
    }

    /**
     * Builds the caller's identity and roles directly from the validated JWT claims (the {@code
     * uid} claim and the {@code groups}/roles already resolved onto {@link SecurityIdentity}) — no
     * database access. Use this wherever only the ID and/or roles are needed instead of {@link
     * #getCurrentUser()}.
     *
     * @return the authenticated user's ID and roles
     */
    public AuthenticatedUser getAuthenticatedUser() {
        UUID id = UUID.fromString(jsonWebToken.getClaim("uid").toString());
        return new AuthenticatedUser(id, securityIdentity.getRoles());
    }

    /**
     * Returns a lazy, uninitialized reference to the current user, without hitting the database.
     * Safe to use for ID comparisons and as a foreign-key parameter in queries/relations; accessing
     * any other field triggers a full load.
     *
     * @return an uninitialized proxy for the current user
     */
    public User getCurrentUserReference() {
        return userRepository.getReference(getAuthenticatedUser().id());
    }
}
