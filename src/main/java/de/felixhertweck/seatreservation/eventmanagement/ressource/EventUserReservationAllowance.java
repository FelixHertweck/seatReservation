package de.felixhertweck.seatreservation.eventmanagement.ressource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.eventmanagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.eventmanagement.service.EventService;
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
}
