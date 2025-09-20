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

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "eventlocationsMarkers")
public class EventLocationMarker extends PanacheEntity {
    private String label;
    private Integer xCoordinate;
    private Integer yCoordinate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_location_id")
    private EventLocation eventLocation;

    public EventLocationMarker() {}

    public EventLocationMarker(String label, Integer xCoordinate, Integer yCoordinate) {
        this.label = label;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getxCoordinate() {
        return xCoordinate;
    }

    public Integer getyCoordinate() {
        return yCoordinate;
    }

    public void setxCoordinate(Integer xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public void setyCoordinate(Integer yCoordinate) {
        this.yCoordinate = yCoordinate;
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
                + ", xCoordinate="
                + xCoordinate
                + ", yCoordinate="
                + yCoordinate
                + ", id="
                + id
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventLocationMarker that = (EventLocationMarker) o;
        return Objects.equals(label, that.label)
                && Objects.equals(xCoordinate, that.xCoordinate)
                && Objects.equals(yCoordinate, that.yCoordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, xCoordinate, yCoordinate);
    }
}
