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
package de.felixhertweck.seatreservation.usermanagement.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.exceptions.EmailCooldownException;
import de.felixhertweck.seatreservation.security.exceptions.InvalidTwoFactorCodeException;
import de.felixhertweck.seatreservation.security.service.EmailCooldownService;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
import de.felixhertweck.seatreservation.supervisor.service.BoxOfficeService;
import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserCreationDto;
import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserTagUpdateRequestDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserTagUpdateResultDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportFailureDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportFailureReason;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportResultDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserTagAssignmentRequestDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserTagAssignmentResultDTO;
import de.felixhertweck.seatreservation.usermanagement.exceptions.SendEmailException;
import de.felixhertweck.seatreservation.usermanagement.exceptions.VerificationCodeNotFoundException;
import de.felixhertweck.seatreservation.usermanagement.exceptions.VerifyTokenExpiredException;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final Validator validator;
    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final TwoFactorService twoFactorService;
    private final EmailCooldownService emailCooldownService;

    @Inject
    public UserService(
            UserRepository userRepository,
            EmailService emailService,
            EmailVerificationRepository emailVerificationRepository,
            TwoFactorService twoFactorService,
            EmailCooldownService emailCooldownService,
            Validator validator) {
        this.validator = validator;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.emailVerificationRepository = emailVerificationRepository;
        this.twoFactorService = twoFactorService;
        this.emailCooldownService = emailCooldownService;
    }

    // Usernames that must never be chosen by self-registration, admin creation, import, or
    // passkey registration -- "boxoffice" is the shared system account reservations at the box
    // office are booked under (see supervisor.service.BoxOfficeService).
    private static final Set<String> RESERVED_USERNAMES = Set.of("boxoffice");

    /**
     * Imports a batch of users. Users without a conflict are created; all others are reported in
     * the result instead of aborting the import. Usernames are compared case-insensitively.
     *
     * @param adminUserCreationDtos The users to import.
     * @return The created users and the users that could not be created, with the reason.
     */
    @Transactional
    public UserImportResultDTO importUsers(List<AdminUserCreationDto> adminUserCreationDtos) {
        LOG.infof("Importing %d users.", adminUserCreationDtos.size());

        Set<String> requestedUsernames =
                adminUserCreationDtos.stream()
                        .map(AdminUserCreationDto::getUsername)
                        .filter(name -> name != null && !name.isBlank())
                        .map(UserService::usernameKey)
                        .collect(Collectors.toSet());
        Set<String> knownExistingUsernames =
                userRepository.findExistingUsernames(requestedUsernames).stream()
                        .map(UserService::usernameKey)
                        .collect(Collectors.toCollection(HashSet::new));
        Set<String> seenInBatch = new HashSet<>();

        List<UserDTO> created = new ArrayList<>();
        List<UserImportFailureDTO> failed = new ArrayList<>();
        for (AdminUserCreationDto adminUser : adminUserCreationDtos) {
            if (adminUser == null) {
                failed.add(
                        new UserImportFailureDTO(
                                null, UserImportFailureReason.INVALID, "Empty user entry."));
                continue;
            }
            String username = adminUser.getUsername();
            String violations =
                    validator.validate(adminUser).stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .sorted()
                            .collect(Collectors.joining("; "));
            if (!violations.isEmpty()) {
                failed.add(
                        new UserImportFailureDTO(
                                username, UserImportFailureReason.INVALID, violations));
                continue;
            }
            if (username == null || username.isBlank()) {
                failed.add(
                        new UserImportFailureDTO(
                                username,
                                UserImportFailureReason.INVALID,
                                "Username cannot be empty."));
                continue;
            }
            String key = usernameKey(username);
            if (RESERVED_USERNAMES.contains(key)) {
                failed.add(
                        new UserImportFailureDTO(
                                username,
                                UserImportFailureReason.RESERVED_USERNAME,
                                "Username '" + username + "' is reserved."));
            } else if (seenInBatch.contains(key)) {
                failed.add(
                        new UserImportFailureDTO(
                                username,
                                UserImportFailureReason.DUPLICATE_IN_BATCH,
                                "Username '" + username + "' appears more than once."));
            } else {
                seenInBatch.add(key);
                if (knownExistingUsernames.contains(key)) {
                    failed.add(usernameExists(username));
                    continue;
                }
                try {
                    created.add(
                            createUser(
                                    new UserCreationDTO(adminUser),
                                    adminUser.getRoles(),
                                    false,
                                    false,
                                    Boolean.TRUE.equals(adminUser.getEmailVerified()),
                                    knownExistingUsernames));
                } catch (InvalidUserException e) {
                    failed.add(
                            new UserImportFailureDTO(
                                    username, UserImportFailureReason.INVALID, e.getMessage()));
                } catch (DuplicateUserException e) {
                    failed.add(usernameExists(username));
                }
            }
        }
        LOG.infof("Import finished: %d created, %d failed.", created.size(), failed.size());
        return new UserImportResultDTO(created, failed);
    }

    private static UserImportFailureDTO usernameExists(String username) {
        return new UserImportFailureDTO(
                username,
                UserImportFailureReason.USERNAME_EXISTS,
                "User with username " + username + " already exists.");
    }

    /** Normalizes a username for duplicate checks, which ignore upper/lower case. */
    private static String usernameKey(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Replaces an existing user: deletes it together with everything that belongs to it and creates
     * it anew, in a single transaction. If anything fails, the existing user is kept.
     *
     * @param existingUserId the user to replace
     * @param replacement the data of the new user; must have the same username as the existing one
     * @param currentUser the acting admin, who cannot replace their own account
     * @return the newly created user, which has a new id
     */
    @Transactional
    public UserDTO replaceUser(
            UUID existingUserId, AdminUserCreationDto replacement, AuthenticatedUser currentUser)
            throws UserNotFoundException, AccessDeniedException {
        User existing =
                userRepository
                        .findByIdOptional(existingUserId)
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                "User with id " + existingUserId + " not found."));
        if (replacement.getUsername() == null
                || !usernameKey(existing.getUsername())
                        .equals(usernameKey(replacement.getUsername()))) {
            throw new InvalidUserException(
                    "The replacement must have the username of the replaced user.");
        }
        // Validates that the acting admin and the reserved system account are not deleted.
        deleteUser(List.of(existingUserId), currentUser);
        return createUser(
                new UserCreationDTO(replacement),
                replacement.getRoles(),
                false,
                false,
                Boolean.TRUE.equals(replacement.getEmailVerified()),
                null);
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
        return createUser(userCreationDTO, roles, sendEmailVerification, false);
    }

    /**
     * Creates a new user.
     *
     * @param userCreationDTO the data for the new user
     * @param roles the roles to assign
     * @param sendEmailVerification whether to send an email verification
     * @param allowNoPassword when {@code true}, the user may be created without a password (e.g. a
     *     passkey-only account); the stored password hash is {@code null} and password login is
     *     unavailable for this user until a password is set
     * @return the created user
     */
    @Transactional
    public UserDTO createUser(
            UserCreationDTO userCreationDTO,
            Set<String> roles,
            boolean sendEmailVerification,
            boolean allowNoPassword)
            throws InvalidUserException, DuplicateUserException {
        return createUser(
                userCreationDTO, roles, sendEmailVerification, allowNoPassword, false, null);
    }

    /**
     * Creates a new user.
     *
     * @param userCreationDTO the data for the new user
     * @param roles the roles to assign
     * @param sendEmailVerification whether to send an email verification
     * @param allowNoPassword when {@code true}, the user may be created without a password (e.g. a
     *     passkey-only account); the stored password hash is {@code null} and password login is
     *     unavailable for this user until a password is set
     * @param markEmailAsVerified when {@code true}, the new user's email is marked as verified
     *     immediately (e.g. an admin creating a pre-verified account); {@code
     *     sendEmailVerification} is ignored in that case since there is nothing left to verify
     * @param knownExistingUsernames when non-{@code null}, the duplicate-username check is done
     *     against this in-memory set of lower-cased usernames instead of a per-call database query;
     *     used by {@link #importUsers} to check all usernames of a batch in a single upfront query.
     *     The newly created user's username is added to the set so later entries in the same batch
     *     still detect duplicates against each other.
     * @return the created user
     */
    @Transactional
    public UserDTO createUser(
            UserCreationDTO userCreationDTO,
            Set<String> roles,
            boolean sendEmailVerification,
            boolean allowNoPassword,
            boolean markEmailAsVerified,
            Set<String> knownExistingUsernames)
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
        boolean passwordProvided =
                userCreationDTO.getPassword() != null
                        && !userCreationDTO.getPassword().trim().isEmpty();
        if (!passwordProvided && !allowNoPassword) {
            LOG.warn("Password is empty or null during user creation.");
            throw new InvalidUserException("Password cannot be empty.");
        }

        if (RESERVED_USERNAMES.contains(userCreationDTO.getUsername().trim().toLowerCase())) {
            LOG.warnf(
                    "Attempt to create user with reserved username: %s",
                    userCreationDTO.getUsername());
            throw new DuplicateUserException(
                    "Username '"
                            + userCreationDTO.getUsername()
                            + "' is reserved and cannot be"
                            + " used.");
        }

        boolean isDuplicate =
                knownExistingUsernames != null
                        ? knownExistingUsernames.contains(
                                usernameKey(userCreationDTO.getUsername()))
                        : userRepository.existsByUsername(userCreationDTO.getUsername());
        if (isDuplicate) {
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

        String salt = null;
        String passwordHash = null;
        if (passwordProvided) {
            salt = generateSalt();
            passwordHash = BcryptUtil.bcryptHash(userCreationDTO.getPassword() + salt);
        } else {
            LOG.debugf(
                    "Creating user %s without a password (passkey-only account).",
                    userCreationDTO.getUsername());
        }

        User user =
                new User(
                        userCreationDTO.getUsername(),
                        email,
                        markEmailAsVerified,
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
        LOG.debugf("User %s persisted successfully with ID: %s", user.getUsername(), user.id);
        if (knownExistingUsernames != null) {
            knownExistingUsernames.add(usernameKey(user.getUsername()));
        }

        if (sendEmailVerification && !markEmailAsVerified) {
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                LOG.errorf(
                        "Email verification requested for user %s, but no email address is set.",
                        user.getUsername());
                throw new SendEmailException(
                        "Email verification requested, but user has no email address.");
            }
            LOG.debugf("Attempting to send email confirmation to %s", user.getEmail());

            emailVerificationRepository.deleteByUserId(user.id);

            EmailVerification emailVerification = emailService.createEmailVerification(user);
            emailService.sendEmailConfirmation(user, emailVerification);
            user.setEmailVerificationSent(true);
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
        LOG.debugf("Entering updateUserCore for user ID: %s", existingUser.id);

        if (markEmailAsVerified != existingUser.isEmailVerified()) {
            LOG.debugf(
                    "Updating emailVerified for user ID %s from %s to %s",
                    existingUser.id,
                    Boolean.toString(existingUser.isEmailVerified()),
                    Boolean.toString(markEmailAsVerified));
            existingUser.setEmailVerified(markEmailAsVerified);
        }

        if (!Objects.equals(email, existingUser.getEmail())) {
            LOG.debugf(
                    "Updating email for user ID %s from %s to %s",
                    existingUser.id, existingUser.getEmail(), email);
            existingUser.setEmail(email);
            existingUser.setEmailVerificationSent(false);
            emailVerificationRepository.deleteByUserId(existingUser.id);

            // The new address hasn't been verified, so it can no longer be trusted for
            // email-based 2FA -- applies to both self-service and admin-driven updates.
            twoFactorService.disableEmailFactorForEmailChange(existingUser);

            if (email != null
                    && !email.trim().isEmpty()
                    && !markEmailAsVerified
                    && sendEmailVerification) {
                LOG.debugf(
                        "Sending email confirmation to %s for user ID %s due to email change.",
                        existingUser.getEmail(), existingUser.id);
                EmailVerification emailVerification =
                        emailService.createEmailVerification(existingUser);
                emailService.sendEmailConfirmation(existingUser, emailVerification);
                existingUser.setEmailVerificationSent(true);
                LOG.debugf(
                        "Email confirmation sent to %s for user ID: %s",
                        existingUser.getEmail(), existingUser.id);
            }
        } else if (email != null
                && !email.trim().isEmpty()
                && !markEmailAsVerified
                && sendEmailVerification) {
            try {
                LOG.debugf(
                        "Resending email confirmation to %s for user ID %s.",
                        existingUser.getEmail(), existingUser.id);
                resendEmailConfirmation(existingUser.getUsername());
                existingUser.setEmailVerificationSent(true);
                LOG.debugf(
                        "Email confirmation resent to %s for user ID: %s",
                        existingUser.getEmail(), existingUser.id);
            } catch (IOException e) {
                LOG.errorf(
                        e,
                        "Failed to resend email confirmation to %s for user ID %s: %s",
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
                    "Updating firstname for user ID %s from %s to %s",
                    existingUser.id, existingUser.getFirstname(), firstname);
            existingUser.setFirstname(firstname);
        }

        // Update lastname
        if (!Objects.equals(lastname, existingUser.getLastname())) {
            LOG.debugf(
                    "Updating lastname for user ID %s from %s to %s",
                    existingUser.id, existingUser.getLastname(), lastname);
            existingUser.setLastname(lastname);
        }

        // Update password if provided
        if (password != null && !password.trim().isEmpty()) {
            LOG.debugf("Updating password for user ID %s.", existingUser.id);
            String newSalt = generateSalt();
            existingUser.setPasswordSalt(newSalt);
            existingUser.setPasswordHash(
                    BcryptUtil.bcryptHash(
                            password + newSalt)); // Hash the new password with new salt
            // Send password changed notification email
            LOG.debugf(
                    "Sending password changed notification to %s for user ID %s.",
                    existingUser.getEmail(), existingUser.id);
            emailService.sendPasswordChangedNotification(existingUser);
            LOG.debugf(
                    "Password changed notification sent to %s for user ID: %s",
                    existingUser.getEmail(), existingUser.id);
        }

        // Update tags if changed
        if (!Objects.equals(tags, existingUser.getTags())) {
            LOG.debugf(
                    "Updating tags for user ID %s from %s to %s",
                    existingUser.id, existingUser.getTags(), tags);
            existingUser.setTags(tags);
        }
        LOG.debugf("Exiting updateUserCore for user ID: %s", existingUser.id);
    }

    /**
     * Updates an existing user with the provided dto.
     *
     * @param id The ID of the user to update.
     * @param user The DTO containing user profile update data.
     * @param currentUser The authenticated user performing the update. Required (must not be null)
     *     whenever {@code user.getRoles()} is non-null, so role changes can be checked against
     *     self-lockout rules.
     * @return The updated UserDTO.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws SendEmailException If an error occurs while sending email confirmation.
     * @throws AccessDeniedException If roles are being updated without a known authenticated user,
     *     if an admin attempts to remove their own admin role, or if roles are being granted to the
     *     reserved "boxoffice" system account.
     */
    @Transactional
    public UserDTO updateUser(UUID id, AdminUserUpdateDTO user, AuthenticatedUser currentUser)
            throws UserNotFoundException, AccessDeniedException {
        if (user == null) {
            LOG.warnf("AdminUserUpdateDTO is null for user ID: %s.", id);
            throw new InvalidUserException("User update data cannot be null.");
        }

        LOG.debugf("AdminUserUpdateDTO for ID %s: %s", id, user.toString());

        User existingUser =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf("User with ID %s not found for update.", id);
                                    return new UserNotFoundException(
                                            "User with id " + id + " not found.");
                                });

        if (user.getRoles() != null) {
            if (currentUser == null) {
                LOG.warnf(
                        "Refusing role update for user ID %s: no authenticated user available to"
                                + " check self-lockout.",
                        id);
                throw new AccessDeniedException("Authenticated user is required to update roles.");
            }
            if (id.equals(currentUser.id()) && !user.getRoles().contains(Roles.ADMIN)) {
                LOG.warnf("User %s attempted to remove their own admin role.", currentUser.id());
                throw new AccessDeniedException("Admins cannot remove their own admin role.");
            }
            if ("boxoffice".equalsIgnoreCase(existingUser.getUsername())
                    && !user.getRoles().isEmpty()) {
                LOG.warnf(
                        "Attempt to grant roles to reserved system account: %s",
                        existingUser.getUsername());
                throw new AccessDeniedException(
                        "The box office system account must never be granted roles.");
            }
        }

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
                    "Updating roles for user ID %s from %s to %s",
                    existingUser.id, existingUser.getRoles(), user.getRoles());
            existingUser.setRoles(user.getRoles());
        }
        userRepository.persist(existingUser);
        LOG.infof("User with ID %s updated successfully by admin.", existingUser.id);
        return new UserDTO(existingUser);
    }

    /**
     * Deletes a user by ID.
     *
     * @param ids The IDs of the users to delete.
     * @param currentUser The currently authenticated user performing the deletion.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     * @throws AccessDeniedException If the user is attempting to delete their own account.
     */
    @Transactional
    public void deleteUser(List<UUID> ids, AuthenticatedUser currentUser)
            throws UserNotFoundException, AccessDeniedException {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LOG.debugf("Attempting to batch delete users with IDs: %s.", ids);

        // Fetch all users in a single query
        List<User> users = userRepository.findByIds(ids);

        // Map users by ID for quick lookup and to preserve iterative validation behavior
        Map<UUID, User> userMap = users.stream().collect(Collectors.toMap(u -> u.id, u -> u));

        for (UUID id : ids) {
            User user = userMap.remove(id);
            if (user == null) {
                LOG.warnf("User with ID %s not found for deletion.", id);
                throw new UserNotFoundException("User with id " + id + " not found.");
            }
            if (currentUser != null && id.equals(currentUser.id())) {
                LOG.warnf("User %s attempted to delete their own account.", currentUser.id());
                throw new AccessDeniedException("Admins cannot delete their own accounts.");
            }
            if (RESERVED_USERNAMES.contains(user.getUsername().toLowerCase())) {
                LOG.warnf("Attempt to delete reserved system account: %s", user.getUsername());
                throw new AccessDeniedException("The box office system account cannot be deleted.");
            }
        }

        userRepository.deleteByIds(ids);

        LOG.infof("Users with IDs %s deleted successfully.", ids);
    }

    public UserDTO getUserById(UUID id) {
        LOG.debugf("Attempting to retrieve user with ID: %s.", id);
        User user =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf("User with ID %s not found.", id);
                                    return new UserNotFoundException(
                                            "User with id " + id + " not found.");
                                });
        LOG.infof("User with ID %s retrieved successfully.", id);
        return new UserDTO(user);
    }

    public List<LimitedUserInfoDTO> getAllUsers() {
        List<LimitedUserInfoDTO> users =
                userRepository.findAllWithTagsAndRoles().stream()
                        .map(LimitedUserInfoDTO::new)
                        .toList();
        LOG.debugf("Returning %d limited user info DTOs.", users.size());
        return users;
    }

    /**
     * Adds a tag to all users with the given usernames. Existing tags are kept; nothing else about
     * the users is touched. Duplicate and blank usernames are ignored.
     *
     * @param request the usernames and the tag to add
     * @return the usernames grouped by outcome
     */
    @Transactional
    public UserTagAssignmentResultDTO addTagToUsers(UserTagAssignmentRequestDTO request) {
        String tag = request.tag().trim();
        if (tag.isEmpty()) {
            throw new InvalidUserException("Tag cannot be blank.");
        }
        Set<String> usernames =
                request.usernames().stream()
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // The boxoffice system account is not a real person and must not be tagged.
        Map<String, User> found = new HashMap<>();
        for (User user : userRepository.findByUsernamesWithTags(usernames)) {
            if (!BoxOfficeService.BOXOFFICE_USERNAME.equalsIgnoreCase(user.getUsername())) {
                found.put(user.getUsername(), user);
            }
        }

        List<String> updated = new ArrayList<>();
        List<String> alreadyTagged = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        for (String username : usernames) {
            User user = found.get(username);
            if (user == null) {
                notFound.add(username);
            } else if (user.getTags().contains(tag)) {
                alreadyTagged.add(username);
            } else {
                Set<String> tags = new HashSet<>(user.getTags());
                tags.add(tag);
                user.setTags(tags);
                updated.add(username);
            }
        }
        LOG.infof(
                "Added tag '%s' to %d users (%d already tagged, %d not found).",
                tag, updated.size(), alreadyTagged.size(), notFound.size());
        return new UserTagAssignmentResultDTO(updated, alreadyTagged, notFound);
    }

    /**
     * Adds and removes tags for several users at once. Only the listed tags are touched; all other
     * tags of the users stay as they are.
     *
     * @param request the user ids and the tags to add and remove
     * @return the user ids grouped by outcome
     */
    @Transactional
    public AdminUserTagUpdateResultDTO updateTagsForUsers(AdminUserTagUpdateRequestDTO request) {
        Set<String> addTags = trimmed(request.addTags());
        Set<String> removeTags = trimmed(request.removeTags());
        if (addTags.isEmpty() && removeTags.isEmpty()) {
            throw new InvalidUserException("At least one tag to add or remove is required.");
        }
        if (addTags.stream().anyMatch(removeTags::contains)) {
            throw new InvalidUserException("A tag cannot be added and removed at the same time.");
        }
        Set<UUID> ids = new LinkedHashSet<>(request.userIds());

        // The boxoffice system account is not a real person and must not be modified.
        Map<UUID, User> found = new HashMap<>();
        for (User user : userRepository.findByIdsWithTags(ids)) {
            if (!BoxOfficeService.BOXOFFICE_USERNAME.equalsIgnoreCase(user.getUsername())) {
                found.put(user.getId(), user);
            }
        }

        List<UUID> updated = new ArrayList<>();
        List<UUID> unchanged = new ArrayList<>();
        List<UUID> notFound = new ArrayList<>();
        for (UUID id : ids) {
            User user = found.get(id);
            if (user == null) {
                notFound.add(id);
                continue;
            }
            Set<String> tags = new HashSet<>(user.getTags());
            boolean changed = tags.addAll(addTags);
            changed |= tags.removeAll(removeTags);
            if (changed) {
                user.setTags(tags);
                updated.add(id);
            } else {
                unchanged.add(id);
            }
        }
        LOG.infof(
                "Bulk tag update: +%s -%s, %d users updated, %d unchanged, %d not found.",
                addTags, removeTags, updated.size(), unchanged.size(), notFound.size());
        return new AdminUserTagUpdateResultDTO(updated, unchanged, notFound);
    }

    private static Set<String> trimmed(Set<String> tags) {
        return tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<UserDTO> getUsersAsAdmin() {
        List<UserDTO> users =
                userRepository.findAllWithTagsAndRoles().stream().map(UserDTO::new).toList();
        LOG.debugf("Returning %d user DTOs for admin view.", users.size());
        return users;
    }

    public List<String> getAvailableRoles() {
        List<String> roles = Roles.ALL_ROLES;
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

        boolean emailChanging =
                !Objects.equals(existingUser.getEmail(), userProfileUpdateDTO.getEmail());

        // Changing the email while 2FA is enabled requires proving current possession of 2FA,
        // otherwise a hijacked session could swap the email and later use it to take over the
        // account (e.g. via password reset).
        if (emailChanging
                && existingUser.isTwoFactorEnabled()
                && !twoFactorService.verifyCurrentTwoFactorCode(
                        existingUser, userProfileUpdateDTO.getTwoFactorCode())) {
            throw new InvalidTwoFactorCodeException(
                    "A valid 2FA code is required to change your email address.");
        }

        boolean markEmailAsVerified =
                existingUser.isEmailVerified()
                        && Objects.equals(existingUser.getEmail(), userProfileUpdateDTO.getEmail());

        updateUserCore(
                existingUser,
                userProfileUpdateDTO.getEmail(),
                userProfileUpdateDTO.getFirstname(),
                userProfileUpdateDTO.getLastname(),
                userProfileUpdateDTO.getPassword(),
                existingUser.getTags(), // tags are admin-managed, users cannot change them
                true,
                markEmailAsVerified);

        userRepository.persist(existingUser);
        LOG.infof("User profile for username %s updated successfully.", username);
        return new UserDTO(existingUser);
    }

    /**
     * Verifies the email address of a user using only the 6-digit verification code.
     *
     * @param verificationCode The 6-digit verification code to verify (must not be null or empty).
     * @return The email address of the user if verification is successful.
     * @throws ValidationException If the verification code format is invalid (null, empty, or not 6
     *     digits).
     * @throws VerificationCodeNotFoundException If the verification code is not found.
     * @throws VerifyTokenExpiredException If the verification code has expired.
     */
    @Transactional
    public String verifyEmailWithCode(String verificationCode) throws VerifyTokenExpiredException {
        LOG.debugf("Attempting to verify email with verification code.");

        // Validate verification code
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            LOG.warn("Invalid verification code provided for email verification.");
            throw new ValidationException("Invalid verification code");
        }

        // Validate that it's a 6-digit code
        if (!verificationCode.matches("\\d{6}")) {
            LOG.warnf("Invalid verification code format: %s", verificationCode);
            throw new ValidationException("Verification code must be 6 digits");
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
        LOG.infof("Email for user ID %s (%s) marked as verified.", user.id, user.getEmail());

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
            throw new ValidationException("User has no email address to send confirmation to.");
        }

        Optional<Instant> retryAfter =
                emailCooldownService.checkAndRecord(
                        EmailCooldownService.Purpose.EMAIL_CONFIRMATION_RESEND, user.id.toString());
        if (retryAfter.isPresent()) {
            throw new EmailCooldownException(
                    "Please wait before requesting another confirmation email.", retryAfter.get());
        }

        // Find existing email verification entry
        EmailVerification emailVerification =
                emailVerificationRepository.findByUserIdOptional(user.id).orElse(null);

        if (emailVerification != null) {
            // Update existing token's expiration time
            emailService.updateEmailVerificationExpiration(emailVerification);
            LOG.debugf("Existing email verification token for user ID %s updated.", user.id);
        } else {
            // Create a new email verification if none exists
            emailVerification = emailService.createEmailVerification(user);
            LOG.debugf("New email verification token created for user ID %s.", user.id);
        }

        // Send email confirmation with the (updated or new) email verification
        emailService.sendEmailConfirmation(user, emailVerification);
        user.setEmailVerificationSent(true);
        LOG.infof("Email confirmation resent to %s for user ID: %s", user.getEmail(), user.id);
    }

    private String generateSalt() {
        byte[] salt = SecurityUtils.generateRandomBytes(16);
        return Base64.getEncoder().encodeToString(salt);
    }
}
