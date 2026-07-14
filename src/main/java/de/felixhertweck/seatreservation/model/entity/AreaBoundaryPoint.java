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

/**
 * A single vertex of a custom boundary polygon for a named area within an event location. Points
 * belonging to the same {@code area} are connected in ascending {@code sortOrder} to form the
 * polygon.
 */
@Entity
@Table(name = "area_boundary_points")
public class AreaBoundaryPoint extends PanacheEntity {

    private String area;
    private int xCoordinate;
    private int yCoordinate;
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_location_id")
    private EventLocation eventLocation;

    public AreaBoundaryPoint() {}

    public AreaBoundaryPoint(String area, int xCoordinate, int yCoordinate, int sortOrder) {
        this.area = area;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.sortOrder = sortOrder;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public int getxCoordinate() {
        return xCoordinate;
    }

    public void setxCoordinate(int xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public int getyCoordinate() {
        return yCoordinate;
    }

    public void setyCoordinate(int yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public EventLocation getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(EventLocation eventLocation) {
        this.eventLocation = eventLocation;
    }

    @Override
    public String toString() {
        return "AreaBoundaryPoint{"
                + "area='"
                + area
                + '\''
                + ", xCoordinate="
                + xCoordinate
                + ", yCoordinate="
                + yCoordinate
                + ", sortOrder="
                + sortOrder
                + ", id="
                + id
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AreaBoundaryPoint that = (AreaBoundaryPoint) o;
        return xCoordinate == that.xCoordinate
                && yCoordinate == that.yCoordinate
                && sortOrder == that.sortOrder
                && Objects.equals(area, that.area);
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, xCoordinate, yCoordinate, sortOrder);
    }
}
