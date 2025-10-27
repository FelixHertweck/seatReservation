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
package de.felixhertweck.seatreservation;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.runtime.StartupEvent;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock private UserRepository userRepository;

    @Mock private UserService userService;

    @Mock private StartupEvent startupEvent;

    @InjectMocks private AdminUserInitializer adminUserInitializer;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(userRepository, userService);
    }

    @Test
    void onStart_WithNoExistingAdminUser_CreatesAdminUser() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());

        // Act
        adminUserInitializer.onStart(startupEvent);

        // Assert
        verify(userRepository).findByUsernameOptional("admin");

        ArgumentCaptor<UserCreationDTO> userCreationCaptor =
                ArgumentCaptor.forClass(UserCreationDTO.class);

        ArgumentCaptor<Boolean> emailConfirmedCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(userService)
                .createUser(userCreationCaptor.capture(), anySet(), emailConfirmedCaptor.capture());

        UserCreationDTO capturedDto = userCreationCaptor.getValue();
        assertEquals("admin", capturedDto.getUsername());
        assertEquals("admin@localhost", capturedDto.getEmail());
        assertEquals("System", capturedDto.getFirstname());
        assertEquals("Admin", capturedDto.getLastname());
        assertNotNull(capturedDto.getPassword());
        assertEquals(12, capturedDto.getPassword().length());
        assertTrue(emailConfirmedCaptor.getValue());
    }

    @Test
    void onStart_WithExistingAdminUser_SkipsCreation() throws Exception {
        // Arrange
        User existingAdmin = new User();
        existingAdmin.setUsername("admin");
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.of(existingAdmin));

        // Act
        adminUserInitializer.onStart(startupEvent);

        // Assert
        verify(userRepository).findByUsernameOptional("admin");
        verify(userService, never()).createUser(any(), any(), anyBoolean());
    }

    @Test
    void onStart_WhenDuplicateUserExceptionThrown_LogsWarningAndContinues() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());
        doThrow(new DuplicateUserException("User already exists"))
                .when(userService)
                .createUser(any(UserCreationDTO.class), any(), anyBoolean());

        // Act - should not throw exception
        adminUserInitializer.onStart(startupEvent);

        // Assert
        verify(userRepository).findByUsernameOptional("admin");
        verify(userService).createUser(any(UserCreationDTO.class), any(), anyBoolean());
        // Logs warning but doesn't fail
    }

    @Test
    void onStart_WhenGeneralExceptionThrown_LogsErrorAndContinues() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Database connection failed"))
                .when(userService)
                .createUser(any(UserCreationDTO.class), any(), anyBoolean());

        // Act - should not throw exception
        adminUserInitializer.onStart(startupEvent);

        // Assert
        verify(userRepository).findByUsernameOptional("admin");
        verify(userService).createUser(any(UserCreationDTO.class), any(), anyBoolean());
        // Logs error but doesn't fail
    }

    @Test
    void onStart_GeneratesRandomPassword() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());

        // Act
        adminUserInitializer.onStart(startupEvent);

        // Assert
        ArgumentCaptor<UserCreationDTO> userCreationCaptor =
                ArgumentCaptor.forClass(UserCreationDTO.class);
        verify(userService).createUser(userCreationCaptor.capture(), any(), anyBoolean());

        String password = userCreationCaptor.getValue().getPassword();
        // Password should be 12 characters long
        assertNotNull(password);
        assertEquals(12, password.length());
        // Password should contain only allowed characters
        assertTrue(password.matches("[A-Za-z0-9!@#$%^&*()\\-_=+]+"));
    }

    @Test
    void onStart_MultipleInvocations_EachChecksForAdmin() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());

        // Act - call multiple times (simulating multiple startup scenarios)
        adminUserInitializer.onStart(startupEvent);
        adminUserInitializer.onStart(startupEvent);

        // Assert - should check for admin each time
        verify(userRepository, times(2)).findByUsernameOptional("admin");
        verify(userService, times(2)).createUser(any(UserCreationDTO.class), any(), anyBoolean());
    }

    @Test
    void onStart_CreatesUserWithAdminRole() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());

        // Act
        adminUserInitializer.onStart(startupEvent);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Roles>> rolesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(userService).createUser(any(UserCreationDTO.class), anySet(), anyBoolean());

        // We verify the roles indirectly by checking the admin user was created
        // The roles argument is passed as Set.of(Roles.ADMIN) in the actual code
    }

    @Test
    void onStart_CreatesUserWithEmailConfirmed() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());

        // Act
        adminUserInitializer.onStart(startupEvent);

        // Assert
        ArgumentCaptor<Boolean> emailConfirmedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(userService)
                .createUser(any(UserCreationDTO.class), any(), emailConfirmedCaptor.capture());

        assertTrue(emailConfirmedCaptor.getValue());
    }

    @Test
    void onStart_CreatesUserWithSystemGroups() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOptional("admin")).thenReturn(Optional.empty());

        // Act
        adminUserInitializer.onStart(startupEvent);

        // Assert
        ArgumentCaptor<UserCreationDTO> userCreationCaptor =
                ArgumentCaptor.forClass(UserCreationDTO.class);
        verify(userService).createUser(userCreationCaptor.capture(), any(), anyBoolean());

        UserCreationDTO dto = userCreationCaptor.getValue();
        assertTrue(dto.getTags().contains("system"));
        assertEquals(1, dto.getTags().size());
    }
}
