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
import jakarta.persistence.*;

@Entity
@Table(name = "event_location_markers")
public class EventLocationMarker extends AbstractEntity {
    private String label;

    @Embedded private Coordinate coordinate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_location_id")
    private EventLocation eventLocation;

    // Protected: JPA needs a no-arg constructor, but a marker must never lack a coordinate.
    protected EventLocationMarker() {}

    public EventLocationMarker(String label, int xCoordinate, int yCoordinate) {
        this.label = label;
        this.coordinate = new Coordinate(xCoordinate, yCoordinate);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = Objects.requireNonNull(coordinate, "coordinate must not be null");
    }

    public EventLocation getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(EventLocation eventLocation) {
        this.eventLocation = eventLocation;
    }

    @Override
    public String toString() {
        return "EventLocationMarker{"
                + "label='"
                + label
                + '\''
                + ", coordinate="
                + coordinate
                + ", id="
                + id
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventLocationMarker that = (EventLocationMarker) o;
        return Objects.equals(label, that.label) && Objects.equals(coordinate, that.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, coordinate);
    }
}
