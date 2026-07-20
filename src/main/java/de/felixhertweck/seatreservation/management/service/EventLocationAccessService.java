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
package de.felixhertweck.seatreservation.management.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;

/**
 * Central ownership check for event locations, shared by the area, entrance, marker and seat
 * services. Keeping this in one place avoids the authorization rule drifting between the callers.
 */
@ApplicationScoped
public class EventLocationAccessService {

    @Inject EventLocationRepository eventLocationRepository;

    /**
     * Finds an event location by ID and verifies that the given user may write to it, i.e. is
     * either an ADMIN or the location's manager.
     *
     * @param eventLocationId the event location ID to find, must not be null
     * @param user the user attempting to access the event location
     * @return the event location entity
     * @throws IllegalArgumentException if the ID is null
     * @throws EventLocationNotFoundException if no such event location exists
     * @throws SecurityException if the user neither is an ADMIN nor manages the location
     */
    public EventLocation findOwnedEventLocation(Long eventLocationId, User user) {
        if (eventLocationId == null) {
            throw new IllegalArgumentException("EventLocation ID must not be null");
        }
        EventLocation eventLocation =
                eventLocationRepository
                        .findByIdOptional(eventLocationId)
                        .orElseThrow(
                                () ->
                                        new EventLocationNotFoundException(
                                                "EventLocation with id "
                                                        + eventLocationId
                                                        + " not found"));
        requireAccess(eventLocation, user);
        return eventLocation;
    }

    /**
     * Verifies that the given user may write to the given event location.
     *
     * @param eventLocation the event location to check
     * @param user the user attempting to access the event location
     * @throws SecurityException if the user neither is an ADMIN nor manages the location
     */
    public void requireAccess(EventLocation eventLocation, User user) {
        if (user.getRoles().contains(Roles.ADMIN)) {
            return;
        }
        // Compare by ID rather than by User#equals: the manager is a lazy association and its
        // proxy's equals() semantics are not something the authorization check should depend on.
        if (!eventLocation.getManager().getId().equals(user.getId())) {
            throw new SecurityException("Manager does not own this EventLocation");
        }
    }
}
