package de.felixhertweck.seatreservation.utils;

import java.security.Principal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;

@ApplicationScoped
public class UserSecurityContext {

    @Inject SecurityContext securityContext;

    @Inject UserRepository userRepository;

    /**
     * Retrieves the current authenticated user based on the security context.
     *
     * @return The current User entity.
     * @throws UserNotFoundException If the current user cannot be found in the database.
     */
    public User getCurrentUser() throws UserNotFoundException {
        Principal principal = securityContext.getUserPrincipal();
        User currentUser = userRepository.findByUsername(principal.getName());
        if (currentUser == null) {
            throw new UserNotFoundException("Current user not found.");
        }
        return currentUser;
    }
}
