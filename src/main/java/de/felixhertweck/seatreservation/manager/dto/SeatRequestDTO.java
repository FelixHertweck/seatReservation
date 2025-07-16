package de.felixhertweck.seatreservation.manager.dto;

import jakarta.validation.constraints.NotNull;

public class SeatRequestDTO {
    @NotNull(message = "Seat number must not be null")
    private String seatNumber;

    @NotNull(message = "EventLocation ID must not be null")
    private Long eventLocationId;

    @NotNull(message = "X coordinate must not be null")
    private int xCoordinate;

    @NotNull(message = "Y coordinate must not be null")
    private int yCoordinate;

    public String getSeatNumber() {
        return seatNumber;
    }

    public Long getEventLocationId() {
        return eventLocationId;
    }

    public int getXCoordinate() {
        return xCoordinate;
    }

    public int getYCoordinate() {
        return yCoordinate;
    }
}
