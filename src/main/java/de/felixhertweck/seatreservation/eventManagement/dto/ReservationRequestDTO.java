package de.felixhertweck.seatreservation.eventManagement.dto;

import jakarta.validation.constraints.NotNull;

public class ReservationRequestDTO {
    @NotNull(message = "Event ID must not be null")
    private Long eventId;

    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Seat ID must not be null")
    private Long seatId;

    public Long getEventId() {
        return eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
