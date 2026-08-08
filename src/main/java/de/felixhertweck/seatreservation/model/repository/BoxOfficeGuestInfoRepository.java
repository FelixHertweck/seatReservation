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

import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.BoxOfficeGuestInfo;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class BoxOfficeGuestInfoRepository
        implements PanacheRepositoryBase<BoxOfficeGuestInfo, UUID> {

    /**
     * Bulk-loads the guest names for the given reservation IDs, keyed by reservation ID. Only box
     * office guest reservations have an entry; reservations owned by a real user have none.
     *
     * @param reservationIds the reservation IDs to look up
     * @return the matching {@link BoxOfficeGuestInfo} rows
     */
    public List<BoxOfficeGuestInfo> findByReservationIdIn(List<UUID> reservationIds) {
        if (reservationIds == null || reservationIds.isEmpty()) {
            return List.of();
        }
        return find("reservation.id in ?1", reservationIds).list();
    }
}
