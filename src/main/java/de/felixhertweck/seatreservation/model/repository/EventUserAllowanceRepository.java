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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.reservation.service.SeatCartAccessGrantStore;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventUserAllowanceRepository
        implements PanacheRepositoryBase<EventUserAllowance, UUID> {

    private static final Logger LOG = Logger.getLogger(EventUserAllowanceRepository.class);

    @Inject SeatCartAccessGrantStore accessGrantStore;

    /**
     * Persists the allowance, then invalidates any cached seat-cart access grant for this
     * user/event so a change in {@code reservationsAllowedCount} takes effect immediately instead
     * of waiting out the grant's TTL - see {@link SeatCartAccessGrantStore}.
     *
     * <p>The invalidation is best-effort: Redis is a pure cache for the seat cart feature and
     * unrelated to allowance persistence, so a Redis failure here must not fail this (possibly
     * unrelated) transaction. Worst case on failure is a stale grant that self-heals via {@link
     * de.felixhertweck.seatreservation.reservation.service.SeatCartService#addSeatToCart} once its
     * TTL runs out or Redis recovers.
     */
    @Override
    public void persist(EventUserAllowance allowance) {
        PanacheRepositoryBase.super.persist(allowance);
        invalidateAccessGrant(allowance);
    }

    /**
     * Persists a collection of allowances in batch, then invalidates cached seat-cart access grants
     * for each allowance.
     */
    @Override
    public void persist(Iterable<EventUserAllowance> entities) {
        PanacheRepositoryBase.super.persist(entities);
        for (EventUserAllowance allowance : entities) {
            invalidateAccessGrant(allowance);
        }
    }

    /**
     * Persists a stream of allowances in batch, then invalidates cached seat-cart access grants for
     * each allowance.
     */
    @Override
    public void persist(Stream<EventUserAllowance> entities) {
        List<EventUserAllowance> list = entities.toList();
        PanacheRepositoryBase.super.persist(list);
        for (EventUserAllowance allowance : list) {
            invalidateAccessGrant(allowance);
        }
    }

    /**
     * Deletes the allowance, then invalidates any cached seat-cart access grant for this user/event
     * - otherwise a revoked user could keep holding seats in their cart for up to the grant's TTL
     * after losing their {@code EventUserAllowance} entirely.
     *
     * <p>See {@link #persist} for why the invalidation is best-effort and must not fail this
     * transaction.
     */
    @Override
    public void delete(EventUserAllowance allowance) {
        PanacheRepositoryBase.super.delete(allowance);
        invalidateAccessGrant(allowance);
    }

    private void invalidateAccessGrant(EventUserAllowance allowance) {
        try {
            accessGrantStore.invalidate(allowance.getEvent().id, allowance.getUser().id);
        } catch (RuntimeException e) {
            LOG.warnf(
                    e,
                    "Failed to invalidate seat-cart access grant for user ID: %s, event ID: %s."
                            + " The grant will self-heal from Postgres once its TTL expires.",
                    allowance.getUser().id,
                    allowance.getEvent().id);
        }
    }

    /**
     * Finds all event user allowances for a specific user.
     *
     * @param user the user to search for
     * @return a list of event user allowances for the specified user
     */
    public List<EventUserAllowance> findByUser(User user) {
        return find("user", user).list();
    }

    /**
     * Finds all event user allowances for a specific event ID.
     *
     * @param eventId the event ID to search for
     * @return a list of event user allowances for the specified event
     */
    public List<EventUserAllowance> findByEventId(UUID eventId) {
        return find("event.id", eventId).list();
    }

    /**
     * Finds all event user allowances for a specific event.
     *
     * @param event the event to search for
     * @return a list of event user allowances for the specified event
     */
    public List<EventUserAllowance> findByEvent(Event event) {
        return find("event", event).list();
    }

    /**
     * Finds an event user allowance by user and event.
     *
     * @param user the user to search for
     * @param event the event to search for
     * @return Optional event user allowance entity
     */
    public Optional<EventUserAllowance> findByUserAndEvent(User user, Event event) {
        return find("user = ?1 and event = ?2", user, event).firstResultOptional();
    }

    /**
     * Finds an event user allowance by user and event ID.
     *
     * @param user the user to search for
     * @param eventId the event ID to search for
     * @return Optional event user allowance entity
     */
    public Optional<EventUserAllowance> findByUserAndEventId(User user, UUID eventId) {
        return find(
                        "select a from EventUserAllowance a join fetch a.event e left join fetch"
                                + " e.event_location where a.user = ?1 and a.event.id = ?2",
                        user,
                        eventId)
                .firstResultOptional();
    }

    /**
     * Finds an event user allowance by user ID and event ID, without needing a loaded {@link User}
     * reference.
     *
     * @param userId the user ID to search for
     * @param eventId the event ID to search for
     * @return Optional event user allowance entity
     */
    public Optional<EventUserAllowance> findByUserIdAndEventId(UUID userId, UUID eventId) {
        return find("user.id = ?1 and event.id = ?2", userId, eventId).firstResultOptional();
    }

    /**
     * Finds all event user allowances for a specific user, eagerly fetching each allowance's event.
     *
     * @param user the user to search for
     * @return a list of event user allowances for the specified user, with the event pre-fetched
     */
    public List<EventUserAllowance> findByUserWithEvent(User user) {
        return find(
                        // ⚡ Bolt: Eagerly fetch eventLocation to prevent N+1 queries when mapping
                        // to UserEventResponseDTO
                        "select a from EventUserAllowance a join fetch a.event e left join fetch"
                                + " e.eventLocation where a.user = ?1",
                        user)
                .list();
    }

    /**
     * Finds distinct event locations from all event user allowances for a specific user.
     *
     * @param user the user to search for
     * @return a list of distinct event locations
     */
    public List<EventLocation> findDistinctEventLocationsByUser(User user) {
        return getEntityManager()
                .createQuery(
                        "select distinct e.event_location from EventUserAllowance a"
                                + " join a.event e"
                                + " where a.user = :user",
                        EventLocation.class)
                .setParameter("user", user)
                .getResultList();
    }

    /**
     * Finds the event user allowances for a specific event among a set of user IDs. Used to batch
     * the per-user allowance lookup for bulk operations instead of querying once per user.
     *
     * @param event the event to search for
     * @param userIds the user IDs to restrict the search to
     * @return a list of matching event user allowances
     */
    public List<EventUserAllowance> findByEventAndUserIds(Event event, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return find("event = ?1 and user.id in ?2", event, userIds).list();
    }

    /**
     * Finds all event user allowances for events managed by a specific user.
     *
     * @param manager the manager to search for
     * @return a list of event user allowances for events managed by the specified user
     */
    public List<EventUserAllowance> findByEventManager(User manager) {
        return find("?1 MEMBER OF event.managers", manager).list();
    }

    /**
     * Finds event user allowances by their IDs, eagerly fetching each one's event and that event's
     * manager. Used to batch the ownership check for bulk operations (e.g. deletion) instead of
     * querying once per ID.
     *
     * @param ids the allowance IDs to find
     * @return the matching allowances, each including its event and manager
     */
    public List<EventUserAllowance> findByIdsWithEventAndManager(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return find(
                        "select distinct a from EventUserAllowance a"
                                + " join fetch a.event e"
                                + " left join fetch e.managers"
                                + " where a.id in ?1",
                        ids)
                .list();
    }
}
