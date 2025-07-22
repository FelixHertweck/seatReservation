package de.felixhertweck.seatreservation.common.dto;

import java.util.Set;

import de.felixhertweck.seatreservation.model.entity.User;

public record UserDTO(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean emailVerified,
        Set<String> roles) {

    public UserDTO(User user) {
        this(
                user.getId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getRoles());
    }
}
