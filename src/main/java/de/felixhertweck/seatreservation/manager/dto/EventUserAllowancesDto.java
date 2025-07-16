package de.felixhertweck.seatreservation.manager.dto;

import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;

public record EventUserAllowancesDto(Long eventId, Long userId, int reservationsAllowedCount) {
    public EventUserAllowancesDto(EventUserAllowance eventUserAllowance) {
        this(
                eventUserAllowance.getEvent().id,
                eventUserAllowance.getUser().id,
                eventUserAllowance.getReservationsAllowedCount());
    }
}
