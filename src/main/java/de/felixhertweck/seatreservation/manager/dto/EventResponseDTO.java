package de.felixhertweck.seatreservation.manager.dto;

import java.time.LocalDateTime;
import java.util.Set;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.user.dto.UserDTO;

public record EventResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime bookingDeadline,
        EventLocationResponseDTO location,
        UserDTO manager,
        Set<EventUserAllowancesDto> eventUserAllowances) {
    public EventResponseDTO(Event event) {
        this(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                new EventLocationResponseDTO(event.getEventLocation()),
                event.getManager() != null ? new UserDTO(event.getManager()) : null,
                event.getUserAllowances() != null
                        ? event.getUserAllowances().stream()
                                .map(EventUserAllowancesDto::new)
                                .collect(java.util.stream.Collectors.toSet())
                        : Set.of());
    }
}
