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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInUpdateDTO;
import de.felixhertweck.seatreservation.supervisor.dto.InitialReservationsDTO;
import de.felixhertweck.seatreservation.supervisor.dto.LiveReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.exception.InvalidEventIdException;
import io.quarkus.arc.Lock;
import io.quarkus.websockets.next.WebSocketConnection;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LiveViewService {

    private static final Logger LOG = Logger.getLogger(LiveViewService.class);

    @Inject ReservationRepository reservationRepository;

    // Map: eventId -> List of WebSocket Connections
    private final Map<Long, List<WebSocketConnection>> eventSubscriptions =
            new ConcurrentHashMap<>();

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

    /**
     * Sends the initial list of reservations for an event to a specific connection.
     *
     * @param eventId the event ID
     * @param connection the WebSocket connection to send to
     */
    private void sendInitialReservations(Long eventId, WebSocketConnection connection) {
        LOG.debugf("Sending initial reservations for event %d", eventId);

        try {
            List<Reservation> reservations = reservationRepository.findByEventId(eventId);

            List<LiveReservationResponseDTO> dtos =
                    reservations.stream()
                            .map(LiveReservationResponseDTO::new)
                            .collect(Collectors.toList());

            InitialReservationsDTO initialMessage = InitialReservationsDTO.initial(dtos);
            ObjectMapper mapper = new ObjectMapper();
            connection.sendText(mapper.writeValueAsString(initialMessage)).await().indefinitely();

            LOG.infof(
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
    public void broadcastCheckInUpdate(Long eventId, LiveReservationResponseDTO reservation) {
        LOG.debugf(
                "Broadcasting check-in update for event %d, reservation: %s", eventId, reservation);

        List<WebSocketConnection> connections = eventSubscriptions.get(eventId);
        if (connections == null || connections.isEmpty()) {
            LOG.debugf("No active connections for event %d", eventId);
            return;
        }

        CheckInUpdateDTO update = CheckInUpdateDTO.checkedIn(reservation);
        broadcastToConnections(connections, update, eventId);
    }

    /**
     * Broadcasts a cancellation update to all subscribed clients for an event.
     *
     * @param eventId the event ID
     * @param reservation the reservation that was cancelled
     */
    public void broadcastCancellationUpdate(Long eventId, LiveReservationResponseDTO reservation) {
        LOG.debugf(
                "Broadcasting cancellation update for event %d, reservation: %s",
                eventId, reservation);

        List<WebSocketConnection> connections = eventSubscriptions.get(eventId);
        if (connections == null || connections.isEmpty()) {
            LOG.debugf("No active connections for event %d", eventId);
            return;
        }

        CheckInUpdateDTO update = CheckInUpdateDTO.cancelled(reservation);
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
                ObjectMapper mapper = new ObjectMapper();
                connection.sendText(mapper.writeValueAsString(message)).await().indefinitely();
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
