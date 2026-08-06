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
package de.felixhertweck.seatreservation.reservation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationResponseDTO;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationService {
    private static final Logger LOG = Logger.getLogger(EventLocationService.class);

    @Inject UserRepository userRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject EventLocationRepository eventLocationRepository;

    /**
     * Retrieves all event locations for which the specified user has event allowances or active
     * reservations.
     *
     * @param username the username of the user for whom to retrieve event locations
     * @return a list of event locations the user is allowed to access
     * @throws UserNotFoundException if the user with the specified username is not found
     */
    public List<UserEventLocationResponseDTO> getLocationsForCurrentUser(String username)
            throws UserNotFoundException {
        LOG.debug("Stub: Retrieving event locations for user.");
        User user = userRepository.findByUsername(username);

        if (user == null) {
            LOG.warn("User not found.");
            throw new UserNotFoundException("User not found");
        }
        LOG.debugf("User %s found. Retrieving event allowances.", username);

        Set<java.util.UUID> locationIds = new HashSet<>();

        // Get all locations from allowances
        locationIds.addAll(
                eventUserAllowanceRepository.findByUserWithEventAndLocation(user).stream()
                        .map(allowance -> allowance.getEvent().getEventLocation().getId())
                        .collect(Collectors.toSet()));

        // Get all locations from reservations
        locationIds.addAll(
                reservationRepository.findByUserWithEventAndLocation(user).stream()
                        .map(reservation -> reservation.getEvent().getEventLocation().getId())
                        .collect(Collectors.toSet()));

        if (locationIds.isEmpty()) {
            return List.of();
        }

        return eventLocationRepository
                .find(
                        "select el from EventLocation el left join fetch el.manager where el.id in"
                                + " ?1",
                        locationIds)
                .list()
                .stream()
                .map(UserEventLocationResponseDTO::new)
                .toList();
    }
}
