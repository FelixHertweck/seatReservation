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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import org.jboss.logging.Logger;

/** Utility class for resolving and validating manager assignments shared across services. */
public final class ManagerResolutionUtils {

    private static final Logger LOG = Logger.getLogger(ManagerResolutionUtils.class);

    private ManagerResolutionUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Retrieves a set of User entities based on the provided manager IDs. Every resolved user must
     * hold the MANAGER or ADMIN role, since being listed as a manager grants manager-level access
     * (e.g. supervisor tooling treats any manager as privileged regardless of the user's own role).
     *
     * @param userRepository repository used to look up the candidate users
     * @param managerIds Set of manager user IDs
     * @param context short label identifying the assignment target for log/error messages, e.g.
     *     "event" or "event location"
     * @return Set of User entities
     * @throws IllegalArgumentException if any manager ID is invalid or the user lacks MANAGER/ADMIN
     */
    public static Set<User> resolveManagers(
            UserRepository userRepository, Set<UUID> managerIds, String context) {
        Set<User> managers = new HashSet<>();
        if (managerIds == null || managerIds.isEmpty()) {
            return managers;
        }
        List<User> foundManagers = userRepository.findByIds(List.copyOf(managerIds));
        managers.addAll(foundManagers);

        if (managers.size() != managerIds.size()) {
            Set<UUID> foundIds = foundManagers.stream().map(u -> u.id).collect(Collectors.toSet());
            for (UUID managerId : managerIds) {
                if (!foundIds.contains(managerId)) {
                    LOG.warnf(
                            "User with id %s not found for %s manager assignment.",
                            managerId, context);
                    throw new IllegalArgumentException("User with id " + managerId + " not found");
                }
            }
        }

        for (User candidate : foundManagers) {
            if (!candidate.getRoles().contains(Roles.MANAGER)
                    && !candidate.getRoles().contains(Roles.ADMIN)) {
                LOG.warnf(
                        "User with id %s lacks MANAGER/ADMIN role for %s manager assignment.",
                        candidate.id, context);
                throw new IllegalArgumentException(
                        "User with id "
                                + candidate.id
                                + " must have the MANAGER or ADMIN role to be assigned as a"
                                + " manager");
            }
        }
        return managers;
    }
}
