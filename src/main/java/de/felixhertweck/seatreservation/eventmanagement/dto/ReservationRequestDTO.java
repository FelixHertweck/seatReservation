package de.felixhertweck.seatreservation.eventmanagement.dto;

import jakarta.validation.constraints.NotNull;

public class ReservationRequestDTO {
    @NotNull(message = "Event ID must not be null")
    private Long eventId;

    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Seat ID must not be null")
    private Long seatId;

    public ReservationRequestDTO(Long eventId, Long userId, Long seatId) {
        this.eventId = eventId;
        this.userId = userId;
        this.seatId = seatId;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSeatId() {
        return seatId;
    }
}
