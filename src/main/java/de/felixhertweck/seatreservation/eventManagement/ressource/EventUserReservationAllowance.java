package de.felixhertweck.seatreservation.eventManagement.ressource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/manager/reservationAllowance")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventUserReservationAllowance {

    @Inject EventService eventService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(responseCode = "200", description = "OK")
    public void setReservationsAllowedForUser(
            @Valid EventUserAllowancesDto userReservationAllowanceDTO) {
        User currentUser = userSecurityContext.getCurrentUser();
        eventService.setReservationsAllowedForUser(userReservationAllowanceDTO, currentUser);
    }

    @GET
    @Path("/{id}")
    public EventUserAllowancesDto getReservationAllowanceById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.getReservationAllowanceById(id, currentUser);
    }
}
