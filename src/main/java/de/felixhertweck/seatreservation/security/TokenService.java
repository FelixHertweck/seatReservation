package de.felixhertweck.seatreservation.security;

import java.time.Duration;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty; // Added for ConfigProperty

import de.felixhertweck.seatreservation.entity.User;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.jwt.Claims;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "jwt.token.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    public String generateToken(User user) {
        return Jwt.upn(user.getUsername())
                .groups(user.getRoles())
                .claim(Claims.email, user.getEmail())
                .expiresIn(Duration.ofHours(expirationMinutes))
                .sign();
    }
}
