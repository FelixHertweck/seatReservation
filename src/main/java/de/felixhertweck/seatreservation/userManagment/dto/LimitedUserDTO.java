package de.felixhertweck.seatreservation.userManagment.dto;

import de.felixhertweck.seatreservation.model.entity.User;

public record LimitedUserDTO(Long id, String username) {

    public LimitedUserDTO(User user) {
        this(user.getId(), user.getUsername());
    }
}
