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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;

@ApplicationScoped
public class UserSecurityContext {

    @Inject SecurityContext securityContext;

    @Inject UserRepository userRepository;

    /**
     * Retrieves the current authenticated user based on the security context.
     *
     * @return The current User entity.
     * @throws UserNotFoundException If the current user cannot be found in the database.
     */
    public User getCurrentUser() throws UserNotFoundException {
        Principal principal = securityContext.getUserPrincipal();
        User currentUser = userRepository.findByUsername(principal.getName());
        if (currentUser == null) {
            throw new UserNotFoundException("Current user not found.");
        }
        return currentUser;
    }
}
