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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserCreationDto;
import de.felixhertweck.seatreservation.usermanagement.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportResolutionAction;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportResolutionDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportResolutionResultDTO;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserImportResolutionServiceTest {

    private UserService userService;
    private UserImportResolutionService resolutionService;
    private final AuthenticatedUser admin =
            new AuthenticatedUser(
                    UUID.fromString("00000000-0000-0000-0000-000000000009"), Set.of("ADMIN"));

    private static final UUID EXISTING_1 = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID EXISTING_2 = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID NEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        resolutionService = new UserImportResolutionService(userService);
    }

    private static AdminUserUpdateDTO update() {
        return new AdminUserUpdateDTO(
                "First", "Last", null, "a@example.com", false, true, Set.of("USER"), Set.of("x"));
    }

    private static AdminUserCreationDto replacement(String username) {
        return new AdminUserCreationDto(
                username,
                "a@example.com",
                false,
                false,
                "pass",
                "First",
                "Last",
                Set.of("USER"),
                Set.of());
    }

    private static UserDTO userDto(UUID id, String username) {
        return new UserDTO(id, username, "F", "L", null, false, false, Set.of("USER"), Set.of());
    }

    @Test
    void resolve_UpdateAndReplace_Succeed() {
        when(userService.updateUser(any(), any(), any())).thenReturn(userDto(EXISTING_1, "anna"));
        when(userService.replaceUser(any(), any(), any())).thenReturn(userDto(NEW_ID, "bob"));

        List<UserImportResolutionResultDTO> results =
                resolutionService.resolve(
                        List.of(
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.UPDATE,
                                        EXISTING_1,
                                        update(),
                                        null),
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.REPLACE,
                                        EXISTING_2,
                                        null,
                                        replacement("bob"))),
                        admin);

        assertEquals(2, results.size());
        assertTrue(results.get(0).success());
        assertEquals(EXISTING_1, results.get(0).userId());
        assertTrue(results.get(1).success());
        assertEquals("bob", results.get(1).username());
        assertEquals(NEW_ID, results.get(1).userId());
    }

    @Test
    void resolve_OneFailureDoesNotStopTheOthers() {
        when(userService.updateUser(any(), any(), any()))
                .thenThrow(new UserNotFoundException("gone"))
                .thenReturn(userDto(EXISTING_2, "carl"));

        List<UserImportResolutionResultDTO> results =
                resolutionService.resolve(
                        List.of(
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.UPDATE,
                                        EXISTING_1,
                                        update(),
                                        null),
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.UPDATE,
                                        EXISTING_2,
                                        update(),
                                        null)),
                        admin);

        assertFalse(results.get(0).success());
        assertEquals("gone", results.get(0).message());
        assertNull(results.get(0).userId());
        assertTrue(results.get(1).success());
    }

    @Test
    void resolve_ReplaceOfOwnAccount_IsReportedAsFailure() {
        when(userService.replaceUser(any(), any(), any()))
                .thenThrow(new AccessDeniedException("Admins cannot delete their own accounts."));

        List<UserImportResolutionResultDTO> results =
                resolutionService.resolve(
                        List.of(
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.REPLACE,
                                        EXISTING_1,
                                        null,
                                        replacement("admin"))),
                        admin);

        assertFalse(results.get(0).success());
        assertEquals("Admins cannot delete their own accounts.", results.get(0).message());
    }

    @Test
    void resolve_MissingPayload_IsReportedAsFailure() {
        List<UserImportResolutionResultDTO> results =
                resolutionService.resolve(
                        List.of(
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.UPDATE, EXISTING_1, null, null),
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.REPLACE,
                                        EXISTING_2,
                                        null,
                                        null)),
                        admin);

        assertFalse(results.get(0).success());
        assertFalse(results.get(1).success());
        verify(userService, never()).updateUser(any(), any(), any());
        verify(userService, never()).replaceUser(any(), any(), any());
    }

    @Test
    void resolve_UnexpectedError_DoesNotLeakDetails() {
        when(userService.updateUser(any(), any(), any()))
                .thenThrow(new IllegalStateException("db exploded"));

        List<UserImportResolutionResultDTO> results =
                resolutionService.resolve(
                        List.of(
                                new UserImportResolutionDTO(
                                        UserImportResolutionAction.UPDATE,
                                        EXISTING_1,
                                        update(),
                                        null)),
                        admin);

        assertFalse(results.get(0).success());
        assertFalse(results.get(0).message().contains("db exploded"));
    }
}
