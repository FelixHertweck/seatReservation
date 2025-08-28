/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.security.service;

import java.time.Duration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;

import de.felixhertweck.seatreservation.model.entity.User;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TokenService {

    private static final Logger LOG = Logger.getLogger(TokenService.class);

    @ConfigProperty(name = "jwt.token.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    /**
     * Gets the expiration time for JWT tokens in minutes.
     *
     * @return the expiration time in minutes
     */
    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param user the user for whom the token is generated
     * @return the generated JWT token
     */
    public String generateToken(User user) {
        LOG.infof("Generating JWT token for user: %s", user.getUsername());
        LOG.debugf(
                "User ID: %d, Roles: %s, Email: %s, Expiration: %d minutes",
                user.id, user.getRoles(), user.getEmail(), expirationMinutes);

        String token =
                Jwt.upn(user.getUsername())
                        .groups(user.getRoles())
                        .claim(Claims.email, user.getEmail() != null ? user.getEmail() : "")
                        .expiresIn(Duration.ofMinutes(expirationMinutes))
                        .sign();
        LOG.infof("JWT token generated successfully for user: %s", user.getUsername());
        return token;
    }

    /**
     * Creates a new HTTP cookie containing the JWT token.
     *
     * @param token the JWT token to include in the cookie
     * @return the created NewCookie
     */
    public NewCookie createNewJwtCookie(String token) {
        return new NewCookie.Builder("jwt")
                .value(token)
                .path("/")
                .maxAge((int) (expirationMinutes * 60))
                .httpOnly(true)
                .secure(true)
                .build();
    }
}
