package de.felixhertweck.seatreservation.reservation.resource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.reservation.dto.EventResponseDTO;
import de.felixhertweck.seatreservation.reservation.service.EventService;
import de.felixhertweck.seatreservation.security.Roles;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/user/events")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({Roles.USER, Roles.MANAGER, Roles.ADMIN})
public class EventResource {

    @Inject EventService eventService;

    @Inject SecurityIdentity securityIdentity;

    @GET
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = EventResponseDTO.class)))
    public List<EventResponseDTO> getEvents() {
        return eventService.getEventsForCurrentUser(securityIdentity.getPrincipal().getName());
    }
}
