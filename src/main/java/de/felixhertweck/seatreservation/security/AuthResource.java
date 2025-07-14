package de.felixhertweck.seatreservation.security;

import java.util.Set;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;

import io.quarkus.security.AuthenticationFailedException;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/auth")
public class AuthResource {
    @Inject AuthService authService; // Injiziert den AuthService für die Authentifizierungslogik

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON) // Erwartet JSON im Request Body
    @Produces(MediaType.TEXT_PLAIN) // Gibt einen Text-String (den JWT) zurück
    @PermitAll // Dieser Endpunkt ist öffentlich zugänglich (keine Authentifizierung erforderlich,
    // um sich anzumelden)
    @APIResponse(responseCode = "200", description = "OK")
    public String login(
            @QueryParam("username")
                    @Parameter(
                            name = "username",
                            description = "Username of the user",
                            required = true)
                    String username,
            @QueryParam("password")
                    @Parameter(
                            name = "password",
                            description = "Password of the user",
                            required = true)
                    String password) {
        try {
            return authService.authenticate(username, password);
        } catch (AuthenticationFailedException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Authentication failed: " + e.getMessage())
                            .build());
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON) // Erwartet JSON im Request Body
    @Produces(MediaType.TEXT_PLAIN) // Gibt einen Text-String (den JWT) zurück
    @PermitAll // Dieser Endpunkt ist öffentlich zugänglich (keine Authentifizierung erforderlich,
    // um sich anzumelden)
    @APIResponse(responseCode = "200", description = "OK")
    public String register(
            @QueryParam("username")
                    @Parameter(
                            name = "username",
                            description = "Username of the user",
                            required = true)
                    String username,
            @QueryParam("password")
                    @Parameter(
                            name = "password",
                            description = "Password of the user",
                            required = true)
                    String password,
            @QueryParam("email")
                    @Parameter(name = "email", description = "Email of the user", required = true)
                    String email,
            @QueryParam("firstname")
                    @Parameter(
                            name = "firstname",
                            description = "Firstname of the user",
                            required = true)
                    String firstname,
            @QueryParam("lastname")
                    @Parameter(
                            name = "lastname",
                            description = "Lastname of the user",
                            required = true)
                    String lastname) {
        try {
            authService.registerUser(
                    username, password, email, firstname, lastname, Set.of(Roles.USER));
        } catch (UserFailedRegistration e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Registration failed: " + e.getMessage())
                            .build());
        }

        try {
            return authService.authenticate(username, password);
        } catch (AuthenticationFailedException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Authentication failed: " + e.getMessage())
                            .build());
        }
    }
}
