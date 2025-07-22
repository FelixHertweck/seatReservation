package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRegistrationDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.service.EventLocationService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/manager/eventlocations")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventLocationResource {

    @Inject EventLocationService eventLocationService;

    @Inject UserSecurityContext userSecurityContext;

    @GET
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = EventLocationResponseDTO.class)))
    public List<EventLocationResponseDTO> getEventLocationsByCurrentManager() {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventLocationService.getEventLocationsByCurrentManager(currentUser);
    }

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    public EventLocationResponseDTO createEventLocation(@Valid EventLocationRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventLocationService.createEventLocation(dto, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    public EventLocationResponseDTO updateEventLocation(
            @PathParam("id") Long id, @Valid EventLocationRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventLocationService.updateEventLocation(id, dto, currentUser);
    }

    @DELETE
    @APIResponse(responseCode = "200", description = "OK")
    @Path("/{id}")
    public void deleteEventLocation(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        eventLocationService.deleteEventLocation(id, currentUser);
    }

    @POST
    @Path("/register")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    public EventLocationResponseDTO createEventLocationWithSeats(
            @Valid EventLocationRegistrationDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventLocationService.createEventLocationWithSeats(dto, currentUser);
    }
}
