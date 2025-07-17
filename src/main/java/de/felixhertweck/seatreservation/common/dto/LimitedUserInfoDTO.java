package de.felixhertweck.seatreservation.common.dto;

import de.felixhertweck.seatreservation.model.entity.User;

public record LimitedUserInfoDTO(Long id, String username) {

    public LimitedUserInfoDTO(User user) {
        this(user.getId(), user.getUsername());
    }
}
