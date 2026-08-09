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
package de.felixhertweck.seatreservation.reservation.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.CheckInTokenRepository;
import de.felixhertweck.seatreservation.utils.CodeGenerator;

@ApplicationScoped
public class CheckInTokenService {

    @Inject CheckInTokenRepository checkInTokenRepository;

    /**
     * Finds an existing check-in token for the given user and event, or creates and persists a new
     * one if none exists. Used for real, stable user identities so all reservations for a given
     * (user, event) share a single token.
     *
     * @param user the user
     * @param event the event
     * @return existing or newly created CheckInToken
     */
    @Transactional
    public CheckInToken getOrCreateForUser(User user, Event event) {
        return checkInTokenRepository
                .findByUserAndEvent(user, event)
                .orElseGet(() -> createFresh(user, event));
    }

    /**
     * Always creates and persists a fresh check-in token for the given user and event. Used for
     * box-office guest transactions under the shared system user to ensure guest reservations
     * remain isolated.
     *
     * @param user the user (e.g. shared boxoffice user)
     * @param event the event
     * @return newly created CheckInToken
     */
    @Transactional
    public CheckInToken createFresh(User user, Event event) {
        String code = CodeGenerator.generateRandomCode();
        CheckInToken checkInToken = new CheckInToken(user, event, code);
        checkInTokenRepository.persist(checkInToken);
        return checkInToken;
    }
}
