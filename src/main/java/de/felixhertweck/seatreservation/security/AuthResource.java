package de.felixhertweck.seatreservation.security;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject AuthService authService;
    @Inject TokenService tokenService;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequestDTO loginRequest) {
        try {
            String token =
                    authService.authenticate(
                            loginRequest.getUsername(), loginRequest.getPassword());

            NewCookie jwtCookie =
                    new NewCookie.Builder("jwt")
                            .value(token)
                            .path("/")
                            .maxAge((int) (tokenService.getExpirationMinutes() * 60))
                            .httpOnly(true)
                            .secure(true)
                            .build();
            return Response.ok().cookie(jwtCookie).build();
        } catch (AuthenticationFailedException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }
}
