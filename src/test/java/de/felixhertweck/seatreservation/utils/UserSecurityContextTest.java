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
package de.felixhertweck.seatreservation.utils;

import java.security.Principal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSecurityContextTest {

    @Mock private SecurityIdentity securityContext;

    @Mock private UserRepository userRepository;

    @Mock private Principal principal;

    @InjectMocks private UserSecurityContext userSecurityContext;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(Roles.USER));
    }

    @Test
    void getCurrentUser_ValidUser_ReturnsUser() throws UserNotFoundException {
        // Arrange
        String username = "testuser";
        when(securityContext.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(testUser);

        // Act
        User result = userSecurityContext.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result);
        assertEquals(username, result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(securityContext).getPrincipal();
        verify(principal).getName();
        verify(userRepository).findByUsername(username);
    }

    @Test
    void getCurrentUser_UserNotFound_ThrowsUserNotFoundException() {
        // Arrange
        String username = "nonexistentuser";
        when(securityContext.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(null);

        // Act & Assert
        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class, () -> userSecurityContext.getCurrentUser());

        assertEquals("Current user not found.", exception.getMessage());

        verify(securityContext).getPrincipal();
        verify(principal).getName();
        verify(userRepository).findByUsername(username);
    }

    @Test
    void getCurrentUser_NullPrincipal_ThrowsNullPointerException() {
        // Arrange
        when(securityContext.getPrincipal()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> userSecurityContext.getCurrentUser());

        verify(securityContext).getPrincipal();
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentUser_PrincipalWithNullName_CallsRepositoryWithNull() {
        // Arrange
        when(securityContext.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(null);
        when(userRepository.findByUsername(null)).thenReturn(null);

        // Act & Assert
        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class, () -> userSecurityContext.getCurrentUser());

        assertEquals("Current user not found.", exception.getMessage());

        verify(securityContext).getPrincipal();
        verify(principal).getName();
        verify(userRepository).findByUsername(null);
    }

    @Test
    void getCurrentUser_EmptyUsername_ThrowsUserNotFoundException() {
        // Arrange
        String emptyUsername = "";
        when(securityContext.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(emptyUsername);
        when(userRepository.findByUsername(emptyUsername)).thenReturn(null);

        // Act & Assert
        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class, () -> userSecurityContext.getCurrentUser());

        assertEquals("Current user not found.", exception.getMessage());

        verify(securityContext).getPrincipal();
        verify(principal).getName();
        verify(userRepository).findByUsername(emptyUsername);
    }

    @Test
    void getCurrentUser_MultipleCallsSameUser_CallsRepositoryEachTime()
            throws UserNotFoundException {
        // Arrange
        String username = "testuser";
        when(securityContext.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(testUser);

        // Act
        User result1 = userSecurityContext.getCurrentUser();
        User result2 = userSecurityContext.getCurrentUser();

        // Assert
        assertEquals(testUser, result1);
        assertEquals(testUser, result2);

        // Should call repository each time (no caching)
        verify(securityContext, times(2)).getPrincipal();
        verify(principal, times(2)).getName();
        verify(userRepository, times(2)).findByUsername(username);
    }

    @Test
    void getCurrentUser_DifferentUsersInSequence_ReturnsCorrectUsers()
            throws UserNotFoundException {
        // Arrange
        User secondUser = new User();
        secondUser.setUsername("seconduser");
        secondUser.setEmail("second@example.com");
        secondUser.setRoles(Set.of(Roles.ADMIN));

        Principal secondPrincipal = mock(Principal.class);

        when(securityContext.getPrincipal()).thenReturn(principal).thenReturn(secondPrincipal);
        when(principal.getName()).thenReturn("testuser");
        when(secondPrincipal.getName()).thenReturn("seconduser");
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(userRepository.findByUsername("seconduser")).thenReturn(secondUser);

        // Act
        User result1 = userSecurityContext.getCurrentUser();
        User result2 = userSecurityContext.getCurrentUser();

        // Assert
        assertEquals(testUser, result1);
        assertEquals("testuser", result1.getUsername());
        assertEquals(secondUser, result2);
        assertEquals("seconduser", result2.getUsername());

        verify(securityContext, times(2)).getPrincipal();
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByUsername("seconduser");
    }
}
