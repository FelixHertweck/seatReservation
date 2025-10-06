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
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EventUserAllowanceRepository implements PanacheRepository<EventUserAllowance> {
    public List<EventUserAllowance> findByUser(User user) {
        return find("user", user).list();
    }

    public List<EventUserAllowance> findByEventId(Long eventId) {
        return find("event.id", eventId).list();
    }

    public List<EventUserAllowance> findByEvent(Event event) {
        return find("event", event).list();
    }

    public Optional<EventUserAllowance> findByUserAndEvent(User user, Event event) {
        return find("user = ?1 and event = ?2", user, event).firstResultOptional();
    }

    public Optional<EventUserAllowance> findByUserAndEventId(User user, Long eventId) {
        return find("user = ?1 and event.id = ?2", user, eventId).firstResultOptional();
    }

    public EventUserAllowance persistOrUpdate(EventUserAllowance allowance) {
        persistAndFlush(allowance);
        return allowance;
    }
}
