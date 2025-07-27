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
package de.felixhertweck.seatreservation.model.entity;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class EventUserAllowance extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @Column(nullable = false, columnDefinition = "int default 1")
    private int reservationsAllowedCount = 1;

    public EventUserAllowance() {}

    public EventUserAllowance(User user, Event event, int reservationsAllowedCount) {
        this.user = user;
        this.event = event;
        this.reservationsAllowedCount = reservationsAllowedCount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public int getReservationsAllowedCount() {
        return reservationsAllowedCount;
    }

    public void setReservationsAllowedCount(int reservationsAllowedCount) {
        this.reservationsAllowedCount = reservationsAllowedCount;
    }

    public EventLocation getEventLocation() {
        return event.getEventLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventUserAllowance that = (EventUserAllowance) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return reservationsAllowedCount == that.reservationsAllowedCount
                && Objects.equals(user, that.user)
                && Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(user, event, reservationsAllowedCount);
    }

    @Override
    public String toString() {
        return "EventUserAllowance{"
                + "id="
                + id
                + ", reservationsAllowedCount="
                + reservationsAllowedCount
                + ", event="
                + event
                + ", user="
                + user
                + '}';
    }
}
