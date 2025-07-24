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
package de.felixhertweck.seatreservation.eventManagement.dto;

import java.util.List;

public class EventLocationRegistrationDTO {

    private EventLocationData eventLocation;
    private List<SeatData> seats;

    public static class EventLocationData {
        private String name;
        private String address;
        private Integer capacity;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }
    }

    public static class SeatData {
        private String seatNumber;
        private int xCoordinate;
        private int yCoordinate;

        // Getters and setters
        public String getSeatNumber() {
            return seatNumber;
        }

        public void setSeatNumber(String seatNumber) {
            this.seatNumber = seatNumber;
        }

        public int getXCoordinate() {
            return xCoordinate;
        }

        public void setXCoordinate(int xCoordinate) {
            this.xCoordinate = xCoordinate;
        }

        public int getYCoordinate() {
            return yCoordinate;
        }

        public void setYCoordinate(int yCoordinate) {
            this.yCoordinate = yCoordinate;
        }
    }

    // Getters and setters
    public EventLocationData getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(EventLocationData eventLocation) {
        this.eventLocation = eventLocation;
    }

    public List<SeatData> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatData> seats) {
        this.seats = seats;
    }
}
