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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject AuthService authService;
    @Inject TokenService tokenService;
    @Inject UserSecurityContext userSecurityContext;

    @ConfigProperty(name = "jwt.cookie.secure", defaultValue = "false")
    boolean cookieSecure;

    @POST
    @Path("/login")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Login successful, JWT cookie set")
    @APIResponse(responseCode = "401", description = "Unauthorized: Invalid credentials")
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

    @POST
    @Path("/register")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Registration successful, JWT cookie set")
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

    @POST
    @Path("/logout")
    @RolesAllowed({"USER", "ADMIN", "MANAGER"})
    @APIResponse(
            responseCode = "200",
            description = "Logout successful, JWT and refresh token cookies cleared")
    public Response logout() {
        LOG.debugf("Received logout request.");

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

    @POST
    @Path("/logoutAllDevices")
    @RolesAllowed({"USER", "ADMIN", "MANAGER"})
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

    @POST
    @Path("/refresh")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Token refresh successful, new JWT cookie set")
    @APIResponse(responseCode = "401", description = "Unauthorized: Invalid or expired token")
    public Response refreshToken(@CookieParam("refreshToken") String refreshToken) {
        LOG.debugf("Received token refresh request.");

        // Validate that refresh token is present
        if (refreshToken == null || refreshToken.isEmpty()) {
            LOG.warn("Refresh token missing in request");
            return clearCookiesAndReturnUnauthorized("No refresh token provided");
        }

        try {
            User user = tokenService.validateRefreshToken(refreshToken);

            String newAccessToken = tokenService.generateToken(user);
            NewCookie jwtAccessCookie = tokenService.createNewJwtCookie(newAccessToken, "jwt");

            String newRefreshToken = tokenService.generateRefreshToken(user);
            NewCookie refreshTokenCookie =
                    tokenService.createNewRefreshTokenCookie(newRefreshToken, "refreshToken");

            NewCookie refreshTokenExpirationCookie =
                    tokenService.createStatusCookie(newRefreshToken, "refreshToken_expiration");

            LOG.debugf(
                    "Token refreshed successfully for user: %s. New JWT and refresh token cookies"
                            + " set.",
                    user.getUsername());
            return Response.ok()
                    .cookie(jwtAccessCookie)
                    .cookie(refreshTokenCookie)
                    .cookie(refreshTokenExpirationCookie)
                    .build();
        } catch (JwtInvalidException e) {
            LOG.warnf("Token refresh failed: %s", e.getMessage());
            return clearCookiesAndReturnUnauthorized(e.getMessage());
        }
    }

    /**
     * Clears authentication cookies and returns a 401 Unauthorized response.
     *
     * @param message the error message to include in the response
     * @return a 401 response with cookie-clearing headers
     */
    private Response clearCookiesAndReturnUnauthorized(String message) {
        NewCookie jwtAccessCookie = tokenService.createNewNullCookie("jwt", true);
        NewCookie refreshTokenCookie = tokenService.createNewNullCookie("refreshToken", true);
        NewCookie refreshTokenExpirationCookie =
                tokenService.createNewNullCookie("refreshToken_expiration", false);

        return Response.status(Response.Status.UNAUTHORIZED)
                .cookie(jwtAccessCookie)
                .cookie(refreshTokenCookie)
                .cookie(refreshTokenExpirationCookie)
                .entity(new de.felixhertweck.seatreservation.common.dto.ErrorResponseDTO(message))
                .build();
    }
}
