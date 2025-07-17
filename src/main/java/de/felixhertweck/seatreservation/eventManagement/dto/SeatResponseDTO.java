package de.felixhertweck.seatreservation.eventManagement.dto;

import de.felixhertweck.seatreservation.model.entity.Seat;

public record SeatResponseDTO(
        Long id,
        String seatNumber,
        EventLocationResponseDTO location,
        int xCoordinate,
        int yCoordinate) {
    public SeatResponseDTO(Seat seat) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                new EventLocationResponseDTO(seat.getLocation()),
                seat.getXCoordinate(),
                seat.getYCoordinate());
    }
}
