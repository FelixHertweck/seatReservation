package de.felixhertweck.seatreservation;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.security.Roles;

@Path("/test")
public class TestResource {

    @GET
    @Path("/public")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String publicEndpoint() {
        return "Dies ist ein öffentlicher Endpunkt. Jeder kann ihn erreichen.";
    }

    @GET
    @Path("/user")
    @RolesAllowed(Roles.USER)
    @Produces(MediaType.TEXT_PLAIN)
    public String userEndpoint() {
        return "Dies ist ein Endpunkt für Benutzer mit der Rolle USER.";
    }

    @GET
    @Path("/admin")
    @RolesAllowed(Roles.ADMIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String adminEndpoint() {
        return "Dies ist ein Endpunkt für Benutzer mit der Rolle ADMIN.";
    }

    @GET
    @Path("/me")
    @RolesAllowed({Roles.USER, Roles.ADMIN})
    @Produces(MediaType.TEXT_PLAIN)
    public String getCurrentUsername(@Context jakarta.ws.rs.core.SecurityContext securityContext) {
        return "Aktueller Benutzer: " + securityContext.getUserPrincipal().getName();
    }
}
