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
package de.felixhertweck.seatreservation.userManagment.resource;

import java.io.IOException;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import de.felixhertweck.seatreservation.userManagment.dto.VerifyEmailCodeRequestDto;
import de.felixhertweck.seatreservation.userManagment.dto.VerifyEmailCodeResponseDto;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/** Resource for user email confirmation. */
@Path("/api/user")
@Tag(name = "User", description = "User operations")
public class EmailConfirmationResource {

    private static final Logger LOG = Logger.getLogger(EmailConfirmationResource.class);

    @Inject UserService userService;

    @Inject SecurityContext securityContext;

    /**
     * Resends the email confirmation for the authenticated user and extends the token's lifetime.
     *
     * @return a response indicating success or failure
     */
    @POST
    @Authenticated
    @Path("/resend-email-confirmation")
    @Operation(
            summary = "Resend email confirmation",
            description =
                    "Resends the email confirmation for the authenticated user and extends the"
                            + " token's lifetime.")
    @APIResponse(responseCode = "204", description = "Email confirmation resent successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden: User not authenticated")
    @APIResponse(responseCode = "404", description = "Not Found: User not found")
    @APIResponse(responseCode = "500", description = "Internal Server Error: Error sending email")
    public Response resendEmailConfirmation() throws IOException {
        String username = securityContext.getUserPrincipal().getName();
        LOG.debugf(
                "Received POST request to /api/user/resend-email-confirmation for user: %s",
                username);
        userService.resendEmailConfirmation(username);
        LOG.infof("Email confirmation resent successfully for user: %s", username);
        return Response.noContent().build();
    }

    /**
     * Verifies a user's email address using a 6-digit verification code.
     *
     * @param request the verification code request
     * @return a response indicating success or failure
     */
    @POST
    @PermitAll
    @Path("/verify-email-code")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Verify email with 6-digit code",
            description = "Verifies a user's email address using a 6-digit verification code")
    @APIResponse(responseCode = "200", description = "Email verified successfully")
    @APIResponse(
            responseCode = "400",
            description = "Bad Request: Invalid verification code format or code not found")
    @APIResponse(responseCode = "410", description = "Gone: Verification code expired")
    public Response verifyEmailWithCode(@Valid VerifyEmailCodeRequestDto request) {
        LOG.debugf("Received POST request to /api/user/verify-email-code with code");
        String email = userService.verifyEmailWithCode(request.getVerificationCode());
        LOG.debugf("Email verified successfully: %s", email);
        return Response.ok()
                .entity(new VerifyEmailCodeResponseDto("Email verified successfully", email))
                .build();
    }
}
