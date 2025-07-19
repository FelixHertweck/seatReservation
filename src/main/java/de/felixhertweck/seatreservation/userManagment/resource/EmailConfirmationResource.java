package de.felixhertweck.seatreservation.userManagment.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.userManagment.service.UserService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Resource for user email confirmation. */
@Path("/api/user")
@Tag(name = "User", description = "User operations")
public class EmailConfirmationResource {

    @Inject UserService userService;

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
    @APIResponse(responseCode = "404", description = "Token not found")
    @APIResponse(responseCode = "410", description = "Token expired")
    public Response confirmEmail(@QueryParam("id") Long id, @QueryParam("token") String token) {
        String email;
        try {
            email = userService.verifyEmail(id, token);
            return Response.ok(getSuccessHtml(email)).build();
        } catch (BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtml(e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(getErrorHtml(e.getMessage()))
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
                + "        <a href=\"/\" class=\"button\">Go to Homepage</a>\n"
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
                + "        <a href=\"/\" class=\"button\">Go to Homepage</a>\n"
                + "    </div>\n"
                + "</body>\n"
                + "</html>";
    }
}
