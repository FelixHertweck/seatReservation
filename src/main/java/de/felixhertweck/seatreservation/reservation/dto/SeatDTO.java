package de.felixhertweck.seatreservation.reservation.dto;

import de.felixhertweck.seatreservation.model.entity.Seat;

public record SeatDTO(
        Long id, String seatNumber, Long locationId, int xCoordinate, int yCoordinate) {
    public SeatDTO(Seat seat) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getLocation().id,
                seat.getXCoordinate(),
                seat.getYCoordinate());
    }
}
