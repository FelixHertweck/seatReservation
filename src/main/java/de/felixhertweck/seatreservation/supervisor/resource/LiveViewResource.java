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
package de.felixhertweck.seatreservation.supervisor.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.supervisor.service.LiveViewService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import org.jboss.logging.Logger;

@WebSocket(path = "/api/supervisor/liveview/{eventId}")
@RolesAllowed({Roles.SUPERVISOR, Roles.ADMIN, Roles.MANAGER})
public class LiveViewResource {

    private static final Logger LOG = Logger.getLogger(LiveViewResource.class);

    @Inject LiveViewService webSocketService;

    @Inject UserSecurityContext userSecurityContext;

    /**
     * Handles WebSocket connection opening. Registers the connection for the event and sends
     * initial reservations.
     *
     * @param connection the WebSocket connection
     * @param eventIdStr the event ID from the path parameter as String
     */
    @OnOpen
    public void onOpen(WebSocketConnection connection, @PathParam("eventId") String eventIdStr) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf("WebSocket connection opened for event %s by user %s", eventIdStr, currentUser);

        // Register the connection with username for authorization checks
        webSocketService.registerConnection(eventIdStr, connection, currentUser.getUsername());

        LOG.infof("WebSocket connection successfully registered for event %s", eventIdStr);
    }

    /**
     * Handles WebSocket connection closing. Unregisters the connection from the event.
     *
     * @param connection the WebSocket connection
     * @param eventIdStr the event ID from the path parameter as String
     */
    @OnClose
    public void onClose(WebSocketConnection connection, @PathParam("eventId") String eventIdStr) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof("WebSocket connection closed for event %s by user %s", eventIdStr, currentUser);

        // Unregister the connection with username for authorization checks
        webSocketService.unregisterConnection(eventIdStr, connection, currentUser.getUsername());

        LOG.infof("WebSocket connection unregistered for event %s", eventIdStr);
    }
}
