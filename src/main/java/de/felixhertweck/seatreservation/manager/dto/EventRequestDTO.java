package de.felixhertweck.seatreservation.manager.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

public class EventRequestDTO {
    @NotNull(message = "Name must not be null")
    private String name;

    @NotNull(message = "Description must not be null")
    private String description;

    @NotNull(message = "Start time must not be null")
    private LocalDateTime startTime;

    @NotNull(message = "End time must not be null")
    private LocalDateTime endTime;

    @NotNull(message = "Booking deadline time must not be null")
    private LocalDateTime bookingDeadline;

    @NotNull(message = "EventLocation ID must not be null")
    private Long eventLocationId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getBookingDeadline() {
        return bookingDeadline;
    }

    public Long getEventLocationId() {
        return eventLocationId;
    }
}
