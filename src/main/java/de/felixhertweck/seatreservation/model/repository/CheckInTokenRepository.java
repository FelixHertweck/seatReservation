/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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

import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class CheckInTokenRepository implements PanacheRepositoryBase<CheckInToken, UUID> {

    /**
     * Finds a CheckInToken by its token value.
     *
     * @param token the token string
     * @return Optional containing the CheckInToken if found
     */
    public Optional<CheckInToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    /**
     * Finds a CheckInToken for a specific user and event.
     *
     * @param user the user
     * @param event the event
     * @return Optional containing the CheckInToken if found
     */
    public Optional<CheckInToken> findByUserAndEvent(User user, Event event) {
        return find("user = ?1 and event = ?2", user, event).firstResultOptional();
    }
}
