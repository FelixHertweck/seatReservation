package de.felixhertweck.seatreservation.security.service;

import java.time.Duration;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.User;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "jwt.token.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public String generateToken(User user) {
        return Jwt.upn(user.getUsername())
                .groups(user.getRoles())
                .claim(Claims.email, user.getEmail())
                .expiresIn(Duration.ofMinutes(expirationMinutes))
                .sign();
    }
}
