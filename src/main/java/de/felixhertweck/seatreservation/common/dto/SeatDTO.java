package de.felixhertweck.seatreservation.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeatDTO(
        Long id,
        String seatNumber,
        Long locationId,
        int xCoordinate,
        int yCoordinate,
        ReservationStatus status) {
    public SeatDTO(Seat seat) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getLocation().id,
                seat.getXCoordinate(),
                seat.getYCoordinate(),
                null);
    }

    public SeatDTO(Seat seat, ReservationStatus status) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getLocation().id,
                seat.getXCoordinate(),
                seat.getYCoordinate(),
                status);
    }
}
