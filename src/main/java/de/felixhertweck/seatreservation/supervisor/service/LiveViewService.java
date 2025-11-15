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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.SupervisorReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.WebsocketInitialDTO;
import de.felixhertweck.seatreservation.supervisor.dto.WebsocketUpdateDTO;
import de.felixhertweck.seatreservation.supervisor.exception.InvalidEventIdException;
import io.quarkus.arc.Lock;
import io.quarkus.websockets.next.WebSocketConnection;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LiveViewService {

    private static final Logger LOG = Logger.getLogger(LiveViewService.class);

    @Inject ReservationRepository reservationRepository;

    @Inject EventRepository eventRepository;
    @Inject UserRepository userRepository;

    // Map: eventId -> List of WebSocket Connections
    private final Map<Long, List<WebSocketConnection>> eventSubscriptions =
            new ConcurrentHashMap<>();

    // Configured ObjectMapper for JSON serialization
    private final ObjectMapper objectMapper;

    public LiveViewService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // Serialize Instant as ISO-8601 string, not as timestamp
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Registers a WebSocket connection for a specific event by parsing the event ID string. Sends
     * all current reservations for the event as initial message.
     *
     * @param eventIdStr the event ID as String to parse
     * @param connection the WebSocket connection
     * @throws InvalidEventIdException if the event ID cannot be parsed as a Long
     */
    @Lock
    public void registerConnection(String eventIdStr, WebSocketConnection connection)
            throws InvalidEventIdException {
        long eventId = parseEventId(eventIdStr);
        registerConnection(eventId, connection);
    }

    /**
     * Registers a websocket connection with authorization check by username.
     *
     * @param eventIdStr the event id as string
     * @param connection websocket connection
     * @param username current user's username
     * @throws InvalidEventIdException if id invalid
     */
    public void registerConnection(
            String eventIdStr, WebSocketConnection connection, String username)
            throws InvalidEventIdException {
        long eventId = parseEventId(eventIdStr);
        User user = userRepository.findByUsername(username);
        if (!isAuthorizedForEvent(user, eventId)) {
            throw new SecurityException("User is not authorized to access event " + eventId);
        }
        registerConnection(eventId, connection);
    }

    /**
     * Unregisters a WebSocket connection for a specific event by parsing the event ID string.
     *
     * @param eventIdStr the event ID as String to parse
     * @param connection the WebSocket connection
     * @throws InvalidEventIdException if the event ID cannot be parsed as a Long
     */
    @Lock
    public void unregisterConnection(String eventIdStr, WebSocketConnection connection)
            throws InvalidEventIdException {
        long eventId = parseEventId(eventIdStr);
        unregisterConnection(eventId, connection);
    }

    public void unregisterConnection(
            String eventIdStr, WebSocketConnection connection, String username)
            throws InvalidEventIdException {
        long eventId = parseEventId(eventIdStr);
        User user = userRepository.findByUsername(username);
        if (!isAuthorizedForEvent(user, eventId)) {
            throw new SecurityException("User is not authorized to access event " + eventId);
        }
        unregisterConnection(eventId, connection);
    }

    /**
     * Parses the event ID from a String and validates it.
     *
     * @param eventIdStr the event ID as String
     * @return the parsed event ID as Long
     * @throws InvalidEventIdException if the event ID cannot be parsed or is invalid
     */
    private long parseEventId(String eventIdStr) throws InvalidEventIdException {
        if (eventIdStr == null || eventIdStr.isBlank()) {
            LOG.warnf("Event ID is null or blank");
            throw new InvalidEventIdException("Event ID cannot be null or blank");
        }

        try {
            long eventId = Long.parseLong(eventIdStr);
            if (eventId <= 0) {
                LOG.warnf("Event ID must be positive, got: %s", eventIdStr);
                throw new InvalidEventIdException(
                        String.format("Event ID must be positive, got: %s", eventIdStr));
            }
            return eventId;
        } catch (NumberFormatException e) {
            LOG.warnf(e, "Invalid event ID format: %s", eventIdStr);
            throw new InvalidEventIdException(
                    String.format("Invalid event ID format: %s", eventIdStr), e);
        }
    }

    /**
     * Registers a WebSocket connection for a specific event. Sends all current reservations for the
     * event as initial message.
     *
     * @param eventId the event ID to subscribe to
     * @param connection the WebSocket connection
     */
    @Lock
    public void registerConnection(Long eventId, WebSocketConnection connection) {
        LOG.debugf("Registering WebSocket connection for event %d", eventId);

        eventSubscriptions.computeIfAbsent(eventId, k -> new ArrayList<>()).add(connection);
        LOG.infof(
                "Connection registered for event %d. Total connections: %d",
                eventId, eventSubscriptions.get(eventId).size());

        // Send initial reservations
        sendInitialReservations(eventId, connection);
    }

    /**
     * Unregisters a WebSocket connection for a specific event.
     *
     * @param eventId the event ID to unsubscribe from
     * @param connection the WebSocket connection
     */
    @Lock
    public void unregisterConnection(Long eventId, WebSocketConnection connection) {
        LOG.debugf("Unregistering WebSocket connection for event %d", eventId);

        List<WebSocketConnection> connections = eventSubscriptions.get(eventId);
        if (connections != null) {
            connections.remove(connection);
            LOG.infof(
                    "Connection unregistered for event %d. Remaining connections: %d",
                    eventId, connections.size());

            // Remove event entry if no more connections
            if (connections.isEmpty()) {
                eventSubscriptions.remove(eventId);
                LOG.debugf(
                        "No more connections for event %d, removing subscription entry.", eventId);
            }
        }
    }

    private boolean isAuthorizedForEvent(User user, long eventId) {
        if (user == null) return false;
        if (eventRepository.isUserSupervisor(eventId, user.id)) return true;
        Event event = eventRepository.findById(eventId);
        if (event != null
                && event.getManager() != null
                && Objects.equals(event.getManager().id, user.id)) return true;
        return user.getRoles() != null && user.getRoles().contains(Roles.ADMIN);
    }

    /**
     * Sends the initial list of reservations for an event to a specific connection.
     *
     * @param eventId the event ID
     * @param connection the WebSocket connection to send to
     */
    private void sendInitialReservations(Long eventId, WebSocketConnection connection) {
        LOG.debugf("Sending initial reservations for event %d", eventId);

        try {
            Event event = eventRepository.findById(eventId);
            EventLocation location = event.getEventLocation();
            List<Reservation> reservations = reservationRepository.findByEventId(eventId);

            List<SupervisorReservationResponseDTO> dtos =
                    reservations.stream()
                            .map(SupervisorReservationResponseDTO::new)
                            .collect(Collectors.toList());

            WebsocketInitialDTO initialMessage =
                    WebsocketInitialDTO.initial(location, event, reservations);
            connection
                    .sendText(objectMapper.writeValueAsString(initialMessage))
                    .await()
                    .indefinitely();

            LOG.debugf(
                    "Sent %d initial reservations to connection for event %d",
                    dtos.size(), eventId);
        } catch (IOException e) {
            LOG.errorf(e, "Error sending initial reservations for event %d to connection", eventId);
        }
    }

    /**
     * Broadcasts a check-in update to all subscribed clients for an event.
     *
     * @param eventId the event ID
     * @param reservation the reservation that was checked in
     */
    public void broadcastUpdate(Long eventId, Reservation reservation) {
        LOG.debugf(
                "Broadcasting check-in update for event %d, reservation: %s", eventId, reservation);

        List<WebSocketConnection> connections = eventSubscriptions.get(eventId);
        if (connections == null || connections.isEmpty()) {
            LOG.debugf("No active connections for event %d", eventId);
            return;
        }

        WebsocketUpdateDTO update = WebsocketUpdateDTO.update(reservation);
        broadcastToConnections(connections, update, eventId);
    }

    /**
     * Helper method to broadcast a message to all connections in a list.
     *
     * @param connections the list of connections to broadcast to
     * @param message the message to send
     * @param eventId the event ID (for logging)
     */
    private void broadcastToConnections(
            List<WebSocketConnection> connections, Object message, Long eventId) {
        List<WebSocketConnection> failedConnections = new ArrayList<>();

        for (WebSocketConnection connection : connections) {
            try {
                connection
                        .sendText(objectMapper.writeValueAsString(message))
                        .await()
                        .indefinitely();
                LOG.debugf("Message sent to connection for event %d", eventId);
            } catch (IOException e) {
                LOG.error("Error sending message to connection for event " + eventId, e);
                failedConnections.add(connection);
            }
        }

        // Remove failed connections
        if (!failedConnections.isEmpty()) {
            LOG.debugf(
                    "Removing %d failed connections for event %d",
                    failedConnections.size(), eventId);
            connections.removeAll(failedConnections);
        }
    }

    /**
     * Gets the number of active connections for a specific event.
     *
     * @param eventId the event ID
     * @return the number of active connections
     */
    public int getActiveConnectionCount(Long eventId) {
        List<WebSocketConnection> connections = eventSubscriptions.get(eventId);
        return connections == null ? 0 : connections.size();
    }
}
