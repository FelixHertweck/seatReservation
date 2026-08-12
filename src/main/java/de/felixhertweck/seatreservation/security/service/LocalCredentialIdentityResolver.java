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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;

@ApplicationScoped
public class LocalCredentialIdentityResolver implements IdentityResolver {

    @Inject UserRepository userRepository;

    @Override
    public String providerId() {
        return "local";
    }

    @Override
    @Transactional
    public User resolveOrProvision(ExternalIdentity identity) {
        if (identity == null) {
            return null;
        }
        User user = null;
        if (identity.email() != null && !identity.email().isBlank()) {
            user = userRepository.findByEmail(identity.email());
        }
        if (user == null && identity.subject() != null && !identity.subject().isBlank()) {
            user = userRepository.findByUsername(identity.subject());
        }
        return user;
    }
}
