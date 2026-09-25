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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import de.felixhertweck.seatreservation.model.repository.TwoFactorAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.TwoFactorBackupCodeRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.exceptions.InvalidTwoFactorCodeException;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
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
import de.felixhertweck.seatreservation.usermanagement.exceptions.VerificationCodeNotFoundException;
import de.felixhertweck.seatreservation.usermanagement.exceptions.VerifyTokenExpiredException;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class UserServiceTest {

    @InjectMock UserRepository userRepository;

    @InjectMock EmailService emailService;

    @InjectMock EmailVerificationRepository emailVerificationRepository;

    @InjectMock TwoFactorBackupCodeRepository backupCodeRepository;

    @InjectMock TwoFactorAttemptRepository twoFactorAttemptRepository;

    @Inject UserService userService;

    @BeforeEach
    void setUp() {
        Mockito.reset(
                userRepository,
                emailService,
                emailVerificationRepository,
                backupCodeRepository,
                twoFactorAttemptRepository);
    }

    @Test
    void createUser_Success_WithEmail() {
        UserCreationDTO dto =
                new UserCreationDTO(
                        "testuser", "test@example.com", "password", "John", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class)))
                .thenReturn(true); // Simulate successful persistence

        EmailVerification mockEmailVerification =
                new EmailVerification(
                        new User(
                                "mock",
                                "mock@example.com",
                                true,
                                false,
                                "hash",
                                "salt",
                                "Mock",
                                "User",
                                Set.of(),
                                Set.of()),
                        "token",
                        Instant.now());
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(mockEmailVerification);

        UserDTO createdUser = userService.createUser(dto, Set.of(Roles.USER), true);

        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.username());
        assertEquals("test@example.com", createdUser.email());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), eq(mockEmailVerification));
    }

    @Test
    void createUser_Success_WithoutEmail() {
        UserCreationDTO dto =
                new UserCreationDTO("testuser", null, "password", "John", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class))).thenReturn(true);

        UserDTO createdUser = userService.createUser(dto, Set.of(Roles.USER), false);

        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.username());
        assertNull(createdUser.email());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_Success_WithEmail_NoVerificationSent() {
        UserCreationDTO dto =
                new UserCreationDTO(
                        "testuser", "test@example.com", "password", "John", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class))).thenReturn(true);

        UserDTO createdUser = userService.createUser(dto, Set.of(Roles.USER), false);

        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.username());
        assertEquals("test@example.com", createdUser.email());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_InvalidUserException_NullDTO() {
        assertThrows(
                InvalidUserException.class,
                () -> userService.createUser(null, Set.of(Roles.USER), false));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_InvalidUserException_EmptyUsername() {
        final UserCreationDTO dto =
                new UserCreationDTO("", "test@example.com", "password", "John", "Doe", null);
        assertThrows(
                InvalidUserException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), false));

        final UserCreationDTO dto2 =
                new UserCreationDTO("   ", "test@example.com", "password", "John", "Doe", null);
        assertThrows(
                InvalidUserException.class,
                () -> userService.createUser(dto2, Set.of(Roles.USER), false));

        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_InvalidUserException_EmptyPassword() {
        final UserCreationDTO dto =
                new UserCreationDTO("testuser", "test@example.com", "", "John", "Doe", null);
        assertThrows(
                InvalidUserException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), false));

        final UserCreationDTO dto2 =
                new UserCreationDTO("testuser", "test@example.com", "   ", "John", "Doe", null);
        assertThrows(
                InvalidUserException.class,
                () -> userService.createUser(dto2, Set.of(Roles.USER), false));

        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void createUser_DuplicateUserException_ReservedUsername_Boxoffice() {
        final UserCreationDTO dto =
                new UserCreationDTO(
                        "boxoffice", "test@example.com", "password", "John", "Doe", null);

        assertThrows(
                DuplicateUserException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), false));
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void createUser_DuplicateUserException_ReservedUsername_CaseInsensitive() {
        final UserCreationDTO dto =
                new UserCreationDTO(
                        "BoxOffice", "test@example.com", "password", "John", "Doe", null);

        assertThrows(
                DuplicateUserException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), false));
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void createUser_DuplicateUserException_ExistingUsername() {
        final UserCreationDTO dto =
                new UserCreationDTO(
                        "existinguser", "test@example.com", "password", "John", "Doe", null);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(
                DuplicateUserException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), false));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_Success_WithDuplicateEmail() {
        UserCreationDTO dto =
                new UserCreationDTO(
                        "newuser", "existing@example.com", "password", "Jane", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class))).thenReturn(true);
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Set.of(),
                                        Set.of()),
                                "token",
                                Instant.now()));

        // Simulate that another user already has this email, but it should not prevent creation
        // (assuming email uniqueness is not enforced at this layer for creation)

        UserDTO createdUser = userService.createUser(dto, Set.of(Roles.USER), true);

        assertNotNull(createdUser);
        assertEquals("newuser", createdUser.username());
        assertEquals("existing@example.com", createdUser.email());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_Success_MarkEmailAsVerified_SkipsVerificationEmail() {
        UserCreationDTO dto =
                new UserCreationDTO(
                        "testuser", "test@example.com", "password", "John", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class))).thenReturn(true);

        UserDTO createdUser =
                userService.createUser(dto, Set.of(Roles.USER), true, false, true, null);

        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.username());
        assertTrue(createdUser.emailVerified());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_RuntimeException_EmailVerificationTrue_NoEmail() {
        UserCreationDTO dto =
                new UserCreationDTO("testuser", null, "password", "John", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class))).thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), true));

        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_UpdateFirstname()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "Old",
                        "User",
                        Collections.singleton(Roles.USER),
                        Set.of());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        "New",
                        existingUser.getLastname(),
                        null,
                        existingUser.getEmail(),
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("New", updatedUser.firstname());
        assertEquals("User", updatedUser.lastname());
        assertEquals("old@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_UpdateLastname()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Old",
                        Collections.singleton(Roles.USER),
                        Set.of());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        existingUser.getFirstname(),
                        "New",
                        null,
                        existingUser.getEmail(),
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("John", updatedUser.firstname());
        assertEquals("New", updatedUser.lastname());
        assertEquals("old@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_UpdatePassword()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Set.of());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        existingUser.getFirstname(),
                        existingUser.getLastname(),
                        "newpassword",
                        existingUser.getEmail(),
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertTrue(
                BcryptUtil.matches(
                        "newpassword" + existingUser.getPasswordSalt(),
                        existingUser.getPasswordHash()));
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
        verify(emailService, times(1)).sendPasswordChangedNotification(any(User.class));
    }

    @Test
    void updateUser_Success_UpdatePassword_SendsEmail()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        existingUser.getFirstname(),
                        existingUser.getLastname(),
                        "newpassword",
                        existingUser.getEmail(),
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        userService.updateUser(id(1), dto, caller);

        verify(emailService, times(1)).sendPasswordChangedNotification(existingUser);
    }

    @Test
    void updateUser_Success_PasswordSaltChangesOnPasswordUpdate()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        String initialSalt = "initialSalt";
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        BcryptUtil.bcryptHash("oldpassword" + initialSalt),
                        initialSalt,
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        existingUser.getFirstname(),
                        existingUser.getLastname(),
                        "newpassword",
                        existingUser.getEmail(),
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        userService.updateUser(id(1), dto, caller);

        assertNotEquals(initialSalt, existingUser.getPasswordSalt());
        assertTrue(
                BcryptUtil.matches(
                        "newpassword" + existingUser.getPasswordSalt(),
                        existingUser.getPasswordHash()));
    }

    @Test
    void updateUser_Success_UpdateRoles()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        new HashSet<>(Collections.singletonList(Roles.USER)),
                        Collections.emptySet());
        existingUser.id = id(1);
        Set<String> newRoles = new HashSet<>(Arrays.asList(Roles.USER, Roles.ADMIN));
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        existingUser.getEmail(),
                        false,
                        false,
                        newRoles,
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals(newRoles, existingUser.getRoles());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_NoEmailChange()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        "New",
                        "Name",
                        "newpass",
                        existingUser.getEmail(),
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("New", updatedUser.firstname());
        assertEquals("Name", updatedUser.lastname());
        assertTrue(
                BcryptUtil.matches(
                        "newpass" + existingUser.getPasswordSalt(),
                        existingUser.getPasswordHash()));
        assertEquals("old@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_UpdateEmail()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        "new@example.com",
                        true,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty()); // No existing token
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.email());
        assertFalse(existingUser.isEmailVerified());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_UserNotFoundException() {
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        "New",
                        null,
                        null,
                        null,
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        when(userRepository.findByIdOptional(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(id(1), dto, null));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_InvalidUserException_NullDTO() {
        assertThrows(InvalidUserException.class, () -> userService.updateUser(id(1), null, null));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_WithDuplicateEmail()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        "duplicate@example.com",
                        true,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty()); // No existing token
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        // Simulate another user already has this email, but it should not prevent update
        // (assuming email uniqueness is not enforced at this layer for update)

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("duplicate@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_UpdateEmail_NoVerificationSent() throws UserNotFoundException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        "new@example.com",
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.email());
        assertFalse(existingUser.isEmailVerified()); // Sollte immer noch false sein
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_NoEmailChange_VerificationSentTrue() throws UserNotFoundException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        "old@example.com",
                        true,
                        true,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));
        UserDTO updatedUser = userService.updateUser(id(1), dto, caller);

        assertNotNull(updatedUser);
        assertEquals("old@example.com", updatedUser.email());
        assertTrue(existingUser.isEmailVerified()); // Sollte immer noch true sein
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void deleteUser_Success() throws UserNotFoundException {
        User existingUser =
                new User(
                        "testuser",
                        "test@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);

        when(userRepository.findByIds(List.of(id(1)))).thenReturn(List.of(existingUser));
        when(userRepository.deleteByIds(List.of(id(1)))).thenReturn(1L);

        // the caller has a different id
        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));

        userService.deleteUser(List.of(id(1)), caller);

        verify(userRepository, times(1)).deleteByIds(List.of(id(1)));
    }

    @Test
    void deleteUser_UserNotFoundException() throws UserNotFoundException {
        when(userRepository.findByIds(List.of(id(1)))).thenReturn(Collections.emptyList());

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));

        assertThrows(
                UserNotFoundException.class, () -> userService.deleteUser(List.of(id(1)), caller));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_SecurityException_CannotDeleteSelf() {
        User existingUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "Admin",
                        "User",
                        Collections.singleton(Roles.ADMIN),
                        Collections.emptySet());
        existingUser.id = id(1);

        when(userRepository.findByIds(List.of(id(1)))).thenReturn(List.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(1), Set.of(Roles.ADMIN));

        assertThrows(
                AccessDeniedException.class, () -> userService.deleteUser(List.of(id(1)), caller));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_SecurityException_CannotDeleteBoxofficeSystemAccount() {
        User boxofficeUser =
                new User(
                        "boxoffice",
                        null,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet());
        boxofficeUser.id = id(5);

        when(userRepository.findByIds(List.of(id(5)))).thenReturn(List.of(boxofficeUser));

        // Caller is a different admin, not deleting themselves -- only the reserved-username
        // guard should fire here.
        AuthenticatedUser caller = new AuthenticatedUser(id(1), Set.of(Roles.ADMIN));

        assertThrows(
                AccessDeniedException.class, () -> userService.deleteUser(List.of(id(5)), caller));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getUserById_Success() throws UserNotFoundException {
        User existingUser =
                new User(
                        "testuser",
                        "test@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));
        when(userRepository.findById(id(1))).thenReturn(existingUser); // Mock findById

        UserDTO foundUser = userService.getUserById(id(1));

        assertNotNull(foundUser);
        assertEquals(existingUser.id, foundUser.id());
    }

    @Test
    void getUserById_UserNotFoundException() throws UserNotFoundException {
        when(userRepository.findByIdOptional(any(UUID.class))).thenReturn(Optional.empty());
        when(userRepository.findById(any(UUID.class)))
                .thenReturn(null); // Mock findById to return null

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(id(1)));
    }

    @Test
    void getAllUsers_Success_WithUsers() {
        User user1 =
                new User(
                        "user1",
                        "a@a.com",
                        true,
                        false,
                        "h1",
                        "salt1",
                        "F1",
                        "L1",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        User user2 =
                new User(
                        "user2",
                        "b@b.com",
                        true,
                        false,
                        "h2",
                        "salt2",
                        "F2",
                        "L2",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        when(userRepository.findAllWithTagsAndRoles()).thenReturn(Arrays.asList(user1, user2));

        List<LimitedUserInfoDTO> users = userService.getAllUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.username().equals(user1.getUsername())));
        assertTrue(users.stream().anyMatch(u -> u.username().equals(user2.getUsername())));
    }

    @Test
    void getAllUsers_Success_NoUsers() {
        when(userRepository.findAllWithTagsAndRoles()).thenReturn(Collections.emptyList());

        List<LimitedUserInfoDTO> users = userService.getAllUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void getAvailableRoles_Success() {
        // Directly test the method without mocking it

        Set<String> roles =
                new HashSet<>(
                        userService.getAvailableRoles()); // Convert List to Set for comparison
        assertNotNull(roles);
        assertFalse(roles.isEmpty());
        assertTrue(roles.contains(Roles.USER));
        assertTrue(roles.contains(Roles.ADMIN));
        assertTrue(roles.contains(Roles.MANAGER));
    }

    @Test
    void updateUserProfile_Success_UpdateFirstname()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "Old",
                        "User",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        final UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO("New", "User", null, existingUser.getEmail(), null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertNotNull(updatedUser);
        assertEquals("New", updatedUser.firstname());
        assertEquals("User", updatedUser.lastname());
        assertEquals("old@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_Success_UpdateLastname()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Old",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        final UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO("John", "New", null, existingUser.getEmail(), null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertNotNull(updatedUser);
        assertEquals("John", updatedUser.firstname());
        assertEquals("New", updatedUser.lastname());
        assertEquals("old@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_Success_UpdatePassword()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        final UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO(null, null, "newpassword", null, null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertNotNull(updatedUser);
        assertTrue(
                BcryptUtil.matches(
                        "newpassword" + existingUser.getPasswordSalt(),
                        existingUser.getPasswordHash()));
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
        verify(emailService, times(1)).sendPasswordChangedNotification(any(User.class));
    }

    @Test
    void addTagToUsers_AddsTagKeepsExistingAndReportsOutcome() {
        User alice =
                new User(
                        "alice",
                        "a@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "A",
                        "A",
                        Set.of(Roles.USER),
                        Set.of("old"));
        User bob =
                new User(
                        "bob",
                        "b@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "B",
                        "B",
                        Set.of(Roles.USER),
                        Set.of("mitglied-2026"));
        User boxoffice =
                new User(
                        "boxoffice",
                        null,
                        true,
                        false,
                        "h",
                        "s",
                        "B",
                        "O",
                        Set.of(Roles.USER),
                        Set.of());
        when(userRepository.findByUsernamesWithTags(any()))
                .thenReturn(List.of(alice, bob, boxoffice));

        UserTagAssignmentResultDTO result =
                userService.addTagToUsers(
                        new UserTagAssignmentRequestDTO(
                                List.of("alice", " bob ", "boxoffice", "ghost", "alice"),
                                "mitglied-2026"));

        assertEquals(List.of("alice"), result.updated());
        assertEquals(List.of("bob"), result.alreadyTagged());
        assertEquals(List.of("boxoffice", "ghost"), result.notFound());
        assertEquals(Set.of("old", "mitglied-2026"), alice.getTags());
        assertTrue(boxoffice.getTags().isEmpty());
    }

    @Test
    void updateTagsForUsers_AddsAndRemovesOnlyListedTags() {
        User alice =
                new User(
                        "alice",
                        "a@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "A",
                        "A",
                        Set.of(Roles.USER),
                        Set.of("old", "keep"));
        alice.id = id(1);
        User bob =
                new User(
                        "bob",
                        "b@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "B",
                        "B",
                        Set.of(Roles.USER),
                        Set.of("new", "keep"));
        bob.id = id(2);
        User boxoffice =
                new User(
                        "boxoffice",
                        null,
                        true,
                        false,
                        "h",
                        "s",
                        "B",
                        "O",
                        Set.of(Roles.USER),
                        Set.of("old"));
        boxoffice.id = id(3);
        when(userRepository.findByIdsWithTags(any())).thenReturn(List.of(alice, bob, boxoffice));

        AdminUserTagUpdateResultDTO result =
                userService.updateTagsForUsers(
                        new AdminUserTagUpdateRequestDTO(
                                List.of(id(1), id(2), id(3), id(4)), Set.of("new"), Set.of("old")));

        assertEquals(List.of(id(1)), result.updated());
        assertEquals(List.of(id(2)), result.unchanged());
        assertEquals(List.of(id(3), id(4)), result.notFound());
        assertEquals(Set.of("new", "keep"), alice.getTags());
        assertEquals(Set.of("new", "keep"), bob.getTags());
        assertEquals(Set.of("old"), boxoffice.getTags());
    }

    @Test
    void updateTagsForUsers_NothingToDo_Throws() {
        assertThrows(
                InvalidUserException.class,
                () ->
                        userService.updateTagsForUsers(
                                new AdminUserTagUpdateRequestDTO(
                                        List.of(id(1)), Set.of(), Set.of())));
    }

    @Test
    void updateTagsForUsers_SameTagAddedAndRemoved_Throws() {
        assertThrows(
                InvalidUserException.class,
                () ->
                        userService.updateTagsForUsers(
                                new AdminUserTagUpdateRequestDTO(
                                        List.of(id(1)), Set.of("x"), Set.of("x"))));
    }

    @Test
    void updateUserProfile_KeepsAdminManagedTags() {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Set.of("mitglied-2026-abcd"));
        final UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO("Jane", null, null, existingUser.getEmail(), null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertEquals(Set.of("mitglied-2026-abcd"), updatedUser.tags());
    }

    @Test
    void updateUserProfile_Success_UpdateEmail() {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO(null, null, null, "new@example.com", null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.email());
        assertFalse(existingUser.isEmailVerified());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    private static final byte[] TOTP_SECRET_BYTES_EMAIL_CHANGE =
            new byte[] {0x12, 0x34, 0x56, 0x78, (byte) 0x90, 0x12, 0x34, 0x56, 0x78, 0x12};

    private static String validTotpCodeForEmailChangeTests(byte[] secretBytes) throws Exception {
        com.eatthepath.otp.TimeBasedOneTimePasswordGenerator totp =
                new com.eatthepath.otp.TimeBasedOneTimePasswordGenerator();
        javax.crypto.SecretKey key =
                new javax.crypto.spec.SecretKeySpec(secretBytes, totp.getAlgorithm());
        return totp.generateOneTimePasswordString(key, Instant.now());
    }

    @Test
    void updateUserProfile_EmailChange_TwoFactorEnabled_ValidCode_SucceedsAndDisablesEmailFactor()
            throws Exception {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.setTwoFactorEnabled(true);
        existingUser.setTotpEnabled(true);
        existingUser.setEmailEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES_EMAIL_CHANGE);
        existingUser.setTotpSecret(secret);
        String validCode = validTotpCodeForEmailChangeTests(TOTP_SECRET_BYTES_EMAIL_CHANGE);

        UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO(null, null, null, "new@example.com", validCode);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.email());
        // The TOTP code proved current possession of 2FA, so the change went through -- but the
        // new address is unverified, so EMAIL 2FA specifically is auto-disabled. TOTP, which the
        // change didn't touch, stays on.
        assertTrue(existingUser.isTotpEnabled());
        assertFalse(existingUser.isEmailEnabled());
        assertTrue(existingUser.isTwoFactorEnabled());
    }

    @Test
    void
            updateUserProfile_EmailChange_TwoFactorEnabled_InvalidCode_ThrowsInvalidTwoFactorCodeException() {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.setTwoFactorEnabled(true);
        existingUser.setTotpEnabled(true);
        existingUser.setTotpSecret("JBSWY3DPEHPK3PXP");

        UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO(null, null, null, "new@example.com", "000000");

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                InvalidTwoFactorCodeException.class,
                () -> userService.updateUserProfile("testuser", dto));

        // Nothing changed: neither the email nor the 2FA state.
        assertEquals("old@example.com", existingUser.getEmail());
        assertTrue(existingUser.isTwoFactorEnabled());
        verify(userRepository, never()).persist(existingUser);
    }

    @Test
    void
            updateUserProfile_EmailChange_TwoFactorEnabled_MissingCode_ThrowsInvalidTwoFactorCodeException() {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.setTwoFactorEnabled(true);
        existingUser.setEmailEnabled(true);

        UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO(null, null, null, "new@example.com", null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                InvalidTwoFactorCodeException.class,
                () -> userService.updateUserProfile("testuser", dto));
    }

    @Test
    void updateUserProfile_EmailUnchanged_TwoFactorEnabled_NoCodeRequired() {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.setTwoFactorEnabled(true);
        existingUser.setTotpEnabled(true);
        existingUser.setTotpSecret("JBSWY3DPEHPK3PXP");

        // Same email as before, only the name changes -- no 2FA code needed since the trust
        // invariant (email address is unchanged) isn't touched.
        UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO("NewFirstName", null, null, "old@example.com", null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));

        UserDTO updatedUser =
                assertDoesNotThrow(() -> userService.updateUserProfile("testuser", dto));

        assertEquals("NewFirstName", updatedUser.firstname());
        assertTrue(existingUser.isTotpEnabled());
        assertTrue(existingUser.isTwoFactorEnabled());
    }

    @Test
    void updateUserProfile_UserNotFoundException() {
        final UserProfileUpdateDTO dto = new UserProfileUpdateDTO("New", null, null, null, null);
        when(userRepository.findByUsernameOptional(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(null); // Mock findByUsername

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUserProfile("nonexistent", dto));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_InvalidUserException_NullDTO() {
        assertThrows(
                InvalidUserException.class, () -> userService.updateUserProfile("testuser", null));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_Success_WithDuplicateEmail()
            throws UserNotFoundException, InvalidUserException, DuplicateUserException {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        final UserProfileUpdateDTO dto =
                new UserProfileUpdateDTO(null, null, null, "duplicate@example.com", null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername
        when(emailVerificationRepository.findByUserIdOptional(any(UUID.class)))
                .thenReturn(Optional.empty());
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        UserDTO updatedUser = userService.updateUserProfile("testuser", dto);

        assertNotNull(updatedUser);
        assertEquals("duplicate@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_InvalidEntryDoesNotBlockTheValidOnes() {
        List<AdminUserCreationDto> dtos =
                List.of(importDto("bad name"), importDto("good.user"), importDto("x"));

        UserImportResultDTO result = userService.importUsers(dtos);

        assertEquals(
                List.of("good.user"), result.created().stream().map(UserDTO::username).toList());
        assertEquals(2, result.failed().size());
        assertTrue(
                result.failed().stream()
                        .allMatch(f -> f.reason() == UserImportFailureReason.INVALID));
        assertTrue(result.failed().get(0).message().contains("username"));
    }

    @Test
    void replaceUser_DeletesAndCreatesAgain() {
        User existing =
                new User(
                        "anna",
                        "old@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "Anna",
                        "Old",
                        Set.of(Roles.USER),
                        Set.of("x"));
        existing.id = id(1);
        AuthenticatedUser admin = new AuthenticatedUser(id(9), Set.of(Roles.ADMIN));
        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existing));
        when(userRepository.findByIds(List.of(id(1)))).thenReturn(List.of(existing));
        when(userRepository.existsByUsername("Anna")).thenReturn(false);

        UserDTO result = userService.replaceUser(id(1), importDto("Anna"), admin);

        assertEquals("Anna", result.username());
        verify(userRepository).deleteByIds(List.of(id(1)));
        verify(userRepository).persist(any(User.class));
    }

    @Test
    void replaceUser_OtherUsername_IsRejectedAndNothingIsDeleted() {
        User existing =
                new User(
                        "anna",
                        "old@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "Anna",
                        "Old",
                        Set.of(Roles.USER),
                        Set.of());
        existing.id = id(1);
        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existing));

        assertThrows(
                InvalidUserException.class,
                () -> userService.replaceUser(id(1), importDto("someone.else"), null));
        verify(userRepository, never()).deleteByIds(any());
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void replaceUser_OwnAccount_IsRejectedAndNothingIsCreated() {
        User existing =
                new User(
                        "admin",
                        "a@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "Ad",
                        "Min",
                        Set.of(Roles.ADMIN),
                        Set.of());
        existing.id = id(9);
        AuthenticatedUser admin = new AuthenticatedUser(id(9), Set.of(Roles.ADMIN));
        when(userRepository.findByIdOptional(id(9))).thenReturn(Optional.of(existing));
        when(userRepository.findByIds(List.of(id(9)))).thenReturn(List.of(existing));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.replaceUser(id(9), importDto("admin"), admin));
        verify(userRepository, never()).deleteByIds(any());
        verify(userRepository, never()).persist(any(User.class));
    }

    @Test
    void importUsers_ImportsWhatWorksAndReportsAllConflicts() {
        List<AdminUserCreationDto> dtos =
                List.of(
                        importDto("new.user"),
                        importDto("taken"),
                        importDto("NEW.user"),
                        importDto("BoxOffice"),
                        importDto("second.user"));
        when(userRepository.findExistingUsernames(any())).thenReturn(List.of("taken"));

        UserImportResultDTO result = userService.importUsers(dtos);

        assertEquals(
                List.of("new.user", "second.user"),
                result.created().stream().map(UserDTO::username).toList());
        assertEquals(
                List.of(
                        UserImportFailureReason.USERNAME_EXISTS,
                        UserImportFailureReason.DUPLICATE_IN_BATCH,
                        UserImportFailureReason.RESERVED_USERNAME),
                result.failed().stream().map(UserImportFailureDTO::reason).toList());
        assertEquals(
                List.of("taken", "NEW.user", "BoxOffice"),
                result.failed().stream().map(UserImportFailureDTO::username).toList());
        verify(userRepository, times(2)).persist(any(User.class));
    }

    private static AdminUserCreationDto importDto(String username) {
        return new AdminUserCreationDto(
                username,
                username + "@example.com",
                false,
                false,
                "pass",
                "First",
                "Last",
                Set.of(Roles.USER),
                Set.of());
    }

    @Test
    void importUsers_Success() throws InvalidUserException, DuplicateUserException {
        List<AdminUserCreationDto> dtos = new ArrayList<>();
        AdminUserCreationDto dto1 =
                new AdminUserCreationDto(
                        "user1",
                        "user1@example.com",
                        false,
                        false,
                        "pass1",
                        "First1",
                        "Last1",
                        Set.of(Roles.USER),
                        Set.of());
        AdminUserCreationDto dto2 =
                new AdminUserCreationDto(
                        "user2",
                        "user2@example.com",
                        false,
                        false,
                        "pass2",
                        "First2",
                        "Last2",
                        Set.of(Roles.MANAGER),
                        Set.of());
        dtos.add(dto1);
        dtos.add(dto2);

        when(userRepository.findByUsernameOptional("user1")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameOptional("user2")).thenReturn(Optional.empty());
        when(userRepository.isPersistent(any(User.class))).thenReturn(true);
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        UserImportResultDTO result = userService.importUsers(dtos);

        assertNotNull(result);
        assertEquals(2, result.created().size());
        assertTrue(result.failed().isEmpty());
        verify(userRepository, times(2)).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_EmptySet() throws InvalidUserException, DuplicateUserException {
        List<AdminUserCreationDto> dtos = Collections.emptyList();

        UserImportResultDTO result = userService.importUsers(dtos);

        assertNotNull(result);
        assertTrue(result.created().isEmpty());
        assertTrue(result.failed().isEmpty());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_InvalidUsername_IsReportedAsFailure() {
        List<AdminUserCreationDto> dtos = new ArrayList<>();
        AdminUserCreationDto invalidDto =
                new AdminUserCreationDto(
                        "",
                        "invalid@example.com",
                        false,
                        false,
                        "pass",
                        "Invalid",
                        "User",
                        Set.of(Roles.USER),
                        Set.of()); // Invalid username
        dtos.add(invalidDto);

        UserImportResultDTO result = userService.importUsers(dtos);

        assertTrue(result.created().isEmpty());
        assertEquals(1, result.failed().size());
        assertEquals(UserImportFailureReason.INVALID, result.failed().get(0).reason());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_ExistingUsername_IsReportedCaseInsensitively() {
        List<AdminUserCreationDto> dtos = new ArrayList<>();
        AdminUserCreationDto duplicateDto =
                new AdminUserCreationDto(
                        "existinguser",
                        "existing@example.com",
                        false,
                        false,
                        "pass",
                        "Existing",
                        "User",
                        Set.of(Roles.USER),
                        Set.of());
        dtos.add(duplicateDto);

        when(userRepository.findExistingUsernames(Set.of("existinguser")))
                .thenReturn(List.of("ExistingUser")); // Simulate existing user (other casing)
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        UserImportResultDTO result = userService.importUsers(dtos);

        assertTrue(result.created().isEmpty());
        assertEquals(1, result.failed().size());
        assertEquals(UserImportFailureReason.USERNAME_EXISTS, result.failed().get(0).reason());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_EmailSendFailure() {
        List<AdminUserCreationDto> dtos = new ArrayList<>();
        AdminUserCreationDto dto1 =
                new AdminUserCreationDto(
                        "user1",
                        "user1@example.com",
                        false,
                        false,
                        "pass1",
                        "First1",
                        "Last1",
                        Set.of(Roles.USER),
                        Set.of());
        dtos.add(dto1);

        when(userRepository.findByUsernameOptional("user1")).thenReturn(Optional.empty());
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(
                        new EmailVerification(
                                new User(
                                        "mock",
                                        "mock@example.com",
                                        true,
                                        false,
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                Instant.now()));

        assertDoesNotThrow(() -> userService.importUsers(dtos));
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    // Tests for new verification code system
    @Test
    void verifyEmailWithCode_Success() throws VerifyTokenExpiredException {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
                        false,
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        user.id = id(1);
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "123456",
                        Instant.now().plusSeconds(Duration.ofMinutes(10).toSeconds()));
        emailVerification.id = id(100);

        when(emailVerificationRepository.findByToken("123456")).thenReturn(emailVerification);

        String result = userService.verifyEmailWithCode("123456");

        assertEquals("test@example.com", result);
        assertTrue(user.isEmailVerified());
        verify(emailVerificationRepository, times(1)).findByToken("123456");
        verify(emailVerificationRepository, times(1)).deleteById(id(100));
        verify(userRepository, times(1)).persist(user);
    }

    @Test
    void verifyEmailWithCode_BadRequestException_NullCode() {
        assertThrows(ValidationException.class, () -> userService.verifyEmailWithCode(null));
    }

    @Test
    void verifyEmailWithCode_BadRequestException_EmptyCode() {
        assertThrows(ValidationException.class, () -> userService.verifyEmailWithCode(""));
    }

    @Test
    void verifyEmailWithCode_BadRequestException_InvalidFormat() {
        assertThrows(
                ValidationException.class,
                () -> userService.verifyEmailWithCode("12345")); // 5 digits instead of 6

        assertThrows(
                ValidationException.class,
                () -> userService.verifyEmailWithCode("abcdef")); // letters instead of digits

        assertThrows(
                ValidationException.class,
                () -> userService.verifyEmailWithCode("1234567")); // 7 digits instead of 6
    }

    @Test
    void verifyEmailWithCode_BadRequestException_CodeNotFound() {
        when(emailVerificationRepository.findByToken("123456")).thenReturn(null);

        assertThrows(
                VerificationCodeNotFoundException.class,
                () -> userService.verifyEmailWithCode("123456"));
    }

    @Test
    void verifyEmailWithCode_TokenExpiredException() {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
                        false,
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        user.id = id(1);
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "123456",
                        Instant.now().minusSeconds(Duration.ofMinutes(10).toSeconds())); // expired
        emailVerification.id = id(100);

        when(emailVerificationRepository.findByToken("123456")).thenReturn(emailVerification);

        assertThrows(
                VerifyTokenExpiredException.class, () -> userService.verifyEmailWithCode("123456"));

        // Ensure user is not marked as verified
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void verifyEmailWithCode_FailsWithUsedCode() {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
                        false,
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        user.id = id(1);
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "123456",
                        Instant.now().plusSeconds(Duration.ofMinutes(10).toSeconds()));
        emailVerification.id = id(100);

        when(emailVerificationRepository.findByToken("123456"))
                .thenReturn(emailVerification)
                .thenReturn(null); // Second call returns null (code is deleted)

        // First verification is successful
        assertDoesNotThrow(() -> userService.verifyEmailWithCode("123456"));
        verify(emailVerificationRepository).deleteById(id(100));

        // Second verification should fail (code already used/deleted)
        assertThrows(
                VerificationCodeNotFoundException.class,
                () -> userService.verifyEmailWithCode("123456"));
    }

    @Test
    void updateUser_SecurityException_AdminRemovesOwnAdminRole() {
        AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        "First",
                        "Last",
                        null,
                        "test@example.com",
                        false,
                        true,
                        Set.of(Roles.USER),
                        Set.of());

        User existingUser = new User();
        existingUser.id = id(1);
        existingUser.setUsername("admin");
        existingUser.setRoles(Set.of(Roles.ADMIN, Roles.USER));

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(1), Set.of(Roles.ADMIN));

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(id(1), dto, caller));
    }

    @Test
    void updateUser_SecurityException_SelfLockout_DoesNotSendPasswordEmail() {
        User existingUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "First",
                        "Last",
                        Set.of(Roles.ADMIN, Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        "First",
                        "Last",
                        "newpassword",
                        "admin@example.com",
                        false,
                        true,
                        Set.of(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(1), Set.of(Roles.ADMIN));

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(id(1), dto, caller));

        verify(emailService, never()).sendPasswordChangedNotification(any(User.class));
        verify(userRepository, never()).persist(any(User.class));
        assertEquals(Set.of(Roles.ADMIN, Roles.USER), existingUser.getRoles());
    }

    @Test
    void updateUser_SecurityException_BoxOfficeAccountCannotBeGrantedRoles() {
        User existingUser =
                new User(
                        "boxoffice",
                        null,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet());
        existingUser.id = id(1);
        AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null, null, null, null, false, false, Set.of(Roles.ADMIN), Set.of());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        AuthenticatedUser caller = new AuthenticatedUser(id(2), Set.of(Roles.ADMIN));

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(id(1), dto, caller));

        verify(userRepository, never()).persist(any(User.class));
        assertTrue(existingUser.getRoles().isEmpty());
    }

    @Test
    void updateUser_SecurityException_NullCurrentUser_WhenRolesProvided() {
        User existingUser =
                new User(
                        "testuser",
                        "old@example.com",
                        true,
                        false,
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = id(1);
        AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(id(1))).thenReturn(Optional.of(existingUser));

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(id(1), dto, null));

        verify(userRepository, never()).persist(any(User.class));
    }
}
