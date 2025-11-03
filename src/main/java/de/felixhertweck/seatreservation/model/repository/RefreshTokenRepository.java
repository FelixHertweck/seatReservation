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
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.RefreshToken;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshToken> {

    /**
     * Find all refresh tokens for a specific user.
     *
     * @param user the user whose refresh tokens to find
     * @return a list of refresh tokens belonging to the user
     */
    public List<RefreshToken> findAllByUser(User user) {
        return list("user", user);
    }

    /**
     * Delete all refresh tokens for a specific user.
     *
     * @param user the user whose refresh tokens to delete
     * @return the number of tokens deleted
     */
    public long deleteAllByUser(User user) {
        return delete("user", user);
    }

    /**
     * Delete a refresh token by its ID and user if it exists.
     *
     * @param id the ID of the refresh token to delete
     * @param user the user who owns the refresh token
     * @return true if the token was deleted, false otherwise
     */
    public boolean deleteWithIdAndUser(Long id, User user) {
        return delete("id = ?1 and user = ?2", id, user) > 0;
    }
}
