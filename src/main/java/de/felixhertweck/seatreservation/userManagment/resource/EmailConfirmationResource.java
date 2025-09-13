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

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.userManagment.service.UserService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    @ConfigProperty(name = "email.frontend-base-url", defaultValue = "")
    String frontendBaseUrl;

    /**
     * Confirms a user's email address using the provided id and token.
     *
     * @param id the confirmation ID
     * @param token the confirmation token
     * @return a response indicating success or failure
     */
    @GET
    @Path("/confirm-email")
    @Produces(MediaType.TEXT_HTML)
    @Operation(
            summary = "Confirm email address",
            description = "Confirms a user's email address using the provided token")
    @APIResponse(responseCode = "200", description = "Email confirmed successfully")
    @APIResponse(responseCode = "400", description = "Invalid token")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "Token not found")
    @APIResponse(responseCode = "410", description = "Token expired")
    public Response confirmEmail(@QueryParam("id") Long id, @QueryParam("token") String token) {
        String email;
        try {
            LOG.infof("Received GET request to /api/user/confirm-email with ID: %d", id);
            LOG.debugf("Attempting to confirm email with ID: %d and token: %s", id, token);
            email = userService.verifyEmail(id, token);
            LOG.infof("Email for ID %d confirmed successfully: %s", id, email);
            return Response.ok(getSuccessHtml(email)).build();
        } catch (IllegalArgumentException e) {
            LOG.warnf(
                    e,
                    "Email confirmation failed for ID %d due to invalid argument: %s",
                    id,
                    e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtml(e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            LOG.warnf(
                    e,
                    "Email confirmation failed for ID %d: Token not found. Message: %s",
                    id,
                    e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(getErrorHtml(e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Unexpected error during email confirmation for ID %d: %s",
                    id,
                    e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtml("Internal server error"))
                    .build();
        }
    }

    /**
     * Resends the email confirmation for the authenticated user and extends the token's lifetime.
     *
     * @return a response indicating success or failure
     */
    @POST
    @Path("/resend-email-confirmation")
    @Operation(
            summary = "Resend email confirmation",
            description =
                    "Resends the email confirmation for the authenticated user and extends the"
                            + " token's lifetime.")
    @APIResponse(responseCode = "204", description = "Email confirmation resent successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "User not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response resendEmailConfirmation() {
        String username = securityContext.getUserPrincipal().getName();
        try {
            LOG.infof(
                    "Received POST request to /api/user/resend-email-confirmation for user: %s",
                    username);
            userService.resendEmailConfirmation(username);
            LOG.infof("Email confirmation resent successfully for user: %s", username);
            return Response.noContent().build();
        } catch (NotFoundException e) {
            LOG.warnf(
                    e,
                    "Resending email confirmation failed for user %s: User not found. Message: %s",
                    username,
                    e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(getErrorHtml(e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Unexpected error during resending email confirmation for user %s: %s",
                    username,
                    e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtml("Internal server error"))
                    .build();
        }
    }

    /**
     * Returns an HTML page with a success message.
     *
     * @param email the confirmed email address
     * @return an HTML page
     */
    private String getSuccessHtml(String email) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>Email Confirmed</title>\n"
                + "    <style>\n"
                + "        body {\n"
                + "            font-family: Arial, sans-serif;\n"
                + "            line-height: 1.6;\n"
                + "            color: #333;\n"
                + "            max-width: 600px;\n"
                + "            margin: 0 auto;\n"
                + "            padding: 20px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "        .container {\n"
                + "            background-color: #f9f9f9;\n"
                + "            border-radius: 8px;\n"
                + "            padding: 30px;\n"
                + "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n"
                + "        }\n"
                + "        h1 {\n"
                + "            color: #2c3e50;\n"
                + "            margin-bottom: 20px;\n"
                + "        }\n"
                + "        .success-icon {\n"
                + "            color: #2ecc71;\n"
                + "            font-size: 48px;\n"
                + "            margin-bottom: 20px;\n"
                + "        }\n"
                + "        .button {\n"
                + "            display: inline-block;\n"
                + "            background-color: #3498db;\n"
                + "            color: white;\n"
                + "            text-decoration: none;\n"
                + "            padding: 12px 24px;\n"
                + "            border-radius: 4px;\n"
                + "            font-weight: bold;\n"
                + "            margin: 20px 0;\n"
                + "        }\n"
                + "        .button:hover {\n"
                + "            background-color: #2980b9;\n"
                + "        }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div class=\"container\">\n"
                + "        <div class=\"success-icon\">✓</div>\n"
                + "        <h1>Email Confirmed!</h1>\n"
                + "        <p>Your email address <strong>"
                + email
                + "</strong> has been successfully confirmed.</p>\n"
                + "        <p>You can now log in to your account and start using all features of"
                + " the Seat Reservation System.</p>\n"
                + "        <a href=\""
                + frontendBaseUrl
                + "\" class=\"button\">Go to Homepage</a>\n"
                + "    </div>\n"
                + "</body>\n"
                + "</html>";
    }

    /**
     * Returns an HTML page with an error message.
     *
     * @param errorMessage the error message
     * @return an HTML page
     */
    private String getErrorHtml(String errorMessage) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>Email Confirmation Error</title>\n"
                + "    <style>\n"
                + "        body {\n"
                + "            font-family: Arial, sans-serif;\n"
                + "            line-height: 1.6;\n"
                + "            color: #333;\n"
                + "            max-width: 600px;\n"
                + "            margin: 0 auto;\n"
                + "            padding: 20px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "        .container {\n"
                + "            background-color: #f9f9f9;\n"
                + "            border-radius: 8px;\n"
                + "            padding: 30px;\n"
                + "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n"
                + "        }\n"
                + "        h1 {\n"
                + "            color: #e74c3c;\n"
                + "            margin-bottom: 20px;\n"
                + "        }\n"
                + "        .error-icon {\n"
                + "            color: #e74c3c;\n"
                + "            font-size: 48px;\n"
                + "            margin-bottom: 20px;\n"
                + "        }\n"
                + "        .button {\n"
                + "            display: inline-block;\n"
                + "            background-color: #3498db;\n"
                + "            color: white;\n"
                + "            text-decoration: none;\n"
                + "            padding: 12px 24px;\n"
                + "            border-radius: 4px;\n"
                + "            font-weight: bold;\n"
                + "            margin: 20px 0;\n"
                + "        }\n"
                + "        .button:hover {\n"
                + "            background-color: #2980b9;\n"
                + "        }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div class=\"container\">\n"
                + "        <div class=\"error-icon\">✗</div>\n"
                + "        <h1>Email Confirmation Error</h1>\n"
                + "        <p>"
                + errorMessage
                + "</p>\n"
                + "        <p>Please try again or contact support if the problem persists.</p>\n"
                + "        <a href=\""
                + frontendBaseUrl
                + "\" class=\"button\">Go to Homepage</a>\n"
                + "    </div>\n"
                + "</body>\n"
                + "</html>";
    }
}
