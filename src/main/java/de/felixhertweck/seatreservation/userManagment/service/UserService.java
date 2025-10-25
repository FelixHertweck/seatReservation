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
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.SendEmailException;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerificationCodeNotFoundException;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerifyTokenExpiredException;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    @Inject UserRepository userRepository;

    @Inject EmailService emailService;

    @Inject EmailVerificationRepository emailVerificationRepository;

    /**
     * Imports a set of users from the provided DTOs. Send directly email verification if email is
     * set.
     *
     * @param adminUserCreationDtos The set of user creation DTOs to import.
     * @return A set of UserDTOs representing the imported users.
     */
    @Transactional
    public Set<UserDTO> importUsers(Set<AdminUserCreationDto> adminUserCreationDtos)
            throws InvalidUserException, DuplicateUserException {
        LOG.infof("Importing %d users.", adminUserCreationDtos.size());

        Set<UserDTO> importedUsers = new HashSet<>();
        for (AdminUserCreationDto adminUser : adminUserCreationDtos) {
            UserDTO user = createUser(new UserCreationDTO(adminUser), adminUser.getRoles(), false);
            importedUsers.add(user);
        }
        return importedUsers;
    }

    /**
     * Creates a new user with the provided dto.
     *
     * @param userCreationDTO The DTO containing user creation dto.
     * @param roles The roles to assign to the user.
     * @param sendEmailVerification Whether to send an email verification if email is set.
     * @return The created UserDTO.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws SendEmailException If an error occurs while sending email confirmation.
     */
    @Transactional
    public UserDTO createUser(
            UserCreationDTO userCreationDTO, Set<String> roles, boolean sendEmailVerification)
            throws InvalidUserException, DuplicateUserException {
        if (userCreationDTO == null) {
            LOG.warn("UserCreationDTO is null during user creation.");
            throw new InvalidUserException("User creation data cannot be null.");
        }

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
        String email = null;

        if (userCreationDTO.getEmail() != null && !userCreationDTO.getEmail().trim().isEmpty()) {
            email = userCreationDTO.getEmail();
            LOG.debugf("User email set to: %s", email);
        }

        String salt = generateSalt();
        String passwordHash = BcryptUtil.bcryptHash(userCreationDTO.getPassword() + salt);

        User user =
                new User(
                        userCreationDTO.getUsername(),
                        email,
                        false,
                        false,
                        passwordHash,
                        salt,
                        userCreationDTO.getFirstname(),
                        userCreationDTO.getLastname(),
                        roles,
                        userCreationDTO.getTags());

        LOG.debugf(
                "User object prepared: username=%s, firstname=%s, lastname=%s, roles=%s, tags=%s",
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getRoles(),
                user.getTags());

        userRepository.persist(user);
        LOG.debugf("User %s persisted successfully with ID: %d", user.getUsername(), user.id);

        if (sendEmailVerification) {
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                LOG.errorf(
                        "Email verification requested for user %s, but no email address is set.",
                        user.getUsername());
                throw new SendEmailException(
                        "Email verification requested, but user has no email address.");
            }
            try {
                LOG.debugf("Attempting to send email confirmation to %s", user.getEmail());

                emailVerificationRepository.deleteByUserId(user.id);

                EmailVerification emailVerification = emailService.createEmailVerification(user);
                emailService.sendEmailConfirmation(user, emailVerification);
                user.setEmailVerificationSent(true);
            } catch (IOException e) {
                LOG.errorf(
                        e,
                        "Failed to send email confirmation to %s for user ID %d: %s",
                        user.getEmail(),
                        user.id,
                        e.getMessage());
                throw new SendEmailException(
                        "Failed to send email confirmation: " + e.getMessage());
            }
        } else {
            LOG.debug("Skipping email verification as per the flag.");
        }

        LOG.infof("User created successfully: %s", user.getUsername());

        return new UserDTO(user);
    }

    private void updateUserCore(
            User existingUser,
            String email,
            String firstname,
            String lastname,
            String password,
            Set<String> tags,
            boolean sendEmailVerification,
            boolean markEmailAsVerified) {
        LOG.debugf("Entering updateUserCore for user ID: %d", existingUser.id);

        if (markEmailAsVerified != existingUser.isEmailVerified()) {
            LOG.debugf(
                    "Updating emailVerified for user ID %d from %s to %s",
                    existingUser.id,
                    existingUser.isEmailVerified().toString(),
                    Boolean.toString(markEmailAsVerified));
            existingUser.setEmailVerified(markEmailAsVerified);
        }

        if (!Objects.equals(email, existingUser.getEmail())) {
            LOG.debugf(
                    "Updating email for user ID %d from %s to %s",
                    existingUser.id, existingUser.getEmail(), email);
            existingUser.setEmail(email);
            existingUser.setEmailVerificationSent(false);
            emailVerificationRepository.deleteByUserId(existingUser.id);

            if (email != null
                    && !email.trim().isEmpty()
                    && !markEmailAsVerified
                    && sendEmailVerification) {
                try {
                    LOG.debugf(
                            "Sending email confirmation to %s for user ID %d due to email change.",
                            existingUser.getEmail(), existingUser.id);
                    EmailVerification emailVerification =
                            emailService.createEmailVerification(existingUser);
                    emailService.sendEmailConfirmation(existingUser, emailVerification);
                    existingUser.setEmailVerificationSent(true);
                    LOG.debugf(
                            "Email confirmation sent to %s for user ID: %d",
                            existingUser.getEmail(), existingUser.id);
                } catch (IOException e) {
                    LOG.errorf(
                            e,
                            "Failed to send email confirmation to %s for user ID %d: %s",
                            existingUser.getEmail(),
                            existingUser.id,
                            e.getMessage());
                    throw new SendEmailException(
                            "Failed to send email confirmation: " + e.getMessage());
                }
            }
        } else if (email != null
                && !email.trim().isEmpty()
                && !markEmailAsVerified
                && sendEmailVerification) {
            try {
                LOG.debugf(
                        "Resending email confirmation to %s for user ID %d.",
                        existingUser.getEmail(), existingUser.id);
                resendEmailConfirmation(existingUser.getUsername());
                existingUser.setEmailVerificationSent(true);
                LOG.debugf(
                        "Email confirmation resent to %s for user ID: %d",
                        existingUser.getEmail(), existingUser.id);
            } catch (IOException e) {
                LOG.errorf(
                        e,
                        "Failed to resend email confirmation to %s for user ID %d: %s",
                        existingUser.getEmail(),
                        existingUser.id,
                        e.getMessage());
                throw new SendEmailException(
                        "Failed to resend email confirmation: " + e.getMessage());
            }
        }

        // Update firstname
        if (!Objects.equals(firstname, existingUser.getFirstname())) {
            LOG.debugf(
                    "Updating firstname for user ID %d from %s to %s",
                    existingUser.id, existingUser.getFirstname(), firstname);
            existingUser.setFirstname(firstname);
        }

        // Update lastname
        if (!Objects.equals(lastname, existingUser.getLastname())) {
            LOG.debugf(
                    "Updating lastname for user ID %d from %s to %s",
                    existingUser.id, existingUser.getLastname(), lastname);
            existingUser.setLastname(lastname);
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
                LOG.debugf(
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
                throw new SendEmailException(
                        "Failed to send password changed notification email: " + e.getMessage());
            }
        }

        // Update tags if changed
        if (!Objects.equals(tags, existingUser.getTags())) {
            LOG.debugf(
                    "Updating tags for user ID %d from %s to %s",
                    existingUser.id, existingUser.getTags(), tags);
            existingUser.setTags(tags);
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
     * @throws SendEmailException If an error occurs while sending email confirmation.
     */
    @Transactional
    public UserDTO updateUser(Long id, AdminUserUpdateDTO user) throws UserNotFoundException {
        if (user == null) {
            LOG.warnf("AdminUserUpdateDTO is null for user ID: %d.", id);
            throw new InvalidUserException("User update data cannot be null.");
        }

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
                user.getPassword(),
                user.getTags(),
                user.getSendEmailVerification(),
                user.getEmailVerified());

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
        LOG.debugf("Attempting to delete user with ID: %d.", id);
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            LOG.warnf("User with ID %d not found for deletion.", id);
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        LOG.infof("User with ID %d deleted successfully.", id);
    }

    public UserDTO getUserById(Long id) {
        LOG.debugf("Attempting to retrieve user with ID: %d.", id);
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
        List<LimitedUserInfoDTO> users =
                userRepository.listAll().stream().map(LimitedUserInfoDTO::new).toList();
        LOG.debugf("Returning %d limited user info DTOs.", users.size());
        return users;
    }

    public List<UserDTO> getUsersAsAdmin() {
        List<UserDTO> users = userRepository.listAll().stream().map(UserDTO::new).toList();
        LOG.debugf("Returning %d user DTOs for admin view.", users.size());
        return users;
    }

    public List<String> getAvailableRoles() {
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

        LOG.debugf("Attempting to update user profile for username: %s.", username);

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

        boolean markEmailAsVerified =
                existingUser.isEmailVerified()
                        && Objects.equals(existingUser.getEmail(), userProfileUpdateDTO.getEmail());

        updateUserCore(
                existingUser,
                userProfileUpdateDTO.getEmail(),
                userProfileUpdateDTO.getFirstname(),
                userProfileUpdateDTO.getLastname(),
                userProfileUpdateDTO.getPassword(),
                userProfileUpdateDTO.getTags(),
                true,
                markEmailAsVerified);

        userRepository.persist(existingUser);
        LOG.infof("User profile for username %s updated successfully.", username);
        return new UserDTO(existingUser);
    }

    /**
     * Verifies the email address of a user using only the 6-digit verification code.
     *
     * @param verificationCode The 6-digit verification code to verify.
     * @return The email address of the user if verification is successful.
     * @throws IllegalArgumentException If the verification code format is invalid (null, empty, or
     *     not 6 digits).
     * @throws VerificationCodeNotFoundException If the verification code is not found.
     * @throws VerifyTokenExpiredException If the verification code has expired.
     */
    @Transactional
    public String verifyEmailWithCode(String verificationCode) throws VerifyTokenExpiredException {
        LOG.debugf("Attempting to verify email with verification code.");

        // Validate verification code
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            LOG.warn("Invalid verification code provided for email verification.");
            throw new IllegalArgumentException("Invalid verification code");
        }

        // Validate that it's a 6-digit code
        if (!verificationCode.matches("\\d{6}")) {
            LOG.warnf("Invalid verification code format: %s", verificationCode);
            throw new IllegalArgumentException("Verification code must be 6 digits");
        }

        // Get the email verification record by token
        EmailVerification emailVerification =
                emailVerificationRepository.findByToken(verificationCode);
        if (emailVerification == null) {
            LOG.warnf("Email verification record not found for code: %s", verificationCode);
            throw new VerificationCodeNotFoundException("Verification code not found");
        }
        LOG.debugf("Email verification record found for code: %s", verificationCode);

        // Check if the token has expired
        if (emailVerification.getExpirationTime().isBefore(Instant.now())) {
            LOG.warnf(
                    "Email verification code has expired. Expiration time: %s, Current time: %s",
                    emailVerification.getExpirationTime(), Instant.now());
            throw new VerifyTokenExpiredException("Verification code expired");
        }
        LOG.debugf("Verification code is not expired.");

        // Delete the email verification record
        emailVerificationRepository.deleteById(emailVerification.id);
        LOG.debugf("Email verification record deleted for code: %s", verificationCode);

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
        LOG.debugf("Attempting to resend email confirmation for username: %s", username);

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
        user.setEmailVerificationSent(true);
        LOG.infof("Email confirmation resent to %s for user ID: %d", user.getEmail(), user.id);
    }

    private String generateSalt() {
        byte[] salt = SecurityUtils.generateRandomBytes(16);
        return Base64.getEncoder().encodeToString(salt);
    }
}
