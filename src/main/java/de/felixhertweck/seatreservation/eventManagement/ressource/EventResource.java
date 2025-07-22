package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.DetailedEventResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.service.EventService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/manager/events")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject EventService eventService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DetailedEventResponseDTO.class)))
    public DetailedEventResponseDTO createEvent(@Valid EventRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.createEvent(dto, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DetailedEventResponseDTO.class)))
    public DetailedEventResponseDTO updateEvent(
            @PathParam("id") Long id, @Valid EventRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.updateEvent(id, dto, currentUser);
    }

    @GET
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = DetailedEventResponseDTO.class)))
    public List<DetailedEventResponseDTO> getEventsByCurrentManager() {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.getEventsByCurrentManager(currentUser);
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Event deleted")
    public void deleteEvent(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        eventService.deleteEvent(id, currentUser);
    }
}
