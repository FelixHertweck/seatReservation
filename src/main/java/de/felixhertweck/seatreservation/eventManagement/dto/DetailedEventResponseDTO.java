package de.felixhertweck.seatreservation.eventManagement.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.model.entity.Event;

public record DetailedEventResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime bookingDeadline,
        EventLocationResponseDTO location,
        UserDTO manager,
        Set<EventUserAllowancesDto> eventUserAllowances) {
    public DetailedEventResponseDTO(Event event) {
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
                                .collect(Collectors.toSet())
                        : Set.of());
    }
}
