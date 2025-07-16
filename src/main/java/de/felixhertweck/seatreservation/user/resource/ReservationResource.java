package de.felixhertweck.seatreservation.user.resource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.user.dto.ReservationRequestCreateDTO;
import de.felixhertweck.seatreservation.user.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.user.service.ReservationService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/user/reservations")
@RolesAllowed(Roles.USER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationResource {

    @Inject ReservationService reservationService;

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
                                            implementation = ReservationResponseDTO.class)))
    public List<ReservationResponseDTO> getMyReservations() {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findReservationsByUser(currentUser);
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ReservationResponseDTO.class)))
    public ReservationResponseDTO getMyReservationById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findReservationByIdForUser(id, currentUser);
    }

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ReservationResponseDTO.class)))
    public List<ReservationResponseDTO> createReservation(ReservationRequestCreateDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.createReservationForUser(dto, currentUser);
    }

    // Maybe remove this method in the future and only allow creation and deletion of reservations
    /*@PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ReservationResponseDTO.class)))
    public ReservationResponseDTO updateReservation(
            @PathParam("id") Long id, ReservationRequestUpdateDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.updateReservationForUser(id, dto, currentUser);
    }*/

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "OK")
    public void deleteReservation(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        reservationService.deleteReservationForUser(id, currentUser);
    }
}
