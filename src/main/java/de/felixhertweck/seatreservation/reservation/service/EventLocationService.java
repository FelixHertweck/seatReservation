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
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationSummaryDTO;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationService {
    private static final Logger LOG = Logger.getLogger(EventLocationService.class);

    @Inject UserRepository userRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;

    /**
     * Retrieves all event locations for which the specified user has event allowances or active
     * reservations.
     *
     * @param username the username of the user for whom to retrieve event locations
     * @return a list of event locations the user is allowed to access
     * @throws UserNotFoundException if the user with the specified username is not found
     */
    public List<UserEventLocationSummaryDTO> getLocationsForCurrentUser(String username)
            throws UserNotFoundException {
        LOG.debug("Stub: Retrieving event locations for user.");
        User user = userRepository.findByUsername(username);

        if (user == null) {
            LOG.warn("User not found.");
            throw new UserNotFoundException("User not found");
        }
        LOG.debugf("User %s found. Retrieving event allowances.", username);

        Set<EventLocation> locations = new HashSet<>();

        // Get all locations from allowances
        locations.addAll(
                eventUserAllowanceRepository.findByUserWithEventAndLocation(user).stream()
                        .map(allowance -> allowance.getEvent().getEventLocation())
                        .collect(Collectors.toSet()));

        // Get all locations from reservations
        locations.addAll(
                reservationRepository.findByUserWithEventAndLocation(user).stream()
                        .map(reservation -> reservation.getEvent().getEventLocation())
                        .collect(Collectors.toSet()));

        return locations.stream().map(UserEventLocationSummaryDTO::new).toList();
    }

    /**
     * Retrieves detail for a single event location by ID if the specified user has access to it.
     *
     * @param locationId the ID of the event location to retrieve
     * @param username the username of the requesting user
     * @return the detail DTO of the event location
     * @throws UserNotFoundException if the user is not found
     * @throws EventLocationNotFoundException if the event location is not found
     * @throws SecurityException if the user is not authorized to access the location
     */
    public UserEventLocationResponseDTO getLocationByIdForCurrentUser(
            UUID locationId, String username)
            throws UserNotFoundException, EventLocationNotFoundException, SecurityException {
        List<UserEventLocationSummaryDTO> allowedLocations = getLocationsForCurrentUser(username);
        boolean hasAccess = allowedLocations.stream().anyMatch(loc -> loc.id().equals(locationId));

        if (!hasAccess) {
            EventLocation loc = eventLocationRepository.findByIdOptional(locationId).orElse(null);
            if (loc == null) {
                throw new EventLocationNotFoundException(
                        "EventLocation with id " + locationId + " not found");
            }
            throw new SecurityException("User is not authorized to access this location");
        }

        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(locationId)
                        .orElseThrow(
                                () ->
                                        new EventLocationNotFoundException(
                                                "EventLocation with id "
                                                        + locationId
                                                        + " not found"));

        return new UserEventLocationResponseDTO(location);
    }
}
