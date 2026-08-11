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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.management.dto.ManagementOverviewDTO;
import de.felixhertweck.seatreservation.management.dto.ManagementOverviewStatsDTO;
import de.felixhertweck.seatreservation.management.dto.UpcomingEventDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OverviewService {

    private static final Logger LOG = Logger.getLogger(OverviewService.class);

    private static final int UPCOMING_EVENTS_LIMIT = 5;
    private static final int DEADLINES_LIMIT = 5;

    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject UserRepository userRepository;

    /**
     * Calculates aggregate statistics and lists for the manager dashboard overview.
     *
     * @param manager Currently authenticated manager
     * @return DTO containing aggregate overview statistics and event lists
     */
    public ManagementOverviewDTO getOverview(AuthenticatedUser manager) {
        LOG.debugf("Calculating overview stats for manager ID: %s", manager.id());

        User currentUser = userRepository.getReference(manager.id());

        List<Event> allEvents =
                manager.isAdmin()
                        ? eventRepository.listAll()
                        : eventRepository.findByManager(currentUser);

        List<Reservation> allReservations =
                manager.isAdmin()
                        ? reservationRepository.listAll()
                        : reservationRepository.findByManager(currentUser);

        List<EventUserAllowance> allAllowances =
                manager.isAdmin()
                        ? eventUserAllowanceRepository.listAll()
                        : eventUserAllowanceRepository.findByEventManager(currentUser);

        Instant now = Instant.now();

        long eventsCount = allEvents.size();

        List<Event> futureEvents =
                allEvents.stream()
                        .filter(e -> e.getStartTime() != null && !e.getStartTime().isBefore(now))
                        .toList();

        long upcomingEventsCount = futureEvents.size();

        long bookingOpenCount =
                allEvents.stream()
                        .filter(
                                e ->
                                        e.getBookingStartTime() != null
                                                && e.getBookingDeadline() != null
                                                && !now.isBefore(e.getBookingStartTime())
                                                && !now.isAfter(e.getBookingDeadline()))
                        .count();

        long reservationsCount = allReservations.size();
        long reservationsReserved =
                allReservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                        .count();
        long reservationsBlocked =
                allReservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.BLOCKED)
                        .count();
        long reservationsPending =
                allReservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                        .count();

        Set<UUID> locationIds =
                allEvents.stream()
                        .map(
                                e ->
                                        e.getEventLocation() != null
                                                ? e.getEventLocation().getId()
                                                : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<UUID, Integer> seatCounts =
                locationIds.isEmpty()
                        ? Map.of()
                        : eventLocationRepository.getSeatCountsByLocationIds(locationIds);

        Map<UUID, Long> reservedCountByEventId =
                allReservations.stream()
                        .filter(
                                r ->
                                        r.getStatus() == ReservationStatus.RESERVED
                                                && r.getEvent() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getEvent().getId(), Collectors.counting()));

        long occupancyReserved = 0;
        long occupancyCapacity = 0;
        for (Event e : futureEvents) {
            occupancyReserved += reservedCountByEventId.getOrDefault(e.getId(), 0L);
            if (e.getEventLocation() != null) {
                occupancyCapacity += seatCounts.getOrDefault(e.getEventLocation().getId(), 0);
            }
        }
        int occupancyPercent =
                occupancyCapacity > 0
                        ? (int) Math.round(((double) occupancyReserved / occupancyCapacity) * 100.0)
                        : 0;

        Map<String, Long> reservedCountByPair =
                allReservations.stream()
                        .filter(
                                r ->
                                        r.getStatus() == ReservationStatus.RESERVED
                                                && r.getEvent() != null
                                                && r.getUser() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getEvent().getId() + ":" + r.getUser().getId(),
                                        Collectors.counting()));

        long contingentUsed = 0;
        long contingentGranted = 0;
        for (EventUserAllowance a : allAllowances) {
            if (a.getEvent() == null || a.getUser() == null) {
                continue;
            }
            long used =
                    reservedCountByPair.getOrDefault(
                            a.getEvent().getId() + ":" + a.getUser().getId(), 0L);
            contingentUsed += used;
            contingentGranted += a.getReservationsAllowedCount() + used;
        }
        int contingentUsagePercent =
                contingentGranted > 0
                        ? (int) Math.round(((double) contingentUsed / contingentGranted) * 100.0)
                        : 0;

        ManagementOverviewStatsDTO stats =
                new ManagementOverviewStatsDTO(
                        eventsCount,
                        upcomingEventsCount,
                        bookingOpenCount,
                        reservationsCount,
                        reservationsReserved,
                        reservationsBlocked,
                        reservationsPending,
                        occupancyPercent,
                        occupancyReserved,
                        occupancyCapacity,
                        contingentUsagePercent,
                        contingentUsed,
                        contingentGranted);

        List<UpcomingEventDTO> upcomingEvents =
                futureEvents.stream()
                        .sorted(Comparator.comparing(Event::getStartTime))
                        .limit(UPCOMING_EVENTS_LIMIT)
                        .map(e -> toUpcomingEventDto(e, seatCounts, reservedCountByEventId))
                        .toList();

        List<UpcomingEventDTO> deadlineWarnings =
                allEvents.stream()
                        .filter(
                                e ->
                                        e.getBookingDeadline() != null
                                                && !e.getBookingDeadline().isBefore(now))
                        .sorted(Comparator.comparing(Event::getBookingDeadline))
                        .limit(DEADLINES_LIMIT)
                        .map(e -> toUpcomingEventDto(e, seatCounts, reservedCountByEventId))
                        .toList();

        return new ManagementOverviewDTO(stats, upcomingEvents, deadlineWarnings);
    }

    private UpcomingEventDTO toUpcomingEventDto(
            Event event, Map<UUID, Integer> seatCounts, Map<UUID, Long> reservedCountByEventId) {
        String locationName =
                event.getEventLocation() != null ? event.getEventLocation().getName() : null;
        int capacity =
                event.getEventLocation() != null
                        ? seatCounts.getOrDefault(event.getEventLocation().getId(), 0)
                        : 0;
        int reservedCount = Math.toIntExact(reservedCountByEventId.getOrDefault(event.getId(), 0L));
        return new UpcomingEventDTO(
                event.getId(),
                event.getName(),
                event.getStartTime(),
                event.getBookingDeadline(),
                locationName,
                reservedCount,
                capacity);
    }
}
