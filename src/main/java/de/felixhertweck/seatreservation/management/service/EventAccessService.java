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

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

/**
 * Central management-ownership check for events, shared by {@link EventService}, {@link
 * ReservationService} and {@link EventReservationAllowanceService}. Keeping this in one place
 * avoids the authorization rule ("is this user a manager of the event, or an ADMIN") drifting
 * between the callers. Mirrors {@link EventLocationAccessService}, which plays the same role for
 * event locations.
 */
@ApplicationScoped
public class EventAccessService {

    private static final Logger LOG = Logger.getLogger(EventAccessService.class);

    @Inject EventRepository eventRepository;

    /**
     * Finds an event by ID and verifies that the given user may manage it, i.e. is either an ADMIN
     * or a manager of the event.
     *
     * @param eventId the event ID to find, must not be null
     * @param user the user attempting to access the event
     * @return the event entity
     * @throws EventNotFoundException if no such event exists
     * @throws SecurityException if the user neither is an ADMIN nor manages the event
     */
    public Event findOwnedEvent(UUID eventId, AuthenticatedUser user) {
        Event event =
                eventRepository
                        .findByIdOptional(eventId)
                        .orElseThrow(
                                () ->
                                        new EventNotFoundException(
                                                "Event with id " + eventId + " not found"));
        requireAccess(event, user);
        return event;
    }

    /**
     * Verifies that the given user may manage the given event.
     *
     * @param event the event to check
     * @param user the user attempting to access the event
     * @throws SecurityException if the user neither is an ADMIN nor manages the event
     */
    public void requireAccess(Event event, AuthenticatedUser user) {
        if (!isManager(event, user)) {
            LOG.warnf(
                    "User %s is not authorized to manage event %s",
                    user == null ? null : user.id(), event == null ? null : event.getId());
            throw new SecurityException("User is not a manager of this event");
        }
    }

    /**
     * Checks whether the given user may manage the given event, without throwing. Null-safe, unlike
     * inlining the equivalent {@code eventRepository.isUserManager(...) || user.isAdmin()} check at
     * call sites, which dereferences {@code user}/{@code event} unconditionally.
     *
     * @param event the event to check, may be {@code null}
     * @param user the user attempting to access the event, may be {@code null}
     * @return {@code true} if the user is an ADMIN or a manager of the event
     */
    public boolean isManager(Event event, AuthenticatedUser user) {
        if (event == null || user == null) {
            return false;
        }
        return user.isAdmin() || eventRepository.isUserManager(event.getId(), user.id());
    }
}
