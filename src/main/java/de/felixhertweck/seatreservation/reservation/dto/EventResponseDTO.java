package de.felixhertweck.seatreservation.reservation.dto;

import java.time.LocalDateTime;

import de.felixhertweck.seatreservation.model.entity.Event;

public record EventResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime bookingDeadline,
        EventLocationResponseDTO location) {
    public EventResponseDTO(Event event) {
        this(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                new EventLocationResponseDTO(event.getEventLocation()));
    }
}
