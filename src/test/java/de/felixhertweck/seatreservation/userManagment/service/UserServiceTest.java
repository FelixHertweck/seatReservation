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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import de.felixhertweck.seatreservation.userManagment.exceptions.VerificationCodeNotFoundException;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerifyTokenExpiredException;
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

    @Inject UserService userService;

    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository, emailService, emailVerificationRepository);
    }

    @Test
    void createUser_Success_WithEmail() throws IOException {
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
    void createUser_Success_WithoutEmail() throws IOException {
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
    void createUser_Success_WithEmail_NoVerificationSent() throws IOException {
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
    void createUser_InvalidUserException_NullDTO() throws IOException {
        assertThrows(
                InvalidUserException.class,
                () -> userService.createUser(null, Set.of(Roles.USER), false));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_InvalidUserException_EmptyUsername() throws IOException {
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
    void createUser_DuplicateUserException_ExistingUsername() throws IOException {
        final UserCreationDTO dto =
                new UserCreationDTO(
                        "existinguser", "test@example.com", "password", "John", "Doe", null);
        when(userRepository.findByUsernameOptional(anyString()))
                .thenReturn(Optional.of(new User()));

        assertThrows(
                DuplicateUserException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), false));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_Success_WithDuplicateEmail() throws IOException {
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
    void createUser_InternalServerErrorException_EmailSendFailure() throws IOException {
        UserCreationDTO dto =
                new UserCreationDTO(
                        "testuser", "test@example.com", "password", "John", "Doe", null);
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
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

        assertThrows(
                RuntimeException.class,
                () -> userService.createUser(dto, Set.of(Roles.USER), true));
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_RuntimeException_EmailVerificationTrue_NoEmail() throws IOException {
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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUser(1L, dto);

        verify(emailService, times(1)).sendPasswordChangedNotification(existingUser);
    }

    @Test
    void updateUser_Success_PasswordSaltChangesOnPasswordUpdate()
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUser(1L, dto);

        assertNotEquals(initialSalt, existingUser.getPasswordSalt());
        assertTrue(
                BcryptUtil.matches(
                        "newpassword" + existingUser.getPasswordSalt(),
                        existingUser.getPasswordHash()));
    }

    @Test
    void updateUser_Success_UpdateRoles()
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

        assertNotNull(updatedUser);
        assertEquals(newRoles, existingUser.getRoles());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_NoEmailChange()
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(anyLong()))
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

        UserDTO updatedUser = userService.updateUser(1L, dto);

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.email());
        assertFalse(existingUser.isEmailVerified());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_UserNotFoundException() throws IOException {
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
        when(userRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(1L, dto));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_InvalidUserException_NullDTO() throws IOException {
        assertThrows(InvalidUserException.class, () -> userService.updateUser(1L, null));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_WithDuplicateEmail()
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(anyLong()))
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

        UserDTO updatedUser = userService.updateUser(1L, dto);

        assertNotNull(updatedUser);
        assertEquals("duplicate@example.com", updatedUser.email());
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_InternalServerErrorException_EmailSendFailure() throws IOException {
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
        existingUser.id = 1L;
        AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        null,
                        "new@example.com",
                        true,
                        false,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));
        when(emailVerificationRepository.findByUserIdOptional(anyLong()))
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
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

        assertThrows(RuntimeException.class, () -> userService.updateUser(1L, dto));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_UpdateEmail_NoVerificationSent()
            throws IOException, UserNotFoundException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.email());
        assertFalse(existingUser.isEmailVerified()); // Sollte immer noch false sein
        verify(userRepository, times(1)).persist(existingUser);
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_Success_NoEmailChange_VerificationSentTrue()
            throws IOException, UserNotFoundException {
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
        existingUser.id = 1L;
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

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        UserDTO updatedUser = userService.updateUser(1L, dto);

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
        existingUser.id = 1L;
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.deleteById(1L)).thenReturn(true); // Mock deleteById to return true

        userService.deleteUser(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_UserNotFoundException() throws UserNotFoundException {
        when(userRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());
        when(userRepository.deleteById(anyLong()))
                .thenReturn(false); // Mock deleteById to return false

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1L));
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
        existingUser.id = 1L;
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findById(1L)).thenReturn(existingUser); // Mock findById

        UserDTO foundUser = userService.getUserById(1L);

        assertNotNull(foundUser);
        assertEquals(existingUser.id, foundUser.id());
    }

    @Test
    void getUserById_UserNotFoundException() throws UserNotFoundException {
        when(userRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());
        when(userRepository.findById(anyLong())).thenReturn(null); // Mock findById to return null

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
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
        when(userRepository.listAll()).thenReturn(Arrays.asList(user1, user2));

        List<LimitedUserInfoDTO> users = userService.getAllUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.username().equals(user1.getUsername())));
        assertTrue(users.stream().anyMatch(u -> u.username().equals(user2.getUsername())));
    }

    @Test
    void getAllUsers_Success_NoUsers() {
        when(userRepository.listAll()).thenReturn(Collections.emptyList());

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
                new UserProfileUpdateDTO(
                        "New",
                        "User",
                        null,
                        existingUser.getEmail(),
                        Collections.singleton(Roles.USER));

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
                new UserProfileUpdateDTO(
                        "John",
                        "New",
                        null,
                        existingUser.getEmail(),
                        Collections.singleton(Roles.USER));

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
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
    void updateUserProfile_Success_UpdateEmail() throws IOException {
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
        when(emailVerificationRepository.findByUserIdOptional(anyLong()))
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

    @Test
    void updateUserProfile_UserNotFoundException() throws IOException {
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
    void updateUserProfile_InvalidUserException_NullDTO() throws IOException {
        assertThrows(
                InvalidUserException.class, () -> userService.updateUserProfile("testuser", null));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_Success_WithDuplicateEmail()
            throws IOException,
                    UserNotFoundException,
                    InvalidUserException,
                    DuplicateUserException {
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
        when(emailVerificationRepository.findByUserIdOptional(anyLong()))
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
    void updateUserProfile_InternalServerErrorException_EmailSendFailure() throws IOException {
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
                new UserProfileUpdateDTO(null, null, null, "new@example.com", null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername
        when(emailVerificationRepository.findByUserIdOptional(anyLong()))
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
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

        assertThrows(RuntimeException.class, () -> userService.updateUserProfile("testuser", dto));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_InternalServerErrorException_PasswordChangeEmailSendFailure()
            throws IOException {
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
                new UserProfileUpdateDTO(
                        "John", "Doe", "newpassword", "old@example.com", Collections.emptySet());

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        doThrow(new IOException("Password changed email send failed"))
                .when(emailService)
                .sendPasswordChangedNotification(any(User.class));

        assertThrows(RuntimeException.class, () -> userService.updateUserProfile("testuser", dto));
        verify(emailService, times(1)).sendPasswordChangedNotification(existingUser);
    }

    @Test
    void importUsers_Success() throws InvalidUserException, DuplicateUserException, IOException {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        AdminUserCreationDto dto1 =
                new AdminUserCreationDto(
                        "user1",
                        "user1@example.com",
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

        Set<UserDTO> importedUsers = userService.importUsers(dtos);

        assertNotNull(importedUsers);
        assertEquals(2, importedUsers.size());
        verify(userRepository, times(2)).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_EmptySet() throws InvalidUserException, DuplicateUserException, IOException {
        Set<AdminUserCreationDto> dtos = Collections.emptySet();

        Set<UserDTO> importedUsers = userService.importUsers(dtos);

        assertNotNull(importedUsers);
        assertTrue(importedUsers.isEmpty());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_InvalidUserException() throws IOException {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        AdminUserCreationDto invalidDto =
                new AdminUserCreationDto(
                        "",
                        "invalid@example.com",
                        false,
                        "pass",
                        "Invalid",
                        "User",
                        Set.of(Roles.USER),
                        Set.of()); // Invalid username
        dtos.add(invalidDto);

        assertThrows(InvalidUserException.class, () -> userService.importUsers(dtos));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_DuplicateUserException() throws IOException {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        AdminUserCreationDto duplicateDto =
                new AdminUserCreationDto(
                        "existinguser",
                        "existing@example.com",
                        false,
                        "pass",
                        "Existing",
                        "User",
                        Set.of(Roles.USER),
                        Set.of());
        dtos.add(duplicateDto);

        when(userRepository.findByUsernameOptional("existinguser"))
                .thenReturn(Optional.of(new User())); // Simulate existing user
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

        assertThrows(DuplicateUserException.class, () -> userService.importUsers(dtos));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void importUsers_EmailSendFailure() throws IOException {
        Set<AdminUserCreationDto> dtos = new HashSet<>();
        AdminUserCreationDto dto1 =
                new AdminUserCreationDto(
                        "user1",
                        "user1@example.com",
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
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

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
        user.id = 1L;
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "123456",
                        Instant.now().plusSeconds(Duration.ofMinutes(10).toSeconds()));
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByToken("123456")).thenReturn(emailVerification);

        String result = userService.verifyEmailWithCode("123456");

        assertEquals("test@example.com", result);
        assertTrue(user.isEmailVerified());
        verify(emailVerificationRepository, times(1)).findByToken("123456");
        verify(emailVerificationRepository, times(1)).deleteById(100L);
        verify(userRepository, times(1)).persist(user);
    }

    @Test
    void verifyEmailWithCode_BadRequestException_NullCode() {
        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmailWithCode(null));
    }

    @Test
    void verifyEmailWithCode_BadRequestException_EmptyCode() {
        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmailWithCode(""));
    }

    @Test
    void verifyEmailWithCode_BadRequestException_InvalidFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.verifyEmailWithCode("12345")); // 5 digits instead of 6

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.verifyEmailWithCode("abcdef")); // letters instead of digits

        assertThrows(
                IllegalArgumentException.class,
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
        user.id = 1L;
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "123456",
                        Instant.now().minusSeconds(Duration.ofMinutes(10).toSeconds())); // expired
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByToken("123456")).thenReturn(emailVerification);

        assertThrows(
                VerifyTokenExpiredException.class, () -> userService.verifyEmailWithCode("123456"));

        // Ensure user is not marked as verified
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).deleteById(any(Long.class));
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
        user.id = 1L;
        EmailVerification emailVerification =
                new EmailVerification(
                        user,
                        "123456",
                        Instant.now().plusSeconds(Duration.ofMinutes(10).toSeconds()));
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByToken("123456"))
                .thenReturn(emailVerification)
                .thenReturn(null); // Second call returns null (code is deleted)

        // First verification is successful
        assertDoesNotThrow(() -> userService.verifyEmailWithCode("123456"));
        verify(emailVerificationRepository).deleteById(100L);

        // Second verification should fail (code already used/deleted)
        assertThrows(
                VerificationCodeNotFoundException.class,
                () -> userService.verifyEmailWithCode("123456"));
    }
}
