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
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject UserRepository userRepository;

    @Inject TokenService tokenService;

    @Inject UserService userService;

    /**
     * Authenticates a user with the given identifier and password.
     *
     * @param identifier the identifier of the user
     * @param password the password of the user
     * @return a JWT token if authentication is successful
     * @throws AuthenticationFailedException if authentication fails
     */
    public String authenticate(String identifier, String password)
            throws AuthenticationFailedException {
        LOG.debugf("Attempting to authenticate user with identifier: %s", identifier);
        User user;
        if (isIdentifierEmail(identifier)) {
            user = userRepository.findByEmail(identifier);
        } else {
            user = userRepository.findByUsername(identifier);
        }

        if (user == null) {
            LOG.warnf("Authentication failed for identifier %s: User not found.", identifier);
            throw new AuthenticationFailedException("Failed to authenticate user: " + identifier);
        }

        // Combine the provided password with the stored salt before hashing for comparison
        String saltedPassword = password + user.getPasswordSalt();
        if (!BcryptUtil.matches(saltedPassword, user.getPasswordHash())) {
            LOG.warnf(
                    "Authentication failed for user %s: Invalid credentials.", user.getUsername());
            throw new AuthenticationFailedException(
                    "Failed to authenticate user: " + user.getUsername());
        }
        LOG.infof("User %s authenticated successfully.", user.getUsername());

        return tokenService.generateToken(user);
    }

    /**
     * Registers a new user with the given registration details.
     *
     * @param registerRequest the registration details
     * @return a JWT token if registration is successful
     * @throws DuplicateUserException if the user already exists
     * @throws InvalidUserException if the user details are invalid
     */
    public String register(RegisterRequestDTO registerRequest)
            throws DuplicateUserException, InvalidUserException {
        LOG.debugf("Attempting to register new user: %s", registerRequest.getUsername());

        UserCreationDTO userCreationDTO = new UserCreationDTO(registerRequest);

        userService.createUser(userCreationDTO, Set.of(Roles.USER), true);

        User user = userRepository.findByUsername(registerRequest.getUsername());

        if (user == null) {
            LOG.warnf("User %s not found after registration.", registerRequest.getUsername());
            throw new InvalidUserException("User not found: " + registerRequest.getUsername());
        }

        LOG.infof("User %s registered successfully", registerRequest.getUsername());

        return tokenService.generateToken(user);
    }

    private boolean isIdentifierEmail(String identifier) {
        return identifier.contains("@");
    }
}
