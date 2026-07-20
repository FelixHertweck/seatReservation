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
    private List<Coordinate> boundary = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_location_id")
    private EventLocation eventLocation;

    public EventLocationArea() {}

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

    @Override
    public String toString() {
        return "EventLocationArea{"
                + "name='"
                + name
                + '\''
                + ", boundary="
                + boundary
                + ", id="
                + id
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventLocationArea that = (EventLocationArea) o;
        return Objects.equals(name, that.name) && Objects.equals(boundary, that.boundary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, boundary);
    }
}
