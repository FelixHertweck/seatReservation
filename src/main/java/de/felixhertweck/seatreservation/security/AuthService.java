package de.felixhertweck.seatreservation.security;

import java.util.Set;

import de.felixhertweck.seatreservation.model.User;
import de.felixhertweck.seatreservation.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;

@ApplicationScoped
public class AuthService {

    @Inject TokenService tokenService;

    @Inject
    UserRepository userRepository;

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

    public boolean isUsernameAvailable(String username) {
        return userRepository.findByUsername(username) == null;
    }

    public void registerUser(
            String username,
            String password,
            String email,
            String firstname,
            String lastname,
            Set<String> roles) {
        String passwordHash = BcryptUtil.bcryptHash(password);

        User user = new User(username, email, passwordHash, firstname, lastname, roles);
        if (!userRepository.createUser(user)) {
            throw new UserFailedRegistration(username);
        }
    }
}
