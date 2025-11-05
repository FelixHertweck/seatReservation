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

import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

/**
 * Initializer for creating the default admin user on application startup. Checks if an admin user
 * exists during application startup and creates one with a random password if not found.
 */
@ApplicationScoped
public class AdminUserInitializer {

    private static final Logger LOG = Logger.getLogger(AdminUserInitializer.class);

    @Inject UserRepository userRepository;

    @Inject UserService userService;

    private static final int PASSWORD_LENGTH = 12;

    /**
     * Generates a random password with mixed character types.
     *
     * @return a randomly generated password of fixed length
     */
    private static String generateRandomPassword() {
        String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(chars.charAt(SecurityUtils.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Observes the startup event and creates an admin user if one does not exist. Logs the
     * generated password on creation for initial access.
     *
     * @param ev the startup event triggered during application startup
     */
    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        LOG.info("Checking for admin user on application startup...");

        if (userRepository.findByUsernameOptional("admin").isEmpty()) {
            LOG.info("Admin user not found. Creating admin user...");
            String randomPassword = generateRandomPassword();
            LOG.infof(
                    """
                    --------- IMPORTANT ---------
                    Generated admin password: %s
                    --------- IMPORTANT ---------\
                    """,
                    randomPassword);
            UserCreationDTO userCreationDTO =
                    new UserCreationDTO(
                            "admin",
                            "admin@localhost",
                            randomPassword,
                            "System",
                            "Admin",
                            Set.of("system"));
            try {
                userService.createUser(userCreationDTO, Set.of(Roles.ADMIN), true);
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
