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

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject UserRepository userRepository;

    @Inject UserService userService;

    @ConfigProperty(name = "registration.enabled", defaultValue = "true")
    boolean registrationEnabled;

    /**
     * Checks if user registration is enabled.
     *
     * @return true if registration is enabled, false otherwise
     */
    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    /**
     * Authenticates a user with the given username and password.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @return the authenticated User if authentication is successful
     * @throws AuthenticationFailedException if authentication fails
     */
    public User authenticate(String username, String password)
            throws AuthenticationFailedException {
        LOG.debugf("Attempting to authenticate user with username: %s", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            LOG.warnf("Authentication failed for username %s: User not found.", username);
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }
        if (passwordMatches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            LOG.infof("User %s authenticated successfully.", user.getUsername());
            return user;
        }

        LOG.warnf("Authentication failed for username %s: Password mismatch.", username);
        throw new AuthenticationFailedException("Failed to authenticate user: " + username);
    }

    public boolean passwordMatches(String password, String passwordSalt, String storedHash) {
        // Combine the provided password with the stored salt before hashing for comparison
        return BcryptUtil.matches(password + passwordSalt, storedHash);
    }

    /**
     * Registers a new user with the given registration details.
     *
     * @param registerRequest the registration details
     * @return the registered User
     * @throws DuplicateUserException if the user already exists
     * @throws InvalidUserException if the user details are invalid
     * @throws RegistrationDisabledException if registration is disabled
     */
    public User register(RegisterRequestDTO registerRequest)
            throws DuplicateUserException, InvalidUserException, RegistrationDisabledException {
        LOG.debugf("Attempting to register new user: %s", registerRequest.getUsername());

        if (!registrationEnabled) {
            LOG.warnf(
                    "Registration attempt made for user %s when registration is disabled.",
                    registerRequest.getUsername());
            throw new RegistrationDisabledException("User registration is currently disabled");
        }

        UserCreationDTO userCreationDTO = new UserCreationDTO(registerRequest);

        userService.createUser(userCreationDTO, Set.of(Roles.USER), true);

        User user = userRepository.findByUsername(registerRequest.getUsername());

        if (user == null) {
            LOG.warnf("User %s not found after registration.", registerRequest.getUsername());
            throw new InvalidUserException("User not found: " + registerRequest.getUsername());
        }

        LOG.infof("User %s registered successfully", registerRequest.getUsername());

        return user;
    }
}
