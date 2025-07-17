package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.SeatResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.service.SeatService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;

@Path("/api/manager/seats")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatResource {

    @Inject SeatService seatService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    public Response createSeat(SeatRequestDTO seatRequestDTO) {
        User currentUser = userSecurityContext.getCurrentUser();
        SeatResponseDTO seat = seatService.createSeatManager(seatRequestDTO, currentUser);
        return Response.status(Response.Status.CREATED).entity(seat).build();
    }

    @GET
    public List<SeatResponseDTO> getAllManagerSeats() {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.findAllSeatsForManager(currentUser);
    }

    @GET
    @Path("/{id}")
    public SeatResponseDTO getManagerSeatById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.findSeatByIdForManager(id, currentUser);
    }

    @PUT
    @Path("/{id}")
    public SeatResponseDTO updateManagerSeat(
            @PathParam("id") Long id, SeatRequestDTO seatUpdateDTO) {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.updateSeatForManager(id, seatUpdateDTO, currentUser);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteManagerSeat(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        seatService.deleteSeatForManager(id, currentUser);
        return Response.noContent().build();
    }
}
