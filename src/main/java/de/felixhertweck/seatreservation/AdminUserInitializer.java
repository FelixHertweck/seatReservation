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

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AdminUserInitializer {

    private static final Logger LOG = Logger.getLogger(AdminUserInitializer.class);

    @Inject UserRepository userRepository;

    @Inject UserService userService;

    // Password hash for 'admin' from import.sql:
    // '$2a$10$IagMwMnYnQAAq6n2p2oe9OOJJKGB7qp.O7NnVdWD6JFeMHwTSNS4q'
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$IagMwMnYnQAAq6n2p2oe9OOJJKGB7qp.O7NnVdWD6JFeMHwTSNS4q";

    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        LOG.info("Checking for admin user on application startup...");

        if (userRepository.findByUsernameOptional("admin").isEmpty()) {
            LOG.info("Admin user not found. Creating admin user...");
            try {
                userService.createAdminUserWithHashedPassword(
                        "admin",
                        "admin@localhost",
                        ADMIN_PASSWORD_HASH,
                        "Admin",
                        "User",
                        Set.of(Roles.ADMIN),
                        Set.of());
                LOG.info("Admin user created successfully.");
            } catch (DuplicateUserException e) {
                LOG.warnf("Admin user already exists, skipping creation: %s", e.getMessage());
            } catch (Exception e) {
                LOG.errorf(e, "Failed to create admin user on startup: %s", e.getMessage());
            }
        } else {
            LOG.info("Admin user already exists. Skipping creation.");
        }
    }
}
