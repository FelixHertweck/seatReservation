package de.felixhertweck.seatreservation.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.entity.User;
import de.felixhertweck.seatreservation.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;

// import org.mindrot.jbcrypt.BCrypt; // In a real app, use a proper bcrypt library

@ApplicationScoped
public class AuthService {

    @Inject UserRepository userRepository;

    @Inject TokenService tokenService;

    public String authenticate(String username, String password)
            throws AuthenticationFailedException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }

        if (!BcryptUtil.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Failed to authenticate user: " + username);
        }

        return tokenService.generateToken(user);
    }
}
