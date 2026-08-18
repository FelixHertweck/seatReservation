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

import java.util.Optional;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.dto.LoginLockedDTO;
import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.PasswordResetConfirmDTO;
import de.felixhertweck.seatreservation.security.dto.PasswordResetRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegistrationStatusDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorRequiredDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorResendEmailRequestDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorVerifyRequestDTO;
import de.felixhertweck.seatreservation.security.dto.UsernameAvailabilityDTO;
import de.felixhertweck.seatreservation.security.dto.UsernameRecoveryRequestDTO;
import de.felixhertweck.seatreservation.security.dto.UsernameSuggestionDTO;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.security.service.AltchaService;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
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
    @Inject TwoFactorService twoFactorService;
    @Inject AltchaService altchaService;

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
     * Checks whether a username is still available for registration.
     *
     * @param username the username to check
     * @return UsernameAvailabilityDTO indicating whether the username is free
     */
    @GET
    @Path("/username-availability")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Username availability checked successfully")
    public UsernameAvailabilityDTO checkUsernameAvailability(
            @QueryParam("username") String username) {
        LOG.debugf("Received request to check username availability");
        return new UsernameAvailabilityDTO(authService.isUsernameAvailable(username));
    }

    /**
     * Suggests a free username derived from a first/last name pair. Runs the whole candidate search
     * server-side so the frontend's "suggest a username" action is a single request.
     *
     * @param firstname the first name to derive the username from
     * @param lastname the last name to derive the username from
     * @return UsernameSuggestionDTO with a free username, or a null username if none was found
     */
    @GET
    @Path("/username-suggestion")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Username suggestion generated")
    public UsernameSuggestionDTO suggestUsername(
            @QueryParam("firstname") String firstname, @QueryParam("lastname") String lastname) {
        LOG.debugf("Received request to suggest a username");
        return new UsernameSuggestionDTO(authService.suggestUsername(firstname, lastname));
    }

    /**
     * Authenticates a user and returns JWT and refresh token cookies.
     *
     * @param loginRequest the login credentials
     * @return Response with JWT and refresh token cookies
     * @throws JwtInvalidException if JWT creation fails
     */
    @POST
    @Path("/login")
    @PermitAll
    @APIResponse(
            responseCode = "200",
            description = "Login successful or 2FA required (check response body)",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TwoFactorRequiredDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized: Invalid credentials")
    @APIResponse(
            responseCode = "429",
            description = "Too Many Requests: Account locked due to failed attempts",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = LoginLockedDTO.class)))
    public Response login(@Valid LoginRequestDTO loginRequest) throws JwtInvalidException {
        LOG.debug("Received login request.");
        LOG.debugf("LoginRequestDTO: %s", loginRequest.toString());
        altchaService.verifyAndConsume(loginRequest.getAltchaPayload());
        User user =
                authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        Optional<TwoFactorRequiredDTO> challenge = twoFactorService.challengeIfRequired(user);
        if (challenge.isPresent()) {
            LOG.infof("User ID: %s requires 2FA authentication.", user.id);
            return Response.ok(challenge.get()).build();
        }

        LOG.debugf(
                "user ID: %s logged in successfully. JWT and refresh token cookies set.", user.id);
        LOG.infof("User ID: %s logged in successfully.", user.id);
        return authCookieResponse(user);
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
        LOG.debug("Received registration request.");
        LOG.debugf("RegisterRequestDTO: %s", registerRequest.toString());

        altchaService.verifyAndConsume(registerRequest.getAltchaPayload());

        User user = authService.register(registerRequest);

        LOG.debugf(
                "user ID: %s registered successfully. JWT and refresh token cookies set.", user.id);

        return authCookieResponse(user);
    }

    /**
     * Logs out the current user by clearing JWT and refresh token cookies.
     *
     * @param refreshToken the refresh token cookie value
     * @return Response with cleared JWT and refresh token cookies
     */
    @POST
    @Path("/logout")
    @Authenticated
    @APIResponse(
            responseCode = "200",
            description = "Logout successful, JWT and refresh token cookies cleared")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public Response logout(@CookieParam("refreshToken") String refreshToken) {
        LOG.debugf("Received logout request.");
        User currentUser = userSecurityContext.getCurrentUserReference();

        // Delete the refresh token from database
        tokenService.deleteRefreshToken(refreshToken, currentUser);

        NewCookie jwtAccessCookie = tokenService.createNewNullCookie("jwt", true);
        NewCookie refreshTokenCookie = tokenService.createNewNullCookie("refreshToken", true);
        NewCookie refreshTokenExpirationCookie =
                tokenService.createNewNullCookie("refreshToken_expiration", false);

        LOG.infof("User ID: %s logged out successfully.", currentUser.id);
        LOG.debugf(
                "User ID: %s logged out successfully. JWT and refresh token cookies cleared.",
                currentUser.id);
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
    @Authenticated
    @APIResponse(responseCode = "200", description = "Logout from all devices successful")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public Response logoutAllDevices() {
        LOG.debugf("Received logout all devices request.");

        User currentUser = userSecurityContext.getCurrentUserReference();
        tokenService.logoutAllDevices(currentUser);

        NewCookie jwtAccessCookie = tokenService.createNewNullCookie("jwt", true);
        NewCookie refreshTokenCookie = tokenService.createNewNullCookie("refreshToken", true);
        NewCookie refreshTokenExpirationCookie =
                tokenService.createNewNullCookie("refreshToken_expiration", false);

        LOG.debugf(
                "user ID: %s logged out from all devices successfully. JWT and refresh token"
                        + " cookies cleared.",
                currentUser.id);

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

        LOG.debugf(
                "Token refreshed successfully for user ID: %s. New JWT and refresh token cookies"
                        + " set.",
                user.id);
        return authCookieResponse(user);
    }

    /**
     * Issues fresh auth cookies for the given user and wraps them in the standard 200 response.
     *
     * @param user the user to issue tokens for
     * @return a 200 Response with the jwt, refreshToken and refreshToken_expiration cookies set
     */
    private Response authCookieResponse(User user) throws JwtInvalidException {
        TokenService.AuthCookies cookies = tokenService.issueAuthCookies(user);
        return Response.ok()
                .cookie(cookies.jwt())
                .cookie(cookies.refreshToken())
                .cookie(cookies.refreshTokenExpiration())
                .build();
    }

    /**
     * Initiates a password reset by sending a reset link to the user's email if the account exists.
     * Always returns a generic response to prevent account enumeration.
     *
     * @param requestDTO the username/email pair identifying the account
     * @return Response indicating the request was accepted
     */
    @POST
    @Path("/password-reset")
    @PermitAll
    @APIResponse(
            responseCode = "200",
            description =
                    "If the account exists, an email has been sent. Generic response to prevent"
                            + " enumeration.")
    public Response requestPasswordReset(@Valid PasswordResetRequestDTO requestDTO) {
        LOG.debug("Received password reset request.");

        altchaService.verifyAndConsume(requestDTO.getAltchaPayload());

        authService.requestPasswordReset(requestDTO);

        return Response.ok().build();
    }

    /**
     * Sets a new password for the account identified by a valid, unexpired reset token.
     *
     * @param confirmDTO the reset token and new password
     * @return Response indicating the password was reset
     */
    @POST
    @Path("/password-reset/confirm")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Password successfully reset.")
    @APIResponse(responseCode = "400", description = "Invalid or unknown token.")
    @APIResponse(responseCode = "410", description = "Token has expired.")
    public Response confirmPasswordReset(@Valid PasswordResetConfirmDTO confirmDTO) {
        LOG.debug("Received password reset confirmation request.");

        authService.confirmPasswordReset(confirmDTO);

        return Response.ok().build();
    }

    /**
     * Sends an email listing every username associated with the given email address, if any. Always
     * returns a generic response to prevent account enumeration.
     *
     * @param requestDTO the email address to look up
     * @return Response indicating the request was accepted
     */
    @POST
    @Path("/username-recovery")
    @PermitAll
    @APIResponse(
            responseCode = "200",
            description =
                    "If the email address is associated with any account, a recovery email has"
                            + " been sent. Generic response to prevent enumeration.")
    public Response requestUsernameRecovery(@Valid UsernameRecoveryRequestDTO requestDTO) {
        LOG.debug("Received username recovery request.");

        altchaService.verifyAndConsume(requestDTO.getAltchaPayload());

        authService.requestUsernameRecovery(requestDTO);

        return Response.ok().build();
    }

    /**
     * Verifies 2FA challenge code and returns JWT/refresh cookies upon success.
     *
     * @param request challenge token and code
     * @return Response with JWT and refresh token cookies
     * @throws JwtInvalidException if JWT creation fails
     */
    @POST
    @Path("/2fa/verify")
    @PermitAll
    @APIResponse(responseCode = "200", description = "2FA verified, auth cookies set")
    @APIResponse(responseCode = "401", description = "Invalid or expired 2FA code")
    public Response verify2fa(@Valid TwoFactorVerifyRequestDTO request) throws JwtInvalidException {
        LOG.debug("Received 2FA verification request.");
        Optional<User> userOpt =
                twoFactorService.verifyChallengeAndGetUser(
                        request.challengeToken(), request.code());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            LOG.infof("User ID: %s successfully verified 2FA.", user.id);
            return authCookieResponse(user);
        }
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Invalid or expired 2FA code")
                .build();
    }

    /**
     * Resends email 2FA code for an active challenge token.
     *
     * @param request challenge token
     * @return Response 200 OK
     */
    @POST
    @Path("/2fa/resend-email")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Email code resent if challenge valid")
    public Response resend2faEmail(@Valid TwoFactorResendEmailRequestDTO request) {
        twoFactorService.resendEmailCode(request.challengeToken());
        return Response.ok().build();
    }
}
