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
import java.time.Instant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.NewCookie;

import de.felixhertweck.seatreservation.model.entity.RefreshToken;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.RefreshTokenRepository;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TokenService {

    private static final Logger LOG = Logger.getLogger(TokenService.class);

    @ConfigProperty(name = "smallrye.jwt.token.expiration.minutes", defaultValue = "60")
    long expirationMinutes;

    @ConfigProperty(name = "smallrye.jwt.refreshtoken.expiration.days", defaultValue = "7")
    long refreshExpirationDays;

    @ConfigProperty(name = "jwt.cookie.secure", defaultValue = "false")
    boolean cookieSecure;

    @Inject RefreshTokenRepository refreshTokenRepository;

    @Inject JWTParser parser;

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
        LOG.debugf(
                "User ID: %d, Roles: %s, Email: %s, Expiration: %d minutes",
                user.id, user.getRoles(), user.getEmail(), expirationMinutes);

        String token =
                Jwt.upn(user.getUsername())
                        .groups(user.getRoles())
                        .claim(Claims.email, user.getEmail() != null ? user.getEmail() : "")
                        .issuedAt(Instant.now())
                        .expiresIn(Duration.ofMinutes(expirationMinutes))
                        .sign();
        LOG.debugf("JWT token generated successfully for user: %s", user.getUsername());
        return token;
    }

    /**
     * Creates a new HTTP cookie containing the JWT token.
     *
     * @param token the JWT token to include in the cookie
     * @param name the name of the cookie
     * @return the created NewCookie
     */
    public NewCookie createNewJwtCookie(String token, String name) {
        return new NewCookie.Builder(name)
                .value(token)
                .path("/")
                .maxAge((int) (expirationMinutes * 60))
                .httpOnly(true)
                .secure(cookieSecure)
                .build();
    }

    /**
     * Generates a refresh token for the given user.
     *
     * @param user the user for whom the refresh token is generated
     * @return the generated refresh token
     */
    @Transactional
    public String generateRefreshToken(User user) {
        LOG.debugf("Generating refresh token for user: %d", user.id);

        // Use SecurityUtils.generateRandomBytes and encode to Base64
        String tokenValue =
                java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(SecurityUtils.generateRandomBytes(32));
        String tokenHash = BcryptUtil.bcryptHash(tokenValue);

        RefreshToken refreshToken =
                new RefreshToken(
                        tokenHash,
                        user,
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(refreshExpirationDays)));

        refreshToken.persist();

        String refreshTokenJwt =
                Jwt.upn(user.getUsername())
                        .claim("token_type", "refresh")
                        .claim("token_id", refreshToken.id.toString())
                        .claim("token_value", tokenValue)
                        .issuedAt(Instant.now())
                        .expiresIn(Duration.ofDays(refreshExpirationDays))
                        .sign();

        return refreshTokenJwt;
    }

    /**
     * Validates the given refresh token for the specified user.
     *
     * @param refreshToken the refresh token to validate
     * @return the user associated with the refresh token if valid
     * @throws JwtInvalidException if the JWT is invalid or cannot be parsed
     */
    public User validateRefreshToken(String refreshToken) throws JwtInvalidException {
        // Validate the JWT structure and signature
        JsonWebToken jwt;
        try {
            jwt = parser.parse(refreshToken);
        } catch (ParseException | RuntimeException e) {
            throw new JwtInvalidException("Invalid JWT", e);
        }

        // Extract token id and value from JWT
        Long tokenId;
        try {
            tokenId = Long.valueOf(jwt.getClaim("token_id"));
        } catch (NumberFormatException e) {
            throw new JwtInvalidException("Invalid token_id in JWT", e);
        }

        String tokenValue = jwt.getClaim("token_value");
        if (tokenValue == null) {
            throw new JwtInvalidException("Missing token_value in JWT");
        }

        RefreshToken storedToken = refreshTokenRepository.findById(tokenId);
        if (storedToken == null) {
            throw new JwtInvalidException("Refresh token not found for token_id: " + tokenId);
        }

        boolean isValid =
                BcryptUtil.matches(tokenValue, storedToken.getTokenHash())
                        && storedToken.getExpiresAt().isAfter(Instant.now());
        if (!isValid) {
            throw new JwtInvalidException(
                    "Refresh token is invalid or expired for token_id: " + tokenId);
        }

        return storedToken.getUser();
    }

    /**
     * Creates a new HTTP cookie containing the refresh token.
     *
     * @param refreshToken the refresh token to include in the cookie
     * @param name the name of the cookie
     * @throws JwtInvalidException if the JWT cannot be parsed
     * @return the created NewCookie
     */
    public NewCookie createNewRefreshTokenCookie(String refreshToken, String name)
            throws JwtInvalidException {
        Long expiration = getExpirationFromJwt(refreshToken);
        long currentEpochSeconds = Instant.now().getEpochSecond();
        int maxAge = (int) Math.max(0, expiration - currentEpochSeconds);

        return new NewCookie.Builder(name)
                .value(refreshToken)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .build();
    }

    /**
     * Creates a new HTTP cookie that effectively deletes the cookie on the client side.
     *
     * @param name the name of the cookie to delete
     * @param httpOnly whether the cookie should be HTTP-only
     * @return the created NewCookie that deletes the cookie
     */
    public NewCookie createNewNullCookie(String name, boolean httpOnly) {
        return new NewCookie.Builder(name)
                .value("")
                .path("/")
                .maxAge(0)
                .httpOnly(httpOnly)
                .secure(cookieSecure)
                .build();
    }

    /**
     * Creates a new HTTP cookie containing the expiration time of the refresh token.
     *
     * @param token the refresh token to extract the expiration time from
     * @param name the name of the cookie
     * @throws JwtInvalidException if the JWT cannot be parsed
     * @return the created NewCookie
     */
    public NewCookie createStatusCookie(String token, String name) throws JwtInvalidException {
        Long expiration = getExpirationFromJwt(token);
        long currentEpochSeconds = Instant.now().getEpochSecond();
        int maxAge = (int) (expiration - currentEpochSeconds);

        return new NewCookie.Builder(name)
                .value(expiration.toString())
                .path("/")
                .maxAge(maxAge)
                .httpOnly(false)
                .build();
    }

    /**
     * Extracts the expiration time from the given JWT token.
     *
     * @param token the JWT token
     * @throws JwtInvalidException if the JWT is invalid or cannot be parsed
     * @return the expiration time in seconds
     */
    private Long getExpirationFromJwt(String token) throws JwtInvalidException {
        JsonWebToken jwt;
        try {
            jwt = parser.parse(token);
        } catch (ParseException e) {
            throw new JwtInvalidException("Invalid JWT", e);
        }

        return jwt.getExpirationTime();
    }

    /**
     * Logs out the user from all devices by deleting all their refresh tokens.
     *
     * @param user the user to log out from all devices
     */
    @Transactional
    public void logoutAllDevices(User user) {
        refreshTokenRepository.deleteAllByUser(user);
        LOG.debugf("All refresh tokens for user %s have been deleted.", user.getUsername());
    }

    /**
     * Deletes a specific refresh token from the database. This method handles errors gracefully and
     * will not throw exceptions, allowing logout to proceed even if the token is invalid.
     *
     * <p><strong>Requirements for successful deletion:</strong>
     *
     * <ul>
     *   <li>The refreshToken must be a valid JWT with a token_id claim
     *   <li>The refreshToken must match the user provided (same user association)
     * </ul>
     *
     * <p>If these requirements are not met, the method logs a warning but does not throw an
     * exception, ensuring the logout operation completes successfully.
     *
     * @param refreshToken the refresh token JWT to delete
     * @param user the user associated with the refresh token
     */
    @Transactional
    public void deleteRefreshToken(String refreshToken, User user) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            LOG.debugf("No refresh token provided to delete");
            return;
        }

        try {
            JsonWebToken jwt = parser.parse(refreshToken);
            Object tokenIdClaim = jwt.getClaim("token_id");

            if (tokenIdClaim == null) {
                LOG.warnf("Refresh token missing token_id claim");
                return;
            }

            Long tokenId = Long.valueOf(tokenIdClaim.toString());

            boolean deleted = refreshTokenRepository.deleteWithIdAndUser(tokenId, user);
            if (deleted) {
                LOG.debugf("Refresh token with id %d has been deleted.", tokenId);
            } else {
                LOG.debugf("Refresh token with id %d not found in database.", tokenId);
            }
        } catch (ParseException e) {
            LOG.warnf("Failed to parse refresh token: %s", e.getMessage());
        } catch (NumberFormatException e) {
            LOG.warnf("Invalid token_id format: %s", e.getMessage());
        }
    }
}
