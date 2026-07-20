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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import org.hibernate.annotations.BatchSize;

/**
 * A named area within an event location, optionally carrying a custom boundary polygon. Seats
 * reference their area via {@link Seat#getArea()}.
 */
@Entity
@Table(name = "event_location_areas")
public class EventLocationArea extends PanacheEntity {

    private String name;

    @ElementCollection
    @CollectionTable(
            name = "event_location_area_boundary",
            joinColumns = @JoinColumn(name = "area_id"))
    @OrderColumn(name = "sort_order")
    // Batched for the same reason as the collections on EventLocation: serializing the areas of a
    // location would otherwise cost one boundary query per area.
    @BatchSize(size = 32)
    private List<Coordinate> boundary = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_location_id")
    private EventLocation eventLocation;

    // Protected: JPA needs a no-arg constructor, but an area must never lack a name.
    protected EventLocationArea() {}

    public EventLocationArea(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Coordinate> getBoundary() {
        return boundary;
    }

    public void setBoundary(List<Coordinate> boundary) {
        this.boundary = boundary;
    }

    public EventLocation getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(EventLocation eventLocation) {
        this.eventLocation = eventLocation;
    }

    /**
     * Deliberately omits {@code boundary}: it is a lazy collection, and toString() is reached from
     * debug logging (via {@link Seat#toString()}), where forcing a load would cost a query — or
     * throw outside an active session.
     */
    @Override
    public String toString() {
        return "EventLocationArea{"
                + "name='"
                + name
                + '\''
                + ", eventLocationId="
                + (eventLocation == null ? null : eventLocation.getId())
                + ", id="
                + id
                + '}';
    }

    /**
     * Compares by id once persisted, mirroring {@link EventLocation#equals(Object)}. The field
     * fallback applies to transient instances only — where {@code boundary} is a plain list, not a
     * lazy proxy — and includes the owning location's id, since two areas of different locations
     * may well carry the same name. Only the location's id is used, never the location itself, to
     * avoid recursing back through Seat into this method.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventLocationArea that = (EventLocationArea) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name)
                && Objects.equals(eventLocationId(), that.eventLocationId())
                && Objects.equals(boundary, that.boundary);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name, eventLocationId(), boundary);
    }

    private Long eventLocationId() {
        return eventLocation == null ? null : eventLocation.getId();
    }
}
