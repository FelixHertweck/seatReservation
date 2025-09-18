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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.userManagment.exceptions.TokenExpiredException;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject UserRepository userRepository;

    @Inject EmailService emailService;

    @Inject EmailVerificationRepository emailVerificationRepository;

    /**
     * Creates a new user in the system. No DTOs are handled in this service layer.
     *
     * @param username the username
     * @param email the email (optional)
     * @param password the raw password (will be salted and hashed)
     * @param firstname the first name
     * @param lastname the last name
     * @param roles roles to assign
     * @param tags tags to assign (optional)
     * @return the persisted User entity
     * @throws InvalidUserException if input is invalid
     * @throws DuplicateUserException if username already exists
     */
    @Transactional
    public User createUser(
            String username,
            String email,
            String password,
            String firstname,
            String lastname,
            Set<String> roles,
            Set<String> tags)
            throws InvalidUserException, DuplicateUserException {
        if (username == null || username.trim().isEmpty()) {
            LOG.warn("Username is empty or null during user creation.");
            throw new InvalidUserException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            LOG.warn("Password is empty or null during user creation.");
            throw new InvalidUserException("Password cannot be empty.");
        }

        if (userRepository.findByUsernameOptional(username).isPresent()) {
            LOG.warnf("Duplicate user creation attempt for username: %s", username);
            throw new DuplicateUserException("User with username " + username + " already exists.");
        }

        String effectiveEmail = null;
        if (email != null && !email.trim().isEmpty()) {
            effectiveEmail = email;
            LOG.debugf("User email set to: %s", effectiveEmail);
        }

        String salt = generateSalt();
        String passwordHash = BcryptUtil.bcryptHash(password + salt);

        User user =
                new User(
                        username,
                        effectiveEmail,
                        false,
                        passwordHash,
                        salt,
                        firstname,
                        lastname,
                        roles,
                        tags);

        LOG.debugf(
                "User object prepared: username=%s, firstname=%s, lastname=%s, roles=%s, tags=%s",
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getRoles(),
                user.getTags());

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            try {
                LOG.debugf("Attempting to send email confirmation to %s", user.getEmail());

                emailVerificationRepository.deleteByUserId(user.id);

                EmailVerification emailVerification = emailService.createEmailVerification(user);
                emailService.sendEmailConfirmation(user, emailVerification);
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
        userRepository.persist(user);
        LOG.infof("User %s persisted successfully with ID: %d", user.getUsername(), user.id);

        return user;
    }

    private void updateUserCore(
            User existingUser,
            String email,
            String firstname,
            String lastname,
            String password,
            Set<String> tags) {
        LOG.debugf("Entering updateUserCore for user ID: %d", existingUser.id);

        // Update email if provided and different
        if (email != null && !email.trim().isEmpty() && !email.equals(existingUser.getEmail())) {
            LOG.debugf(
                    "Updating email for user ID %d from %s to %s",
                    existingUser.id, existingUser.getEmail(), email);
            existingUser.setEmail(email);
            // Reset email verification status
            existingUser.setEmailVerified(false);

            // Delete existing email verification entry in a new transaction to avoid constraint
            // violation
            emailVerificationRepository.deleteByUserId(existingUser.id);

            // Send new email confirmation
            try {
                LOG.debugf(
                        "Sending email confirmation to %s for user ID %d due to email change.",
                        existingUser.getEmail(), existingUser.id);
                EmailVerification emailVerification =
                        emailService.createEmailVerification(existingUser);
                emailService.sendEmailConfirmation(existingUser, emailVerification);
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
            String newSalt = generateSalt();
            existingUser.setPasswordSalt(newSalt);
            existingUser.setPasswordHash(
                    BcryptUtil.bcryptHash(
                            password + newSalt)); // Hash the new password with new salt
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

        if (tags != null) {
            if (!tags.equals(existingUser.getTags())) {
                LOG.debugf(
                        "Updating tags for user ID %d from %s to %s",
                        existingUser.id, existingUser.getTags(), tags);
                existingUser.setTags(tags);
            }
        }

        LOG.debugf("Exiting updateUserCore for user ID: %d", existingUser.id);
    }

    /**
     * Updates an existing user with the provided dto.
     *
     * @param id The ID of the user to update.
     * @param firstname The new first name.
     * @param lastname The new last name.
     * @param password The new raw password.
     * @param email The new email address.
     * @param roles The new set of roles.
     * @param tags The new set of tags.
     * @return The updated UserDTO.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws RuntimeException If an error occurs while sending email confirmation.
     */
    @Transactional
    public User updateUser(
            Long id,
            String firstname,
            String lastname,
            String password,
            String email,
            Set<String> roles,
            Set<String> tags)
            throws UserNotFoundException {
        LOG.infof("Attempting to update user with ID: %d by admin.", id);

        User existingUser =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf("User with ID %d not found for update.", id);
                                    return new UserNotFoundException(
                                            "User with id " + id + " not found.");
                                });

        updateUserCore(existingUser, email, firstname, lastname, password, tags);

        if (roles != null) {
            LOG.debugf(
                    "Updating roles for user ID %d from %s to %s",
                    existingUser.id, existingUser.getRoles(), roles);
            existingUser.setRoles(roles);
        }
        userRepository.persist(existingUser);
        LOG.infof("User with ID %d updated successfully by admin.", existingUser.id);
        return existingUser;
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

    public User getUserById(Long id) {
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
        return user;
    }

    public List<User> getAllUsers() {
        LOG.infof("Retrieving all users.");
        List<User> users = userRepository.listAll();
        LOG.debugf("Returning %d users.", users.size());
        return users;
    }

    public List<User> getUsersAsAdmin() {
        LOG.infof("Retrieving all users (admin view).");
        List<User> users = userRepository.listAll();
        LOG.debugf("Returning %d users for admin view.", users.size());
        return users;
    }

    public List<String> getAvailableRoles() {
        LOG.infof("Retrieving available roles.");
        List<String> roles = Arrays.asList(Roles.ALL_ROLES);
        LOG.debugf("Returning %d available roles.", roles.size());
        return roles;
    }

    @Transactional
    public User updateUserProfile(
            String username,
            String firstname,
            String lastname,
            String password,
            String email,
            Set<String> tags)
            throws UserNotFoundException {
        LOG.infof("Attempting to update user profile for username: %s.", username);

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

        updateUserCore(existingUser, email, firstname, lastname, password, tags);

        userRepository.persist(existingUser);
        LOG.infof("User profile for username %s updated successfully.", username);
        return existingUser;
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

    /**
     * Resends the email confirmation for a given username and extends the token's lifetime.
     *
     * @param username The username of the user for whom to resend the email.
     * @throws UserNotFoundException If the user with the given username does not exist.
     * @throws IOException If an error occurs while sending the email.
     */
    @Transactional
    public void resendEmailConfirmation(String username) throws UserNotFoundException, IOException {
        LOG.infof("Attempting to resend email confirmation for username: %s", username);

        User user =
                userRepository
                        .findByUsernameOptional(username)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "User with username %s not found for resending email"
                                                    + " confirmation.",
                                            username);
                                    return new UserNotFoundException(
                                            "User with username " + username + " not found.");
                                });

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            LOG.warnf("User %s has no email address, cannot resend confirmation.", username);
            throw new IllegalArgumentException(
                    "User has no email address to send confirmation to.");
        }

        // Find existing email verification entry
        EmailVerification emailVerification =
                emailVerificationRepository.findByUserIdOptional(user.id).orElse(null);

        if (emailVerification != null) {
            // Update existing token's expiration time
            emailService.updateEmailVerificationExpiration(emailVerification);
            LOG.debugf("Existing email verification token for user ID %d updated.", user.id);
        } else {
            // Create a new email verification if none exists
            emailVerification = emailService.createEmailVerification(user);
            LOG.debugf("New email verification token created for user ID %d.", user.id);
        }

        // Send email confirmation with the (updated or new) email verification
        emailService.sendEmailConfirmation(user, emailVerification);
        LOG.infof("Email confirmation resent to %s for user ID: %d", user.getEmail(), user.id);
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
