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
package de.felixhertweck.seatreservation.userManagment.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.InvalidUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.TokenExpiredException;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    @Inject UserRepository userRepository;

    @Inject EmailService emailService;

    @Inject EmailVerificationRepository emailVerificationRepository;

    /**
     * Creates a new user with the provided dto.
     *
     * @param userCreationDTO The DTO containing user creation dto.
     * @return The created UserDTO.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws RuntimeException If an error occurs while sending email confirmation.
     */
    @Transactional
    public UserDTO createUser(UserCreationDTO userCreationDTO)
            throws InvalidUserException, DuplicateUserException {
        if (userCreationDTO == null) {
            LOG.warn("UserCreationDTO is null during user creation.");
            throw new InvalidUserException("User creation data cannot be null.");
        }

        LOG.infof("Attempting to create new user with username: %s", userCreationDTO.getUsername());
        LOG.debugf("UserCreationDTO: %s", userCreationDTO.toString());

        if (userCreationDTO.getUsername() == null
                || userCreationDTO.getUsername().trim().isEmpty()) {
            LOG.warn("Username is empty or null during user creation.");
            throw new InvalidUserException("Username cannot be empty.");
        }
        if (userCreationDTO.getPassword() == null
                || userCreationDTO.getPassword().trim().isEmpty()) {
            LOG.warn("Password is empty or null during user creation.");
            throw new InvalidUserException("Password cannot be empty.");
        }

        if (userRepository.findByUsernameOptional(userCreationDTO.getUsername()).isPresent()) {
            LOG.warnf(
                    "Duplicate user creation attempt for username: %s",
                    userCreationDTO.getUsername());
            throw new DuplicateUserException(
                    "User with username " + userCreationDTO.getUsername() + " already exists.");
        }
        User user = new User();
        user.setUsername(userCreationDTO.getUsername());
        if (userCreationDTO.getEmail() != null && !userCreationDTO.getEmail().trim().isEmpty()) {
            user.setEmail(userCreationDTO.getEmail());
            LOG.debugf("User email set to: %s", user.getEmail());
        }
        user.setPasswordHash(
                BcryptUtil.bcryptHash(userCreationDTO.getPassword())); // Hash the password
        user.setFirstname(userCreationDTO.getFirstname());
        user.setLastname(userCreationDTO.getLastname());
        user.setRoles(new HashSet<>(List.of(Roles.USER))); // Default role for new users
        LOG.debugf(
                "User object prepared: username=%s, firstname=%s, lastname=%s, roles=%s",
                user.getUsername(), user.getFirstname(), user.getLastname(), user.getRoles());

        userRepository.persist(user);
        LOG.infof("User %s persisted successfully with ID: %d", user.getUsername(), user.id);

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            try {
                LOG.debugf("Attempting to send email confirmation to %s", user.getEmail());
                emailService.sendEmailConfirmation(user);
                LOG.infof(
                        "Email confirmation sent to %s for user ID: %d", user.getEmail(), user.id);
            } catch (IOException e) {
                LOG.errorf(
                        e,
                        "Failed to send email confirmation to %s for user ID %d: %s",
                        user.getEmail(),
                        user.id,
                        e.getMessage());
                throw new RuntimeException("Failed to send email confirmation: " + e.getMessage());
            }
        }

        return new UserDTO(user);
    }

    private void updateUserCore(
            User existingUser, String email, String firstname, String lastname, String password) {
        LOG.debugf("Entering updateUserCore for user ID: %d", existingUser.id);

        // Update email if provided and different
        if (email != null && !email.trim().isEmpty() && !email.equals(existingUser.getEmail())) {
            LOG.debugf(
                    "Updating email for user ID %d from %s to %s",
                    existingUser.id, existingUser.getEmail(), email);
            existingUser.setEmail(email);
            // Reset email verification status and send confirmation email
            existingUser.setEmailVerified(false);
            try {
                LOG.debugf(
                        "Sending email confirmation to %s for user ID %d due to email change.",
                        existingUser.getEmail(), existingUser.id);
                emailService.sendEmailConfirmation(existingUser);
                LOG.infof(
                        "Email confirmation sent to %s for user ID: %d",
                        existingUser.getEmail(), existingUser.id);
            } catch (IOException e) {
                LOG.errorf(
                        e,
                        "Failed to send email confirmation to %s for user ID %d: %s",
                        existingUser.getEmail(),
                        existingUser.id,
                        e.getMessage());
                throw new RuntimeException("Failed to send email confirmation: " + e.getMessage());
            }
        }

        // Update firstname if provided
        if (firstname != null && !firstname.trim().isEmpty()) {
            if (!firstname.equals(existingUser.getFirstname())) {
                LOG.debugf(
                        "Updating firstname for user ID %d from %s to %s",
                        existingUser.id, existingUser.getFirstname(), firstname);
                existingUser.setFirstname(firstname);
            }
        }

        // Update lastname if provided
        if (lastname != null && !lastname.trim().isEmpty()) {
            if (!lastname.equals(existingUser.getLastname())) {
                LOG.debugf(
                        "Updating lastname for user ID %d from %s to %s",
                        existingUser.id, existingUser.getLastname(), lastname);
                existingUser.setLastname(lastname);
            }
        }

        // Update password if provided
        if (password != null && !password.trim().isEmpty()) {
            LOG.debugf("Updating password for user ID %d.", existingUser.id);
            existingUser.setPasswordHash(BcryptUtil.bcryptHash(password)); // Hash the password
            // Send password changed notification email
            try {
                LOG.debugf(
                        "Sending password changed notification to %s for user ID %d.",
                        existingUser.getEmail(), existingUser.id);
                emailService.sendPasswordChangedNotification(existingUser);
                LOG.infof(
                        "Password changed notification sent to %s for user ID: %d",
                        existingUser.getEmail(), existingUser.id);
            } catch (IOException e) {
                LOG.errorf(
                        e,
                        "Failed to send password changed notification email to %s for user ID %d:"
                                + " %s",
                        existingUser.getEmail(),
                        existingUser.id,
                        e.getMessage());
                throw new RuntimeException(
                        "Failed to send password changed notification email: " + e.getMessage());
            }
        }
        LOG.debugf("Exiting updateUserCore for user ID: %d", existingUser.id);
    }

    /**
     * Updates an existing user with the provided dto.
     *
     * @param id The ID of the user to update.
     * @param user The DTO containing user profile update data.
     * @return The updated UserDTO.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws RuntimeException If an error occurs while sending email confirmation.
     */
    @Transactional
    public UserDTO updateUser(Long id, AdminUserUpdateDTO user) throws UserNotFoundException {
        if (user == null) {
            LOG.warnf("AdminUserUpdateDTO is null for user ID: %d.", id);
            throw new InvalidUserException("User update data cannot be null.");
        }

        LOG.infof("Attempting to update user with ID: %d by admin.", id);
        LOG.debugf("AdminUserUpdateDTO for ID %d: %s", id, user.toString());

        User existingUser =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf("User with ID %d not found for update.", id);
                                    return new UserNotFoundException(
                                            "User with id " + id + " not found.");
                                });

        updateUserCore(
                existingUser,
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getPassword());

        if (user.getRoles() != null) {
            LOG.debugf(
                    "Updating roles for user ID %d from %s to %s",
                    existingUser.id, existingUser.getRoles(), user.getRoles());
            existingUser.setRoles(user.getRoles());
        }
        userRepository.persist(existingUser);
        LOG.infof("User with ID %d updated successfully by admin.", existingUser.id);
        return new UserDTO(existingUser);
    }

    /**
     * Deletes a user by ID.
     *
     * @param id The ID of the user to delete.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     */
    @Transactional
    public void deleteUser(Long id) throws UserNotFoundException {
        LOG.infof("Attempting to delete user with ID: %d.", id);
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            LOG.warnf("User with ID %d not found for deletion.", id);
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        LOG.infof("User with ID %d deleted successfully.", id);
    }

    public UserDTO getUserById(Long id) {
        LOG.infof("Attempting to retrieve user with ID: %d.", id);
        User user =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf("User with ID %d not found.", id);
                                    return new UserNotFoundException(
                                            "User with id " + id + " not found.");
                                });
        LOG.infof("User with ID %d retrieved successfully.", id);
        return new UserDTO(user);
    }

    public List<LimitedUserInfoDTO> getAllUsers() {
        LOG.infof("Retrieving all users (limited info).");
        List<LimitedUserInfoDTO> users =
                userRepository.listAll().stream().map(LimitedUserInfoDTO::new).toList();
        LOG.debugf("Returning %d limited user info DTOs.", users.size());
        return users;
    }

    public List<UserDTO> getUsersAsAdmin() {
        LOG.infof("Retrieving all users (admin view).");
        List<UserDTO> users = userRepository.listAll().stream().map(UserDTO::new).toList();
        LOG.debugf("Returning %d user DTOs for admin view.", users.size());
        return users;
    }

    public List<String> getAvailableRoles() {
        LOG.infof("Retrieving available roles.");
        List<String> roles = Arrays.asList(Roles.ALL_ROLES);
        LOG.debugf("Returning %d available roles.", roles.size());
        return roles;
    }

    @Transactional
    public UserDTO updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO)
            throws UserNotFoundException {
        if (userProfileUpdateDTO == null) {
            LOG.warnf("UserProfileUpdateDTO is null for username: %s.", username);
            throw new InvalidUserException("User profile update data cannot be null.");
        }

        LOG.infof("Attempting to update user profile for username: %s.", username);
        LOG.debugf("UserProfileUpdateDTO for %s: %s", username, userProfileUpdateDTO.toString());

        User existingUser =
                userRepository
                        .findByUsernameOptional(username)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "User with username %s not found for profile update.",
                                            username);
                                    return new UserNotFoundException(
                                            "User with username " + username + " not found.");
                                });

        updateUserCore(
                existingUser,
                userProfileUpdateDTO.getEmail(),
                userProfileUpdateDTO.getFirstname(),
                userProfileUpdateDTO.getLastname(),
                userProfileUpdateDTO.getPassword());

        userRepository.persist(existingUser);
        LOG.infof("User profile for username %s updated successfully.", username);
        return new UserDTO(existingUser);
    }

    /**
     * Verifies the email address of a user using the provided token.
     *
     * @param id The ID of the email verification record.
     * @param token The token to verify.
     * @return The email address of the user if verification is successful.
     * @throws IllegalArgumentException If the ID or token is invalid.
     * @throws TokenExpiredException If the token has expired.
     */
    @Transactional
    public String verifyEmail(Long id, String token) throws TokenExpiredException {
        LOG.infof("Attempting to verify email with ID: %d and token.", id);
        LOG.debugf("Verification ID: %d, Token: %s", id, token);

        // Validate id and token
        if (id == null || token == null || token.trim().isEmpty()) {
            LOG.warn("Invalid ID or token provided for email verification.");
            throw new IllegalArgumentException("Invalid id or token");
        }

        // Get the email verification record
        EmailVerification emailVerification =
                emailVerificationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf("Email verification token not found for ID: %d", id);
                                    return new IllegalArgumentException("Token not found");
                                });
        LOG.debugf("Email verification record found for ID: %d", id);

        // Check if the token is correct
        if (!emailVerification.getToken().equals(token)) {
            LOG.warnf(
                    "Invalid token provided for email verification ID %d. Expected: %s, Received:"
                            + " %s",
                    id, emailVerification.getToken(), token);
            throw new IllegalArgumentException("Invalid token");
        }
        LOG.debugf("Token for ID %d is correct.", id);

        // Check if the token has expired
        if (emailVerification.getExpirationTime().isBefore(LocalDateTime.now())) {
            LOG.warnf(
                    "Email verification token for ID %d has expired. Expiration time: %s, Current"
                            + " time: %s",
                    id, emailVerification.getExpirationTime(), LocalDateTime.now());
            throw new TokenExpiredException("Token expired");
        }
        LOG.debugf("Token for ID %d is not expired.", id);

        // Delete the email verification record
        emailVerificationRepository.deleteById(id);
        LOG.infof("Email verification record for ID %d deleted.", id);

        // Mark the email as verified
        User user = emailVerification.getUser();
        user.setEmailVerified(true);
        userRepository.persist(user);
        LOG.infof("Email for user ID %d (%s) marked as verified.", user.id, user.getEmail());

        return user.getEmail();
    }
}
