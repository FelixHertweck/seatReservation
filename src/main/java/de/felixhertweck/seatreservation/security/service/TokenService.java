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
