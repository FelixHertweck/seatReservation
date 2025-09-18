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

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.TokenExpiredException;
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
                                "hash",
                                "salt",
                                "Mock",
                                "User",
                                Set.of(),
                                Set.of()),
                        "token",
                        LocalDateTime.now());
        when(emailService.createEmailVerification(any(User.class)))
                .thenReturn(mockEmailVerification);

        User createdUser =
                userService.createUser(
                        dto.getUsername(),
                        dto.getEmail(),
                        dto.getPassword(),
                        dto.getFirstname(),
                        dto.getLastname(),
                        Set.of(Roles.USER),
                        dto.getTags());

        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.getUsername());
        assertEquals("test@example.com", createdUser.getEmail());
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

        User createdUser =
                userService.createUser(
                        dto.getUsername(),
                        dto.getEmail(),
                        dto.getPassword(),
                        dto.getFirstname(),
                        dto.getLastname(),
                        Set.of(Roles.USER),
                        dto.getTags());

        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.getUsername());
        assertNull(createdUser.getEmail());
        verify(userRepository, times(1)).persist(any(User.class));
        verify(emailService, never()).createEmailVerification(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void createUser_InvalidUserException_NullDTO() throws IOException {
        assertThrows(
                InvalidUserException.class,
                () ->
                        userService.createUser(
                                null, null, null, null, null, Set.of(Roles.USER), null));
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
                () ->
                        userService.createUser(
                                dto.getUsername(),
                                dto.getEmail(),
                                dto.getPassword(),
                                dto.getFirstname(),
                                dto.getLastname(),
                                Set.of(Roles.USER),
                                dto.getTags()));

        final UserCreationDTO dto2 =
                new UserCreationDTO("   ", "test@example.com", "password", "John", "Doe", null);
        assertThrows(
                InvalidUserException.class,
                () ->
                        userService.createUser(
                                dto2.getUsername(),
                                dto2.getEmail(),
                                dto2.getPassword(),
                                dto2.getFirstname(),
                                dto2.getLastname(),
                                Set.of(Roles.USER),
                                dto2.getTags()));

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
                () ->
                        userService.createUser(
                                dto.getUsername(),
                                dto.getEmail(),
                                dto.getPassword(),
                                dto.getFirstname(),
                                dto.getLastname(),
                                Set.of(Roles.USER),
                                dto.getTags()));

        final UserCreationDTO dto2 =
                new UserCreationDTO("testuser", "test@example.com", "   ", "John", "Doe", null);
        assertThrows(
                InvalidUserException.class,
                () ->
                        userService.createUser(
                                dto2.getUsername(),
                                dto2.getEmail(),
                                dto2.getPassword(),
                                dto2.getFirstname(),
                                dto2.getLastname(),
                                Set.of(Roles.USER),
                                dto2.getTags()));

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
                () ->
                        userService.createUser(
                                dto.getUsername(),
                                dto.getEmail(),
                                dto.getPassword(),
                                dto.getFirstname(),
                                dto.getLastname(),
                                Set.of(Roles.USER),
                                dto.getTags()));
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Set.of(),
                                        Set.of()),
                                "token",
                                LocalDateTime.now()));

        // Simulate that another user already has this email, but it should not prevent creation
        // (assuming email uniqueness is not enforced at this layer for creation)

        User createdUser =
                userService.createUser(
                        dto.getUsername(),
                        dto.getEmail(),
                        dto.getPassword(),
                        dto.getFirstname(),
                        dto.getLastname(),
                        Set.of(Roles.USER),
                        dto.getTags());

        assertNotNull(createdUser);
        assertEquals("newuser", createdUser.getUsername());
        assertEquals("existing@example.com", createdUser.getEmail());
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Set.of(),
                                        Set.of()),
                                "token",
                                LocalDateTime.now()));
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

        assertThrows(
                RuntimeException.class,
                () ->
                        userService.createUser(
                                dto.getUsername(),
                                dto.getEmail(),
                                dto.getPassword(),
                                dto.getFirstname(),
                                dto.getLastname(),
                                Set.of(Roles.USER),
                                dto.getTags()));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
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
                        null,
                        null,
                        null,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("New", updatedUser.getFirstname());
        assertEquals("User", updatedUser.getLastname());
        assertEquals("old@example.com", updatedUser.getEmail());
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
                        "oldhash",
                        "salt",
                        "John",
                        "Old",
                        Collections.singleton(Roles.USER),
                        Set.of());
        existingUser.id = 1L;
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        "New",
                        null,
                        null,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("John", updatedUser.getFirstname());
        assertEquals("New", updatedUser.getLastname());
        assertEquals("old@example.com", updatedUser.getEmail());
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
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Set.of());
        existingUser.id = 1L;
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        "newpassword",
                        null,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

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
                        "newpassword",
                        null,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUser(
                1L,
                dto.getFirstname(),
                dto.getLastname(),
                dto.getPassword(),
                dto.getEmail(),
                dto.getRoles(),
                dto.getTags());

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
                        BcryptUtil.bcryptHash("oldpassword" + initialSalt),
                        initialSalt,
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = 1L;
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(
                        null,
                        null,
                        "newpassword",
                        null,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUser(
                1L,
                dto.getFirstname(),
                dto.getLastname(),
                dto.getPassword(),
                dto.getEmail(),
                dto.getRoles(),
                dto.getTags());

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
                        "oldhash",
                        "salt",
                        "John",
                        "Doe",
                        new HashSet<>(Collections.singletonList(Roles.USER)),
                        Collections.emptySet());
        existingUser.id = 1L;
        Set<String> newRoles = new HashSet<>(Arrays.asList(Roles.USER, Roles.ADMIN));
        final AdminUserUpdateDTO dto =
                new AdminUserUpdateDTO(null, null, null, null, newRoles, Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

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
                        null,
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("New", updatedUser.getFirstname());
        assertEquals("Name", updatedUser.getLastname());
        assertTrue(
                BcryptUtil.matches(
                        "newpass" + existingUser.getPasswordSalt(),
                        existingUser.getPasswordHash()));
        assertEquals("old@example.com", updatedUser.getEmail());
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                LocalDateTime.now()));

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.getEmail());
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
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        when(userRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () ->
                        userService.updateUser(
                                1L,
                                dto.getFirstname(),
                                dto.getLastname(),
                                dto.getPassword(),
                                dto.getEmail(),
                                dto.getRoles(),
                                dto.getTags()));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUser_UserNotFoundException_NullDTO() throws IOException {
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(1L, null, null, null, null, null, null));
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                LocalDateTime.now()));

        // Simulate another user already has this email, but it should not prevent update
        // (assuming email uniqueness is not enforced at this layer for update)

        User updatedUser =
                userService.updateUser(
                        1L,
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getRoles(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("duplicate@example.com", updatedUser.getEmail());
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                LocalDateTime.now()));
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

        assertThrows(
                RuntimeException.class,
                () ->
                        userService.updateUser(
                                1L,
                                dto.getFirstname(),
                                dto.getLastname(),
                                dto.getPassword(),
                                dto.getEmail(),
                                dto.getRoles(),
                                dto.getTags()));
        verify(emailService, times(1)).createEmailVerification(any(User.class));
        verify(emailService, times(1))
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void deleteUser_Success() throws UserNotFoundException {
        User existingUser =
                new User(
                        "testuser",
                        "test@example.com",
                        true,
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
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        existingUser.id = 1L;
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findById(1L)).thenReturn(existingUser); // Mock findById

        User foundUser = userService.getUserById(1L);

        assertNotNull(foundUser);
        assertEquals(existingUser.id, foundUser.getId());
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
                        "h2",
                        "salt2",
                        "F2",
                        "L2",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        when(userRepository.listAll()).thenReturn(Arrays.asList(user1, user2));

        List<User> users = userService.getAllUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals(user1.getUsername())));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals(user2.getUsername())));
    }

    @Test
    void getAllUsers_Success_NoUsers() {
        when(userRepository.listAll()).thenReturn(Collections.emptyList());

        List<User> users = userService.getAllUsers();

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
                        "oldhash",
                        "salt",
                        "Old",
                        "User",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        final UserProfileUpdateDTO dto = new UserProfileUpdateDTO("New", null, null, null, null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername

        User updatedUser =
                userService.updateUserProfile(
                        "testuser",
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("New", updatedUser.getFirstname());
        assertEquals("User", updatedUser.getLastname());
        assertEquals("old@example.com", updatedUser.getEmail());
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
                        "oldhash",
                        "salt",
                        "John",
                        "Old",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        final UserProfileUpdateDTO dto = new UserProfileUpdateDTO(null, "New", null, null, null);

        when(userRepository.findByUsernameOptional("testuser"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(existingUser); // Mock findByUsername

        User updatedUser =
                userService.updateUserProfile(
                        "testuser",
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("John", updatedUser.getFirstname());
        assertEquals("New", updatedUser.getLastname());
        assertEquals("old@example.com", updatedUser.getEmail());
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

        User updatedUser =
                userService.updateUserProfile(
                        "testuser",
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getTags());

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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                LocalDateTime.now()));

        User updatedUser =
                userService.updateUserProfile(
                        "testuser",
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.getEmail());
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
                () ->
                        userService.updateUserProfile(
                                "nonexistent",
                                dto.getFirstname(),
                                dto.getLastname(),
                                dto.getPassword(),
                                dto.getEmail(),
                                dto.getTags()));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailService, never())
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));
    }

    @Test
    void updateUserProfile_UserNotFoundException_NullDTO() throws IOException {
        when(userRepository.findByUsernameOptional("testuser")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUserProfile("testuser", null, null, null, null, null));
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                LocalDateTime.now()));

        User updatedUser =
                userService.updateUserProfile(
                        "testuser",
                        dto.getFirstname(),
                        dto.getLastname(),
                        dto.getPassword(),
                        dto.getEmail(),
                        dto.getTags());

        assertNotNull(updatedUser);
        assertEquals("duplicate@example.com", updatedUser.getEmail());
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
                                        "hash",
                                        "salt",
                                        "Mock",
                                        "User",
                                        Collections.emptySet(),
                                        Collections.emptySet()),
                                "token",
                                LocalDateTime.now()));
        doThrow(new IOException("Email send failed"))
                .when(emailService)
                .sendEmailConfirmation(any(User.class), any(EmailVerification.class));

        assertThrows(
                RuntimeException.class,
                () ->
                        userService.updateUserProfile(
                                "testuser",
                                dto.getFirstname(),
                                dto.getLastname(),
                                dto.getPassword(),
                                dto.getEmail(),
                                dto.getTags()));
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
        doThrow(new IOException("Password changed email send failed"))
                .when(emailService)
                .sendPasswordChangedNotification(any(User.class));

        assertThrows(
                RuntimeException.class,
                () ->
                        userService.updateUserProfile(
                                "testuser",
                                dto.getFirstname(),
                                dto.getLastname(),
                                dto.getPassword(),
                                dto.getEmail(),
                                dto.getTags()));
        verify(emailService, times(1)).sendPasswordChangedNotification(existingUser);
    }

    @Test
    void verifyEmail_Success() throws TokenExpiredException {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        user.id = 1L;
        EmailVerification emailVerification =
                new EmailVerification(user, "validtoken", LocalDateTime.now().plusMinutes(10));
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByIdOptional(100L))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationRepository.findById(100L))
                .thenReturn(emailVerification); // Mock findById
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(user); // Mock findById

        userService.verifyEmail(100L, "validtoken");

        assertTrue(user.isEmailVerified());
        verify(userRepository, times(1)).persist(user);
        verify(emailVerificationRepository, times(1)).deleteById(100L);
    }

    @Test
    void verifyEmail_BadRequestException_NullId() {
        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmail(null, "token"));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_BadRequestException_NullToken() {
        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmail(1L, null));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_BadRequestException_EmptyToken() {
        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmail(1L, ""));
        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmail(1L, "   "));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_NotFoundException_TokenNotFound() {
        when(emailVerificationRepository.findByIdOptional(anyLong())).thenReturn(Optional.empty());
        when(emailVerificationRepository.findById(anyLong())).thenReturn(null); // Mock findById

        assertThrows(IllegalArgumentException.class, () -> userService.verifyEmail(1L, "token"));
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_BadRequestException_InvalidToken() {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        user.id = 1L;
        EmailVerification emailVerification =
                new EmailVerification(user, "correcttoken", LocalDateTime.now().plusMinutes(10));
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByIdOptional(100L))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationRepository.findById(100L))
                .thenReturn(emailVerification); // Mock findById
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(user); // Mock findById

        assertThrows(
                IllegalArgumentException.class, () -> userService.verifyEmail(100L, "wrongtoken"));
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_TokenExpiredException() throws TokenExpiredException {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
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
                        user, "validtoken", LocalDateTime.now().minusMinutes(10)); // Expired token
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByIdOptional(100L))
                .thenReturn(Optional.of(emailVerification));
        when(emailVerificationRepository.findById(100L))
                .thenReturn(emailVerification); // Mock findById
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(user); // Mock findById

        assertThrows(
                TokenExpiredException.class, () -> userService.verifyEmail(100L, "validtoken"));
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).persist(any(User.class));
        verify(emailVerificationRepository, never()).delete(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_FailsWithUsedToken() {
        User user =
                new User(
                        "testuser",
                        "test@example.com",
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Collections.singleton(Roles.USER),
                        Collections.emptySet());
        user.id = 1L;
        EmailVerification emailVerification =
                new EmailVerification(user, "validtoken", LocalDateTime.now().plusMinutes(10));
        emailVerification.id = 100L;

        when(emailVerificationRepository.findByIdOptional(100L))
                .thenReturn(Optional.of(emailVerification));
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(user));

        // First verification is successful
        assertDoesNotThrow(() -> userService.verifyEmail(100L, "validtoken"));
        verify(emailVerificationRepository).deleteById(100L);

        // Now, mock the repository to reflect the deletion
        when(emailVerificationRepository.findByIdOptional(100L)).thenReturn(Optional.empty());

        // Second attempt should throw an exception because the token is no longer found
        assertThrows(
                IllegalArgumentException.class, () -> userService.verifyEmail(100L, "validtoken"));
    }
}
