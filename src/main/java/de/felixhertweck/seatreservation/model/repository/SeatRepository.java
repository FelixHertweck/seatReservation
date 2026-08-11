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
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.entity.Seat;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SeatRepository implements PanacheRepositoryBase<Seat, UUID> {

    private static final Logger LOG = Logger.getLogger(SeatRepository.class);

    /**
     * Finds all seats for a specific event location.
     *
     * @param eventLocation the event location to search for
     * @return a list of seats for the specified event location
     */
    public List<Seat> findByEventLocation(EventLocation eventLocation) {
        LOG.debugf("Finding seats by event location ID: %s", eventLocation.id);
        List<Seat> seats = find("location", eventLocation).list();
        LOG.debugf("Found %d seats for event location ID: %s", seats.size(), eventLocation.id);
        return seats;
    }

    /**
     * Counts how many seats reference the given area, used to guard against deleting an area still
     * in use.
     *
     * @param area the area to check
     * @return the number of seats referencing the area
     */
    public long countByArea(EventLocationArea area) {
        return count("area", area);
    }

    /**
     * Counts how many seats reference the given entrance, used to guard against deleting an
     * entrance still in use.
     *
     * @param entrance the entrance to check
     * @return the number of seats referencing the entrance
     */
    public long countByEntrance(EventLocationEntrance entrance) {
        return count("entrance", entrance);
    }

    /**
     * Finds seats by their IDs, eagerly fetching each one's area and entrance. Used to batch-load
     * the seats referenced by a set of reservations instead of relying on lazy per-seat loads (e.g.
     * when rendering reservation emails).
     *
     * @param ids the seat IDs to find
     * @return the matching seats, each including its area and entrance
     */
    public List<Seat> findByIdsWithAreaAndEntrance(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return find(
                        "select s from Seat s"
                                + " left join fetch s.area"
                                + " left join fetch s.entrance"
                                + " where s.id in ?1",
                        ids)
                .list();
    }

    /**
     * Finds seats by their IDs, eagerly fetching each one's event location and that location's
     * manager. Used to batch the ownership check for bulk operations (e.g. deletion) instead of
     * querying once per ID.
     *
     * @param ids the seat IDs to find
     * @return the matching seats, each including its event location and manager
     */
    public List<Seat> findByIdsWithLocation(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return find(
                        "select s from Seat s"
                                + " join fetch s.location l"
                                + " join fetch l.createdBy"
                                + " where s.id in ?1",
                        ids)
                .list();
    }

    /**
     * Finds which of the given area IDs are still referenced by at least one seat, used to guard
     * bulk area deletion without one count query per area.
     *
     * @param areaIds the area IDs to check
     * @return the subset of {@code areaIds} that are still referenced by a seat
     */
    public List<UUID> findUsedAreaIds(Collection<UUID> areaIds) {
        if (areaIds.isEmpty()) {
            return List.of();
        }
        return getEntityManager()
                .createQuery(
                        "select distinct s.area.id from Seat s where s.area.id in :ids", UUID.class)
                .setParameter("ids", areaIds)
                .getResultList();
    }

    /**
     * Finds which of the given entrance IDs are still referenced by at least one seat, used to
     * guard bulk entrance deletion without one count query per entrance.
     *
     * @param entranceIds the entrance IDs to check
     * @return the subset of {@code entranceIds} that are still referenced by a seat
     */
    public List<UUID> findUsedEntranceIds(Collection<UUID> entranceIds) {
        if (entranceIds.isEmpty()) {
            return List.of();
        }
        return getEntityManager()
                .createQuery(
                        "select distinct s.entrance.id from Seat s where s.entrance.id in :ids",
                        UUID.class)
                .setParameter("ids", entranceIds)
                .getResultList();
    }
}
