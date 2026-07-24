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

        Map<UUID, UserEventResponseDTO> eventMap = new HashMap<>();

        // Add allowances. Also grants seat-cart access for each event here, reusing this query
        // instead of SeatCartService doing its own DB lookup on every cart write.
        eventUserAllowanceRepository
                .findByUser(user)
                .forEach(
                        allowance -> {
                            eventMap.put(
                                    allowance.getEvent().getId(),
                                    new UserEventResponseDTO(
                                            allowance.getEvent(),
                                            allowance.getReservationsAllowedCount()));
                            seatCartService.grantAccess(allowance.getEvent().getId(), user.id);
                        });

        // Reservations only add if event not already exists
        reservationRepository
                .findByUser(user)
                .forEach(
                        reservation ->
                                eventMap.putIfAbsent(
                                        reservation.getEvent().getId(),
                                        new UserEventResponseDTO(reservation.getEvent(), 0)));

        LOG.debugf("Returning %d events for user ID: %s", eventMap.size(), user.id);
        return eventMap.values().stream().map(this::withPendingSeatStatuses).toList();
    }

    /**
     * Merges seats currently held in another user's Redis cart into the event's seat statuses as
     * {@link ReservationStatus#PENDING}, so the requesting user sees them as temporarily
     * unavailable. A seat already covered by a persisted {@link
     * de.felixhertweck.seatreservation.model.entity.Reservation} keeps that status.
     */
    private UserEventResponseDTO withPendingSeatStatuses(UserEventResponseDTO dto) {
        Set<UUID> pendingSeatIds = seatCartService.findPendingSeatIds(dto.id());
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
