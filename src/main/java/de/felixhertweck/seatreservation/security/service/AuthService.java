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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.AuthenticationFailedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject UserRepository userRepository;

    @Inject TokenService tokenService;

    public String authenticate(String username, String password)
            throws AuthenticationFailedException {
        LOG.infof("Attempting to authenticate user: %s", username);
        User user = userRepository.findByUsername(username);

        if (user == null) {
            LOG.warnf("Authentication failed for user %s: User not found.", username);
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }
        LOG.debugf("User %s found. Verifying password.", username);

        if (!BcryptUtil.matches(password, user.getPasswordHash())) {
            LOG.warnf("Authentication failed for user %s: Invalid credentials.", username);
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }
        LOG.infof("User %s authenticated successfully. Generating token.", username);

        return tokenService.generateToken(user);
    }
}
