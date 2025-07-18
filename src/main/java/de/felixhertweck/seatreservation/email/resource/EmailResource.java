package de.felixhertweck.seatreservation.email.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.security.Roles;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Resource for email operations. */
@Path("/api/email")
@Tag(name = "Email", description = "Email operations")
public class EmailResource {

    @Inject EmailService emailService;

    /**
     * Sends a hello world email to the specified email address.
     *
     * @param emailRequest the request containing the recipient's email address
     * @return a response indicating success or failure
     */
    @POST
    @Path("/send-hello")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(Roles.ADMIN)
    @Operation(
            summary = "Send a hello world email",
            description =
                    "Sends a test email with a hello world message to the specified email address")
    @APIResponse(responseCode = "200", description = "Email sent successfully")
    @APIResponse(responseCode = "400", description = "Invalid email address")
    @APIResponse(responseCode = "500", description = "Error sending email")
    public Response sendHelloEmail(@RequestBody EmailRequest emailRequest) {
        try {
            emailService.sendHelloWorldEmail(emailRequest.email);
            return Response.ok().entity("{\"message\": \"Email sent successfully\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /** Request object for email operations. */
    public static class EmailRequest {
        @NotBlank(message = "Email address is required")
        @Email(message = "Invalid email format")
        public String email;
    }
}
