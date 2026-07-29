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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.SeatStatusDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.reservation.dto.UserEventResponseDTO;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class);

    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject SeatCartService seatCartService;

    /**
     * @param user a reference to the current user (id only, e.g. from {@code
     *     UserSecurityContext#getCurrentUserReference()}); only used as a foreign-key query
     *     parameter, never dereferenced beyond its ID
     */
    @Transactional
    public List<UserEventResponseDTO> getEventsForCurrentUser(User user) {
        LOG.debugf("Attempting to retrieve events for current user ID: %s", user.id);

        Map<UUID, Event> events = new HashMap<>();
        Map<UUID, Integer> reservationsAllowedByEvent = new HashMap<>();

        // Add allowances. Also grants seat-cart access for each event here, reusing this query
        // instead of SeatCartService doing its own DB lookup on every cart write.
        for (EventUserAllowance allowance :
                eventUserAllowanceRepository.findByUserWithEvent(user)) {
            Event event = allowance.getEvent();
            events.put(event.getId(), event);
            reservationsAllowedByEvent.put(event.getId(), allowance.getReservationsAllowedCount());
            seatCartService.grantAccess(
                    event.getId(), user.id, allowance.getReservationsAllowedCount());
        }

        // Reservations only add if event not already exists
        for (Reservation reservation : reservationRepository.findByUserWithEvent(user)) {
            Event event = reservation.getEvent();
            events.putIfAbsent(event.getId(), event);
            reservationsAllowedByEvent.putIfAbsent(event.getId(), 0);
        }

        // Bulk-load every relevant event's reservations in one query instead of relying on the
        // lazy event.getReservations() collection (which would run one query per event).
        Map<UUID, List<Reservation>> reservationsByEvent =
                events.isEmpty()
                        ? Map.of()
                        : reservationRepository
                                .find("event.id in ?1", events.keySet())
                                .list()
                                .stream()
                                .collect(Collectors.groupingBy(r -> r.getEvent().getId()));

        List<UserEventResponseDTO> result =
                events.values().stream()
                        .map(
                                event ->
                                        new UserEventResponseDTO(
                                                event,
                                                reservationsAllowedByEvent.get(event.getId()),
                                                reservationsByEvent.getOrDefault(
                                                        event.getId(), List.of())))
                        .map(dto -> withPendingSeatStatuses(dto, user.id))
                        .toList();

        LOG.debugf("Returning %d events for user ID: %s", result.size(), user.id);
        return result;
    }

    /**
     * Merges seats currently held in another user's Redis cart into the event's seat statuses as
     * {@link ReservationStatus#PENDING}, so the requesting user sees them as temporarily
     * unavailable. A seat already covered by a persisted {@link
     * de.felixhertweck.seatreservation.model.entity.Reservation} keeps that status. Seats the
     * requesting user holds themselves are excluded - those are the user's own in-progress
     * selection, not something blocking them, so they must stay selectable.
     */
    private UserEventResponseDTO withPendingSeatStatuses(UserEventResponseDTO dto, UUID userId) {
        Set<UUID> pendingSeatIds = seatCartService.findPendingSeatIds(dto.id(), userId);
        if (pendingSeatIds.isEmpty()) {
            return dto;
        }

        List<SeatStatusDTO> seatStatuses =
                dto.seatStatuses() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(dto.seatStatuses());
        Set<UUID> alreadyStatused =
                seatStatuses.stream().map(SeatStatusDTO::seatId).collect(Collectors.toSet());
        for (UUID seatId : pendingSeatIds) {
            if (!alreadyStatused.contains(seatId)) {
                seatStatuses.add(new SeatStatusDTO(seatId, ReservationStatus.PENDING));
            }
        }

        return new UserEventResponseDTO(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.startTime(),
                dto.endTime(),
                dto.bookingDeadline(),
                dto.bookingStartTime(),
                seatStatuses,
                dto.locationId(),
                dto.reservationsAllowed());
    }
}
