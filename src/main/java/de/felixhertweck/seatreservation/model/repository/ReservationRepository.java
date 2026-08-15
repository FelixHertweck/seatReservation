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
package de.felixhertweck.seatreservation.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class ReservationRepository implements PanacheRepositoryBase<Reservation, UUID> {
    /**
     * Finds all reservations for events managed by a specific user.
     *
     * @param manager the manager user to search for
     * @return a list of reservations for events managed by the specified user
     */
    public List<Reservation> findByManager(User manager) {
        return find(
                        "SELECT r FROM Reservation r JOIN r.event e JOIN e.managers m WHERE m ="
                                + " ?1",
                        manager)
                .list();
    }

    /**
     * Finds all reservations for a specific event.
     *
     * @param event the event to search for
     * @return a list of reservations for the specified event
     */
    public List<Reservation> findByEvent(Event event) {
        return find("event", event).list();
    }

    /**
     * Finds reservations by their IDs.
     *
     * @param ids the reservation IDs to search for
     * @return a list of reservations matching the IDs
     */
    @Override
    public List<Reservation> findByIds(List<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return find("id in ?1", ids).list();
    }

    /**
     * Finds all reservations for a collection of event IDs, eagerly fetching each reservation's
     * seat.
     *
     * @param eventIds the collection of event IDs to search for
     * @return a list of reservations with seat pre-fetched
     */
    public List<Reservation> findByEventIdsWithSeat(Collection<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return find(
                        "select r from Reservation r left join fetch r.seat where r.event.id in ?1",
                        eventIds)
                .list();
    }

    /**
     * Retrieves reserved seat counts aggregated by event ID for a collection of event IDs.
     *
     * @param eventIds collection of event IDs
     * @return map of event ID to reserved seat count
     */
    public Map<UUID, Integer> getReservedSeatCountsByEventIds(Collection<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> results =
                getEntityManager()
                        .createQuery(
                                "SELECT r.event.id, COUNT(r) FROM Reservation r WHERE r.event.id IN"
                                    + " ?1 AND r.status ="
                                    + " de.felixhertweck.seatreservation.model.entity.ReservationStatus.RESERVED"
                                    + " GROUP BY r.event.id",
                                Object[].class)
                        .setParameter(1, eventIds)
                        .getResultList();
        return results.stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Long) row[1]).intValue()));
    }

    /**
     * Finds all reservations for a given user that are not blocked.
     *
     * @param user the user to search for
     * @return a list of non-blocked reservations for the specified user
     */
    public List<Reservation> findByUser(User user) {
        return find("user = ?1 and status != ?2", user, ReservationStatus.BLOCKED).list();
    }

    /**
     * Finds all reservations for a given user that are not blocked, eagerly fetching each
     * reservation's event and seat with location details.
     *
     * @param user the user to search for
     * @return a list of non-blocked reservations for the specified user, with the event, seat, and
     *     location details pre-fetched
     */
    public List<Reservation> findByUserWithDetails(User user) {
        return find(
                        "select r from Reservation r"
                                + " left join fetch r.event"
                                + " left join fetch r.seat s"
                                + " left join fetch s.location"
                                + " left join fetch s.entrance"
                                + " left join fetch s.area"
                                + " where r.user = ?1 and r.status != ?2",
                        user,
                        ReservationStatus.BLOCKED)
                .list();
    }

    /**
     * Finds distinct event locations from all active reservations for a specific user.
     *
     * @param user the user to search for
     * @return a list of distinct event locations
     */
    public List<EventLocation> findDistinctEventLocationsByUser(User user) {
        return getEntityManager()
                .createQuery(
                        "select distinct e.event_location from Reservation r"
                                + " join r.event e"
                                + " where r.user = :user and r.status != :status",
                        EventLocation.class)
                .setParameter("user", user)
                .setParameter("status", ReservationStatus.BLOCKED)
                .getResultList();
    }

    /**
     * Finds all reservations for a specific event ID.
     *
     * @param eventId the event ID to search for
     * @return a list of reservations for the specified event
     */
    public List<Reservation> findByEventId(UUID eventId) {
        return find("event.id", eventId).list();
    }

    /**
     * Finds all reservations for a specific event ID eagerly fetching each reservation's user and
     * seat.
     *
     * @param eventId the event ID to search for
     * @return a list of reservations for the specified event, with the user and seat pre-fetched
     */
    public List<Reservation> findByEventIdWithUserAndSeat(UUID eventId) {
        return find(
                        "select r from Reservation r left join fetch r.user left join fetch r.seat"
                                + " where r.event.id = ?1",
                        eventId)
                .list();
    }

    /**
     * Finds all reservations for a specific event ID whose seat is among the given seat IDs.
     *
     * @param eventId the event ID to search for
     * @param seatIds the seat IDs to restrict the search to
     * @return a list of reservations for the specified event and seats
     */
    public List<Reservation> findByEventIdAndSeatIds(UUID eventId, List<UUID> seatIds) {
        return find("event.id = ?1 and seat.id in ?2", eventId, seatIds).list();
    }

    /**
     * Finds all reservations for a specific user and event.
     *
     * @param user the user to search for
     * @param event the event to search for
     * @return a list of reservations for the specified user and event
     */
    public List<Reservation> findByUserAndEvent(User user, Event event) {
        return find("user = ?1 and event = ?2", user, event).list();
    }

    /**
     * Finds all reservations for a specific user and event with a given status.
     *
     * @param user the user to search for
     * @param event the event to search for
     * @param status the status to filter by
     * @return a list of reservations for the specified user, event, and status
     */
    public List<Reservation> findByUserAndEvent(User user, Event event, ReservationStatus status) {
        return find("user = ?1 and event = ?2 and status = ?3", user, event, status).list();
    }

    /**
     * Finds all reservations for a specific user and event, eagerly fetching each reservation's
     * seat.
     *
     * @param user the user to search for
     * @param event the event to search for
     * @return a list of reservations for the specified user and event, with the seat pre-fetched
     */
    public List<Reservation> findByUserAndEventWithSeat(User user, Event event) {
        return find(
                        "select r from Reservation r join fetch r.seat"
                                + " where r.user = ?1 and r.event = ?2",
                        user,
                        event)
                .list();
    }

    /**
     * Finds all reservations for a given user that are not blocked, eagerly fetching each
     * reservation's event.
     *
     * @param user the user to search for
     * @return a list of non-blocked reservations for the specified user, with the event pre-fetched
     */
    public List<Reservation> findByUserWithEvent(User user) {
        return find(
                        "select r from Reservation r join fetch r.event"
                                + " where r.user = ?1 and r.status != ?2",
                        user,
                        ReservationStatus.BLOCKED)
                .list();
    }

    /**
     * Finds distinct usernames of users who have an active reservation (not BLOCKED) for a specific
     * event ID.
     *
     * @param eventId the event ID to search for
     * @return a list of distinct usernames
     */
    @SuppressWarnings("unchecked")
    public List<String> findDistinctUsernamesByEventId(UUID eventId) {
        return (List<String>)
                (List<?>)
                        find(
                                        "select distinct r.user.username from Reservation r where"
                                            + " r.event.id = ?1 and r.status != ?2 and r.user is"
                                            + " not null",
                                        eventId,
                                        ReservationStatus.BLOCKED)
                                .list();
    }

    /**
     * Finds all reservations for a specific user and event ID.
     *
     * @param user the user to search for
     * @param eventId the event ID to search for
     * @return a list of reservations for the specified user and event
     */
    public List<Reservation> findByUserAndEventId(User user, UUID eventId) {
        return find(
                        "select r from Reservation r join fetch r.event e left join fetch"
                                + " e.event_location where r.user = ?1 and r.event.id = ?2",
                        user,
                        eventId)
                .list();
    }

    /**
     * Persists multiple reservations at once.
     *
     * @param newReservations the list of reservations to persist
     */
    public void persistAll(List<Reservation> newReservations) {
        newReservations.forEach(this::persist);
    }

    /**
     * Finds all reservations associated with a specific check-in token.
     *
     * @param checkInToken the CheckInToken entity to search for
     * @return a list containing the reservations found
     */
    public List<Reservation> findByCheckInToken(CheckInToken checkInToken) {
        return find("checkInToken", checkInToken).list();
    }

    /**
     * Finds multiple reservations by their IDs and associated user ID and event ID.
     *
     * @param ids the reservation IDs to search for
     * @param userId the user ID to search for
     * @param eventId the event ID to search for
     * @return a list of reservations if found
     */
    public List<Reservation> findAllByIdUserIdAndEventId(
            List<UUID> ids, UUID userId, UUID eventId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return find("id in (?1) and user.id = ?2 and event.id = ?3", ids, userId, eventId).list();
    }
}
