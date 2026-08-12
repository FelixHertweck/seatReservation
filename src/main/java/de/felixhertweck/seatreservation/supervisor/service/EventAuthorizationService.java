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
package de.felixhertweck.seatreservation.supervisor.service;

import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;

/**
 * Central operator-access check for events, shared by {@link LiveViewService}, {@link
 * BoxOfficeService} and {@link CheckInService}. A user may operate on an event (live view,
 * check-in, box office) if they are a supervisor or manager of the event, or an ADMIN. Keeping this
 * in one place avoids the rule drifting between the three call sites.
 */
@ApplicationScoped
public class EventAuthorizationService {

    @Inject EventRepository eventRepository;

    /**
     * Checks whether the given user may operate on the given event, without throwing.
     *
     * @param user the user attempting access, may be {@code null}
     * @param eventId the event ID, may be {@code null}
     * @return {@code true} if the user is a supervisor or manager of the event, or an ADMIN
     */
    public boolean isAuthorizedForEvent(AuthenticatedUser user, UUID eventId) {
        if (user == null || eventId == null) {
            return false;
        }
        if (eventRepository.isUserSupervisor(eventId, user.id())) {
            return true;
        }
        if (eventRepository.isUserManager(eventId, user.id())) {
            return true;
        }
        return user.isAdmin();
    }

    /**
     * Verifies that the given user may operate on the given event.
     *
     * @param user the user attempting access
     * @param eventId the event ID
     * @throws SecurityException if the user is neither a supervisor/manager of the event nor an
     *     ADMIN
     */
    public void assertAuthorizedForEvent(AuthenticatedUser user, UUID eventId) {
        if (!isAuthorizedForEvent(user, eventId)) {
            throw new SecurityException("User is not authorized to access event " + eventId);
        }
    }
}
