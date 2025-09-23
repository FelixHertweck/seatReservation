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
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationResponseDTO;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationService {
    private static final Logger LOG = Logger.getLogger(EventLocationService.class);

    @Inject UserRepository userRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    public List<UserEventLocationResponseDTO> getLocationsForCurrentUser(String username) {
        LOG.debugf("Stub: Retrieving event locations for user: %s", username);
        User user = userRepository.findByUsername(username);

        if (user == null) {
            LOG.warnf("User not found: %s", username);
            throw new UserNotFoundException("User not found: " + username);
        }
        LOG.debugf("User %s found. Retrieving event allowances.", username);

        Set<EventLocation> locations = new HashSet<>();

        // Get all locations from allowances
        locations.addAll(
                eventUserAllowanceRepository.findByUser(user).stream()
                        .map(allowance -> allowance.getEvent().getEventLocation())
                        .collect(Collectors.toSet()));

        // Get all locations from reservations
        locations.addAll(
                user.getReservations().stream()
                        .map(reservation -> reservation.getEvent().getEventLocation())
                        .collect(Collectors.toSet()));

        return locations.stream().map(UserEventLocationResponseDTO::new).toList();
    }
}
