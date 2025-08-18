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
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "markers")
public class Marker extends PanacheEntity {
    private String label;
    private int x;
    private int y;

    @ManyToOne private EventLocation location;

    public Marker() {}

    public Marker(String label, int x, int y, EventLocation location) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.location = location;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public EventLocation getEventLocation() {
        return location;
    }

    public void setEventLocation(EventLocation location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Marker marker = (Marker) o;
        if (id != null && marker.id != null) {
            return Objects.equals(id, marker.id);
        }
        return x == marker.x && y == marker.y && Objects.equals(label, marker.label);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(label, x, y);
    }

    @Override
    public String toString() {
        return "Marker{" + "id=" + id + ", label='" + label + '\'' + ", x=" + x + ", y=" + y + '}';
    }
}
