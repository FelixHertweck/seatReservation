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
package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    private static final Logger LOG = Logger.getLogger(UserRepository.class);

    public User findByUsername(String username) {
        LOG.debugf("Finding user by username: %s", username);
        User user = find("username", username).firstResult();
        if (user != null) {
            LOG.debugf("User %s found.", username);
        } else {
            LOG.debugf("User %s not found.", username);
        }
        return user;
    }

    public Optional<User> findByUsernameOptional(String username) {
        LOG.debugf("Finding user by username (optional): %s", username);
        Optional<User> user = find("username", username).firstResultOptional();
        if (user.isPresent()) {
            LOG.debugf("User %s found (optional).", username);
        } else {
            LOG.debugf("User %s not found (optional).", username);
        }
        return user;
    }

    public User findByEmail(String email) {
        LOG.debugf("Finding user by email: %s", email);
        User user = find("email", email).firstResult();
        if (user != null) {
            LOG.debugf("User %s found.", email);
        } else {
            LOG.debugf("User %s not found.", email);
        }
        return user;
    }

    public List<User> findAllByEmail(String email) {
        LOG.debugf("Finding all users by email: %s", email);
        List<User> users = list("email", email);
        LOG.debugf("Found %d users with email %s.", users.size(), email);
        return users;
    }
}
