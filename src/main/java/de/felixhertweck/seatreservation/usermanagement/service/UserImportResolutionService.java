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

import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportResolutionDTO;
import de.felixhertweck.seatreservation.usermanagement.dto.UserImportResolutionResultDTO;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

/**
 * Applies the resolutions chosen for import conflicts. Every resolution runs in its own transaction
 * (each call into {@link UserService} is transactional), so one failing resolution never affects
 * the others.
 */
@ApplicationScoped
public class UserImportResolutionService {

    private static final Logger LOG = Logger.getLogger(UserImportResolutionService.class);

    private final UserService userService;

    @Inject
    public UserImportResolutionService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Applies all resolutions and reports the outcome of each.
     *
     * @param resolutions the resolutions to apply
     * @param currentUser the acting admin
     * @return one result per resolution, in the same order
     */
    public List<UserImportResolutionResultDTO> resolve(
            List<UserImportResolutionDTO> resolutions, AuthenticatedUser currentUser) {
        List<UserImportResolutionResultDTO> results = new ArrayList<>();
        for (UserImportResolutionDTO resolution : resolutions) {
            results.add(resolveOne(resolution, currentUser));
        }
        long succeeded = results.stream().filter(UserImportResolutionResultDTO::success).count();
        LOG.infof(
                "Resolved import conflicts: %d succeeded, %d failed.",
                succeeded, results.size() - succeeded);
        return results;
    }

    private UserImportResolutionResultDTO resolveOne(
            UserImportResolutionDTO resolution, AuthenticatedUser currentUser) {
        try {
            UserDTO user =
                    switch (resolution.action()) {
                        case UPDATE -> {
                            if (resolution.update() == null) {
                                throw new InvalidUserException(
                                        "The values to apply are required for an update.");
                            }
                            yield userService.updateUser(
                                    resolution.existingUserId(), resolution.update(), currentUser);
                        }
                        case REPLACE -> {
                            if (resolution.replacement() == null) {
                                throw new InvalidUserException(
                                        "The replacement user is required for a replacement.");
                            }
                            yield userService.replaceUser(
                                    resolution.existingUserId(),
                                    resolution.replacement(),
                                    currentUser);
                        }
                    };
            return new UserImportResolutionResultDTO(
                    resolution.existingUserId(), user.username(), true, null, user.id());
        } catch (InvalidUserException
                | UserNotFoundException
                | AccessDeniedException
                | DuplicateUserException e) {
            return failure(resolution, e.getMessage());
        } catch (RuntimeException e) {
            LOG.errorf(e, "Unexpected error resolving user %s.", resolution.existingUserId());
            return failure(resolution, "Unexpected error while resolving this user.");
        }
    }

    private static UserImportResolutionResultDTO failure(
            UserImportResolutionDTO resolution, String message) {
        String username =
                resolution.replacement() != null ? resolution.replacement().getUsername() : null;
        return new UserImportResolutionResultDTO(
                resolution.existingUserId(), username, false, message, null);
    }
}
