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
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventUserAllowanceRepository implements PanacheRepository<EventUserAllowance> {
    public List<EventUserAllowance> findByUser(User user) {
        return find("user", user).list();
    }

    public List<EventUserAllowance> findByUserWithEventLocation(User user) {
        return find(
                        "SELECT eua FROM EventUserAllowance eua "
                                + "JOIN FETCH eua.event e "
                                + "LEFT JOIN FETCH e.event_location el "
                                + "LEFT JOIN FETCH el.manager "
                                + "JOIN FETCH eua.user "
                                + "WHERE eua.user = ?1",
                        user)
                .list();
    }

    public List<EventUserAllowance> findByEventId(Long eventId) {
        return find("event.id", eventId).list();
    }
}
