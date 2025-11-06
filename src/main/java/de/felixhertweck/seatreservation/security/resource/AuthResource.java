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
package de.felixhertweck.seatreservation.security.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegistrationStatusDTO;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/**
 * REST resource for authentication and authorization endpoints. Provides endpoints for user login,
 * registration, logout, and token refresh.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject AuthService authService;
    @Inject TokenService tokenService;
    @Inject UserSecurityContext userSecurityContext;

    /**
     * Gets the current registration status.
     *
     * @return RegistrationStatusDTO containing the registration status
     */
    @GET
    @Path("/registration-status")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Registration status retrieved successfully")
    public RegistrationStatusDTO getRegistrationStatus() {
        LOG.debugf("Received request to check registration status");
        return new RegistrationStatusDTO(authService.isRegistrationEnabled());
    }

    /**
     * Authenticates a user and returns JWT and refresh token cookies.
     *
     * @param loginRequest the login credentials
     * @return Response with JWT and refresh token cookies
     * @throws JwtInvalidException if JWT generation fails
     */
    @POST
    @Path("/login")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Login successful, JWT cookie set")
    @APIResponse(responseCode = "401", description = "Unauthorized: Invalid credentials")
    @APIResponse(
            responseCode = "429",
            description =
                    "Too Many Requests: Account temporarily locked due to too many failed login"
                            + " attempts")
    public Response login(@Valid LoginRequestDTO loginRequest) throws JwtInvalidException {
        LOG.debugf("Received login request for username: %s", loginRequest.getUsername());
        LOG.debugf("LoginRequestDTO: %s", loginRequest.toString());
        User user =
                authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        String accessToken = tokenService.generateToken(user);
        NewCookie jwtAccessCookie = tokenService.createNewJwtCookie(accessToken, "jwt");

        String refreshToken = tokenService.generateRefreshToken(user);
        NewCookie refreshTokenCookie =
                tokenService.createNewRefreshTokenCookie(refreshToken, "refreshToken");

        NewCookie refreshTokenExpirationCookie =
                tokenService.createStatusCookie(refreshToken, "refreshToken_expiration");

        LOG.debugf(
                "User %s logged in successfully. JWT and refresh token cookies set.",
                loginRequest.getUsername());
        return Response.ok()
                .cookie(jwtAccessCookie)
                .cookie(refreshTokenCookie)
                .cookie(refreshTokenExpirationCookie)
                .build();
    }

    /**
     * Registers a new user and returns JWT and refresh token cookies.
     *
     * @param registerRequest the registration details
     * @return Response with JWT and refresh token cookies
     */
    @POST
    @Path("/register")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Registration successful, JWT cookie set")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid user data")
    @APIResponse(responseCode = "403", description = "Forbidden: Registration is disabled")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: User with this username already exists")
    public Response register(@Valid RegisterRequestDTO registerRequest) {
        LOG.debugf("Received registration request for username: %s", registerRequest.getUsername());
        LOG.debugf("RegisterRequestDTO: %s", registerRequest.toString());

        User user = authService.register(registerRequest);

        String accessToken = tokenService.generateToken(user);
        NewCookie jwtAccessCookie = tokenService.createNewJwtCookie(accessToken, "jwt");

        String refreshToken = tokenService.generateRefreshToken(user);
        NewCookie refreshTokenCookie =
                tokenService.createNewRefreshTokenCookie(refreshToken, "refreshToken");

        NewCookie refreshTokenExpirationCookie =
                tokenService.createStatusCookie(refreshToken, "refreshToken_expiration");

        LOG.debugf(
                "User %s registered successfully. JWT and refresh token cookies set.",
                registerRequest.getUsername());

        return Response.ok()
                .cookie(jwtAccessCookie)
                .cookie(refreshTokenCookie)
                .cookie(refreshTokenExpirationCookie)
                .build();
    }

    /**
     * Logs out the current user by clearing JWT and refresh token cookies.
     *
     * @param refreshToken the refresh token cookie value
     * @return Response with cleared JWT and refresh token cookies
     */
    @POST
    @Path("/logout")
    @RolesAllowed({"USER", "ADMIN", "MANAGER"})
    @APIResponse(
            responseCode = "200",
            description = "Logout successful, JWT and refresh token cookies cleared")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public Response logout(@CookieParam("refreshToken") String refreshToken) {
        LOG.debugf("Received logout request.");
        User currentUser = userSecurityContext.getCurrentUser();

        // Delete the refresh token from database
        tokenService.deleteRefreshToken(refreshToken, currentUser);

        NewCookie jwtAccessCookie = tokenService.createNewNullCookie("jwt", true);
        NewCookie refreshTokenCookie = tokenService.createNewNullCookie("refreshToken", true);
        NewCookie refreshTokenExpirationCookie =
                tokenService.createNewNullCookie("refreshToken_expiration", false);

        LOG.debugf("User logged out successfully. JWT and refresh token cookies cleared.");
        return Response.ok()
                .cookie(jwtAccessCookie)
                .cookie(refreshTokenCookie)
                .cookie(refreshTokenExpirationCookie)
                .build();
    }

    /**
     * Logs out the current user from all devices by clearing all their refresh tokens.
     *
     * @return Response with cleared JWT and refresh token cookies
     */
    @POST
    @Path("/logoutAllDevices")
    @RolesAllowed({"USER", "ADMIN", "MANAGER"})
    @APIResponse(responseCode = "200", description = "Logout from all devices successful")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public Response logoutAllDevices() {
        LOG.debugf("Received logout all devices request.");

        User currentUser = userSecurityContext.getCurrentUser();
        tokenService.logoutAllDevices(currentUser);

        NewCookie jwtAccessCookie = tokenService.createNewNullCookie("jwt", true);
        NewCookie refreshTokenCookie = tokenService.createNewNullCookie("refreshToken", true);
        NewCookie refreshTokenExpirationCookie =
                tokenService.createNewNullCookie("refreshToken_expiration", false);

        LOG.debugf(
                "User %s logged out from all devices successfully. JWT and refresh token cookies"
                        + " cleared.",
                currentUser.getUsername());

        return Response.ok()
                .cookie(jwtAccessCookie)
                .cookie(refreshTokenCookie)
                .cookie(refreshTokenExpirationCookie)
                .build();
    }

    /**
     * Refreshes the JWT token using a valid refresh token.
     *
     * @param refreshToken the refresh token cookie value
     * @return Response with new JWT and refresh token cookies
     * @throws JwtInvalidException if the refresh token is invalid or expired
     */
    @POST
    @Path("/refresh")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Token refresh successful, new JWT cookie set")
    @APIResponse(responseCode = "401", description = "Unauthorized: Invalid or expired token")
    public Response refreshToken(@CookieParam("refreshToken") String refreshToken)
            throws JwtInvalidException {
        LOG.debugf("Received token refresh request.");

        // Validate that refresh token is present
        if (refreshToken == null || refreshToken.isEmpty()) {
            LOG.warn("Refresh token missing in request");
            throw new JwtInvalidException("No refresh token provided");
        }

        User user = tokenService.validateRefreshToken(refreshToken);

        String newAccessToken = tokenService.generateToken(user);
        NewCookie jwtAccessCookie = tokenService.createNewJwtCookie(newAccessToken, "jwt");

        String newRefreshToken = tokenService.generateRefreshToken(user);
        NewCookie refreshTokenCookie =
                tokenService.createNewRefreshTokenCookie(newRefreshToken, "refreshToken");

        NewCookie refreshTokenExpirationCookie =
                tokenService.createStatusCookie(newRefreshToken, "refreshToken_expiration");

        LOG.debugf(
                "Token refreshed successfully for user: %s. New JWT and refresh token cookies set.",
                user.getUsername());
        return Response.ok()
                .cookie(jwtAccessCookie)
                .cookie(refreshTokenCookie)
                .cookie(refreshTokenExpirationCookie)
                .build();
    }
}
