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

import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

/**
 * Central ownership check for event locations, shared by the area, entrance, marker and seat
 * services. Keeping this in one place avoids the authorization rule drifting between the callers.
 */
@ApplicationScoped
public class EventLocationAccessService {

    private static final Logger LOG = Logger.getLogger(EventLocationAccessService.class);

    @Inject EventLocationRepository eventLocationRepository;

    /**
     * Finds an event location by ID and verifies that the given user may write to it, i.e. is
     * either an ADMIN or the location's manager.
     *
     * @param eventLocationId the event location ID to find, must not be null
     * @param user the user attempting to access the event location
     * @return the event location entity
     * @throws ValidationException if the ID is null
     * @throws EventLocationNotFoundException if no such event location exists
     * @throws AccessDeniedException if the user neither is an ADMIN nor manages the location
     */
    public EventLocation findOwnedEventLocation(UUID eventLocationId, AuthenticatedUser user) {
        if (eventLocationId == null) {
            throw new ValidationException("EventLocation ID must not be null");
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
     * @throws AccessDeniedException if the user neither is an ADMIN nor manages the location
     */
    public void requireAccess(EventLocation eventLocation, AuthenticatedUser user) {
        if (eventLocation == null
                || user == null
                || (!user.isAdmin()
                        && !eventLocationRepository.isUserManager(
                                eventLocation.getId(), user.id()))) {
            LOG.warnf(
                    "User %s is not authorized to manage event location %s",
                    user == null ? null : user.id(),
                    eventLocation == null ? null : eventLocation.getId());
            throw new AccessDeniedException("Manager does not own this EventLocation");
        }
    }
}
