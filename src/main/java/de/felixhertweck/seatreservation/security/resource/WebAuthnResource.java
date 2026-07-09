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

import java.util.List;
import java.util.Set;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.WebAuthnCredentialDTO;
import de.felixhertweck.seatreservation.security.dto.WebAuthnCredentialUpdateDTO;
import de.felixhertweck.seatreservation.security.dto.WebAuthnRegistrationStartDTO;
import de.felixhertweck.seatreservation.security.dto.WebAuthnStatusDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import de.felixhertweck.seatreservation.security.service.WebAuthnService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import io.quarkus.security.webauthn.WebAuthnAuthenticatorStorage;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/**
 * REST resource for passkey (WebAuthn) authentication. The cryptographic ceremony is delegated to
 * the Quarkus WebAuthn extension ({@link WebAuthnSecurity}); on success we issue the same JWT and
 * refresh-token cookies as password login so that passkeys are a drop-in alternative to passwords.
 *
 * <p>The extension stores the ceremony challenge in a cookie tied to the {@link RoutingContext}, so
 * the {@code /options} and follow-up endpoints must be called on the same browser session.
 */
@Path("/api/auth/webauthn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WebAuthnResource {

    private static final Logger LOG = Logger.getLogger(WebAuthnResource.class);

    /**
     * A plain mapper used to parse the raw WebAuthn payloads. The application's CDI ObjectMapper
     * registers an XSS-sanitizing String deserializer (see {@code
     * SanitizingObjectMapperCustomizer}) that HTML-escapes characters such as '=', which would
     * corrupt the base64url ceremony data. The WebAuthn credential is machine-generated and must be
     * passed through verbatim.
     */
    private static final ObjectMapper RAW_JSON_MAPPER = new ObjectMapper();

    @Inject WebAuthnSecurity webAuthnSecurity;
    @Inject WebAuthnAuthenticatorStorage webAuthnStorage;
    @Inject WebAuthnService webAuthnService;
    @Inject AuthService authService;
    @Inject TokenService tokenService;
    @Inject UserSecurityContext userSecurityContext;
    @Inject UserRepository userRepository;
    @Inject CurrentVertxRequest currentVertxRequest;
    @Inject Validator validator;
    @Inject ObjectMapper objectMapper;

    /**
     * Returns creation options for adding a passkey to the currently authenticated account.
     *
     * @return the WebAuthn {@code PublicKeyCredentialCreationOptions} as JSON
     */
    @POST
    @Path("/register/options")
    @Authenticated
    @APIResponse(responseCode = "200", description = "Registration options created")
    public String registerOptions() {
        User user = userSecurityContext.getCurrentUser();
        RoutingContext ctx = currentVertxRequest.getCurrent();
        return webAuthnSecurity
                .getRegisterChallenge(user.getUsername(), displayName(user), ctx)
                .map(webAuthnSecurity::toJsonString)
                .await()
                .indefinitely();
    }

    /**
     * Verifies and stores a new passkey for the currently authenticated account.
     *
     * @param body the browser's attestation response
     * @param userAgent the caller's User-Agent, used to derive a sensible default passkey name
     * @return 200 on success
     */
    @POST
    @Path("/register")
    @Authenticated
    @APIResponse(responseCode = "200", description = "Passkey registered")
    @APIResponse(responseCode = "400", description = "Invalid attestation")
    public Response register(String body, @HeaderParam("User-Agent") String userAgent) {
        User user = userSecurityContext.getCurrentUser();
        RoutingContext ctx = currentVertxRequest.getCurrent();
        WebAuthnCredentialRecord record =
                verifyRegistration(user.getUsername(), parseWebAuthnPayload(body), ctx);
        webAuthnService.addCredentialToUser(user, record, defaultDeviceLabel(userAgent));
        return Response.ok().build();
    }

    /**
     * Returns creation options for registering a brand-new passkey-only account. Fails early if
     * registration is disabled or the username is taken.
     *
     * @param registration the desired account details (username, name and email required)
     * @return the WebAuthn {@code PublicKeyCredentialCreationOptions} as JSON
     */
    @POST
    @Path("/register-new/options")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Registration options created")
    @APIResponse(responseCode = "403", description = "Registration is disabled")
    @APIResponse(responseCode = "409", description = "Username already exists")
    public String registerNewOptions(@Valid WebAuthnRegistrationStartDTO registration) {
        if (!authService.isRegistrationEnabled()) {
            throw new RegistrationDisabledException("User registration is currently disabled");
        }
        if (userRepository.findByUsernameOptional(registration.getUsername()).isPresent()) {
            throw new DuplicateUserException(
                    "User with username " + registration.getUsername() + " already exists.");
        }
        RoutingContext ctx = currentVertxRequest.getCurrent();
        return webAuthnSecurity
                .getRegisterChallenge(
                        registration.getUsername(), registration.getDisplayName(), ctx)
                .map(webAuthnSecurity::toJsonString)
                .await()
                .indefinitely();
    }

    /**
     * Verifies a new passkey, creates the account (no password), and logs the user in.
     *
     * @param body the account details plus the browser's attestation response
     * @param userAgent the caller's User-Agent, used to derive a sensible default passkey name
     * @return 200 with JWT and refresh-token cookies set
     */
    @POST
    @Path("/register-new")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Account created, JWT cookie set")
    @APIResponse(responseCode = "400", description = "Invalid attestation")
    @APIResponse(responseCode = "403", description = "Registration is disabled")
    @APIResponse(responseCode = "409", description = "Username already exists")
    public Response registerNew(String body, @HeaderParam("User-Agent") String userAgent)
            throws JwtInvalidException {
        JsonObject root = parseWebAuthnPayload(body);
        JsonObject registrationJson = root.getJsonObject("registration");
        if (registrationJson == null) {
            throw new IllegalArgumentException("Missing registration details");
        }
        JsonObject credential = root.getJsonObject("credential");
        if (credential == null) {
            throw new IllegalArgumentException("Missing credential");
        }

        // Deserialize the profile fields through the CDI (XSS-sanitizing) mapper, then validate.
        WebAuthnRegistrationStartDTO registration;
        try {
            registration =
                    objectMapper.readValue(
                            registrationJson.encode(), WebAuthnRegistrationStartDTO.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid registration details", e);
        }
        Set<ConstraintViolation<WebAuthnRegistrationStartDTO>> violations =
                validator.validate(registration);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }

        RoutingContext ctx = currentVertxRequest.getCurrent();
        WebAuthnCredentialRecord record =
                verifyRegistration(registration.getUsername(), credential, ctx);
        User user =
                webAuthnService.createUserWithCredential(
                        registration, record, defaultDeviceLabel(userAgent));
        LOG.infof("Passkey account created and logged in: user ID %d", user.id);
        return authCookieResponse(user);
    }

    /**
     * Returns assertion options for a passkey login. The username is optional: when omitted, a
     * discoverable credential (usernameless passkey) can be used.
     *
     * @param username optional username to scope the login to
     * @return the WebAuthn {@code PublicKeyCredentialRequestOptions} as JSON
     */
    @POST
    @Path("/login/options")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Login options created")
    public String loginOptions(@QueryParam("username") String username) {
        RoutingContext ctx = currentVertxRequest.getCurrent();
        return webAuthnSecurity
                .getLoginChallenge(username, ctx)
                .map(webAuthnSecurity::toJsonString)
                .await()
                .indefinitely();
    }

    /**
     * Verifies a passkey assertion and logs the user in.
     *
     * @param credential the browser's assertion response
     * @return 200 with JWT and refresh-token cookies set
     */
    @POST
    @Path("/login")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Login successful, JWT cookie set")
    @APIResponse(responseCode = "401", description = "Invalid assertion")
    public Response login(String body) throws JwtInvalidException {
        RoutingContext ctx = currentVertxRequest.getCurrent();
        WebAuthnCredentialRecord record;
        try {
            record = webAuthnSecurity.login(parseWebAuthnPayload(body), ctx).await().indefinitely();
        } catch (RuntimeException e) {
            LOG.debugf("Passkey authentication failed: %s", e.getMessage());
            throw new AuthenticationFailedException("Passkey authentication failed", e);
        }

        // Persist the updated signature counter.
        webAuthnStorage
                .update(record.getCredentialID(), record.getCounter())
                .await()
                .indefinitely();

        User user = userRepository.findByUsername(record.getUsername());
        if (user == null) {
            throw new AuthenticationFailedException(
                    "No user associated with the presented passkey");
        }
        webAuthnService.recordSuccessfulLogin(user);
        LOG.infof("User ID: %d logged in successfully via passkey.", user.id);
        return authCookieResponse(user);
    }

    /**
     * Lists the current user's registered passkeys.
     *
     * @return the user's passkeys
     */
    @GET
    @Path("/credentials")
    @Authenticated
    @APIResponse(responseCode = "200", description = "Passkeys listed")
    public List<WebAuthnCredentialDTO> listCredentials() {
        return webAuthnService.listCredentials(userSecurityContext.getCurrentUser());
    }

    /**
     * Deletes one of the current user's passkeys.
     *
     * @param id the entity id of the passkey
     * @return 204 on success, 404 if not found
     */
    @DELETE
    @Path("/credentials/{id}")
    @Authenticated
    @APIResponse(responseCode = "204", description = "Passkey deleted")
    @APIResponse(responseCode = "404", description = "Passkey not found")
    @APIResponse(
            responseCode = "409",
            description = "Cannot delete the last passkey of a password-less account")
    public Response deleteCredential(@PathParam("id") Long id) {
        User user = userSecurityContext.getCurrentUser();
        boolean deleted = webAuthnService.deleteCredential(user, id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    /**
     * Renames one of the current user's passkeys.
     *
     * @param id the entity id of the passkey
     * @param update the new label
     * @return 200 on success, 404 if not found
     */
    @PUT
    @Path("/credentials/{id}")
    @Authenticated
    @APIResponse(responseCode = "200", description = "Passkey renamed")
    @APIResponse(responseCode = "404", description = "Passkey not found")
    public Response renameCredential(
            @PathParam("id") Long id, @Valid WebAuthnCredentialUpdateDTO update) {
        User user = userSecurityContext.getCurrentUser();
        boolean renamed = webAuthnService.renameCredential(user, id, update.getLabel());
        if (!renamed) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    /**
     * Reports which authentication methods the current user has, so the client can prompt the user
     * to create a passkey when none exists.
     *
     * @return the user's authentication method status
     */
    @GET
    @Path("/status")
    @Authenticated
    @APIResponse(responseCode = "200", description = "Status retrieved")
    public WebAuthnStatusDTO status() {
        return webAuthnService.getStatus(userSecurityContext.getCurrentUser());
    }

    private WebAuthnCredentialRecord verifyRegistration(
            String username, JsonObject credential, RoutingContext ctx) {
        try {
            return webAuthnSecurity.register(username, credential, ctx).await().indefinitely();
        } catch (RuntimeException e) {
            LOG.debugf("Passkey registration verification failed: %s", e.getMessage());
            throw new IllegalArgumentException("Passkey registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Issues fresh auth cookies for the given user and wraps them in the standard 200 response, so
     * that passkey logins get the exact same cookie contract as password logins.
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
     * Parses a raw WebAuthn JSON payload using a plain mapper so that the machine-generated
     * base64url ceremony data is preserved verbatim (the application-wide ObjectMapper would
     * XSS-escape it).
     */
    private JsonObject parseWebAuthnPayload(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Missing request body");
        }
        try {
            return new JsonObject(RAW_JSON_MAPPER.readValue(body, java.util.Map.class));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON body", e);
        }
    }

    /**
     * Derives a sensible default passkey name from the caller's User-Agent, e.g. "Chrome on
     * Windows". Falls back to whichever of browser/OS could be detected, or {@code null} when
     * neither can be, so the client shows a generic label. The vocabulary is fixed, so a spoofed
     * User-Agent cannot inject arbitrary text.
     */
    static String defaultDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String browser = detectBrowser(userAgent);
        String os = detectOs(userAgent);
        if (browser != null && os != null) {
            return browser + " on " + os;
        }
        if (browser != null) {
            return browser;
        }
        return os;
    }

    private static String detectBrowser(String ua) {
        // Order matters: Edge/Opera UAs also contain "Chrome"; Chrome's UA also contains "Safari".
        if (ua.contains("Edg")) {
            return "Edge";
        }
        if (ua.contains("OPR") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Firefox")) {
            return "Firefox";
        }
        if (ua.contains("Chrome")) {
            return "Chrome";
        }
        if (ua.contains("Safari")) {
            return "Safari";
        }
        return null;
    }

    private static String detectOs(String ua) {
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("iPhone")) {
            return "iPhone";
        }
        if (ua.contains("iPad")) {
            return "iPad";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return null;
    }

    private static String displayName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getFirstname() != null && !user.getFirstname().isBlank()) {
            sb.append(user.getFirstname().trim());
        }
        if (user.getLastname() != null && !user.getLastname().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(user.getLastname().trim());
        }
        return sb.length() > 0 ? sb.toString() : user.getUsername();
    }
}
