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

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OidcIdentityResolver implements IdentityResolver {

    private static final Logger LOG = Logger.getLogger(OidcIdentityResolver.class);

    @Inject UserRepository userRepository;

    @Override
    public String providerId() {
        return "oidc";
    }

    @Override
    @Transactional
    public User resolveOrProvision(ExternalIdentity identity) {
        if (identity == null || identity.email() == null || identity.email().isBlank()) {
            LOG.warn("Cannot resolve or provision identity without email.");
            return null;
        }

        User user = userRepository.findByEmail(identity.email());
        if (user != null) {
            if (!identity.emailVerified()) {
                LOG.warnf(
                        "Refusing to link OIDC subject %s to existing user %s: provider did not"
                                + " assert email_verified for %s",
                        identity.subject(), user.getUsername(), identity.email());
                return null;
            }
            LOG.infof(
                    "Resolved existing user %s for OIDC subject %s",
                    user.getUsername(), identity.subject());
            return user;
        }

        // Just-in-time provision new user for OIDC
        User newUser = new User();
        String baseUsername = identity.subject() != null ? identity.subject() : identity.email();
        newUser.setUsername(baseUsername);
        newUser.setEmail(identity.email());
        newUser.setFirstname(identity.givenName() != null ? identity.givenName() : identity.name());
        newUser.setLastname(identity.familyName());
        newUser.setEmailVerified(identity.emailVerified());
        newUser.setRoles(Set.of(Roles.USER));

        userRepository.persist(newUser);
        LOG.infof("Provisioned new OIDC user %s (%s)", newUser.getUsername(), newUser.getEmail());
        return newUser;
    }
}
