package de.felixhertweck.seatreservation.security;

import java.time.Duration;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.entity.User;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.jwt.Claims;

@ApplicationScoped
public class TokenService {

    public String generateToken(User user) {
        return Jwt.upn(user.getUsername())
                .groups(user.getRoles())
                .claim(Claims.email, user.getEmail())
                .expiresIn(Duration.ofHours(1))
                .sign();
    }
}
