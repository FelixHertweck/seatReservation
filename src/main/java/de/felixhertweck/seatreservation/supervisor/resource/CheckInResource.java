/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.supervisor.resource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.SupervisorEventResponseDTO;
import de.felixhertweck.seatreservation.supervisor.service.CheckInService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/supervisor/checkin")
@RolesAllowed({Roles.SUPERVISOR, Roles.ADMIN, Roles.MANAGER})
@Produces(MediaType.APPLICATION_JSON)
public class CheckInResource {

    private static final Logger LOG = Logger.getLogger(CheckInResource.class);

    @Inject CheckInService checkInService;
    @Inject UserSecurityContext userSecurityContext;

    /**
     * POST endpoint to retrieve check-in information based on tokens. Validates each token
     * individually and returns the corresponding reservations.
     *
     * @param requestDTO contains userId, eventId and checkInTokens
     * @return list of reservations for the provided tokens
     */
    @POST
    @Path("/info")
    @APIResponse(
            responseCode = "200",
            description = "OK - Check-in information retrieved successfully",
            content = @Content(schema = @Schema(implementation = CheckInInfoResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "400", description = "Bad Request - Invalid input parameters")
    @APIResponse(responseCode = "404", description = "Token not found")
    @Consumes(MediaType.APPLICATION_JSON)
    public CheckInInfoResponseDTO getCheckInInfo(@Valid CheckInInfoRequestDTO requestDTO) {
        int tokenCount = requestDTO.checkInTokens != null ? requestDTO.checkInTokens.size() : 0;
        LOG.debug(
                String.format(
                        "Received check-in info request for user %s and event %s with %d tokens.",
                        requestDTO.userId, requestDTO.eventId, tokenCount));

        User currentUser = userSecurityContext.getCurrentUser();
        CheckInInfoResponseDTO responseDto =
                checkInService.getReservationInfos(
                        currentUser,
                        requestDTO.userId,
                        requestDTO.eventId,
                        requestDTO.checkInTokens);

        LOG.debug(
                String.format(
                        "Check-in info request for user %s and event %s processed successfully with"
                                + " %d results.",
                        requestDTO.userId, requestDTO.eventId, responseDto.reservations().size()));
        return responseDto;
    }

    /**
     * POST endpoint to process check-ins and cancellations. Sets the live status for reservations
     * based on the provided IDs.
     *
     * @param requestDTO contains lists of reservation IDs for check-in and cancellation
     * @return response with status 204 No Content
     */
    @POST
    @Path("/process")
    @APIResponse(responseCode = "204", description = "No Content - Successfully processed")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "400", description = "Bad Request - Invalid input parameters")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response processCheckIn(@Valid CheckInProcessRequestDTO requestDTO) {
        int checkInCount = requestDTO.checkIn != null ? requestDTO.checkIn.size() : 0;
        int cancelCount = requestDTO.cancel != null ? requestDTO.cancel.size() : 0;
        LOG.infof(
                "Received check-in process request for user %d, event %d with %d check-ins and %d"
                        + " cancellations.",
                requestDTO.userId, requestDTO.eventId, checkInCount, cancelCount);

        User currentUser = userSecurityContext.getCurrentUser();
        checkInService.processCheckIn(requestDTO, currentUser);

        LOG.infof(
                "Check-in process request for user %d, event %d processed successfully with %d"
                        + " check-ins and %d cancellations.",
                requestDTO.userId, requestDTO.eventId, checkInCount, cancelCount);

        return Response.noContent().build();
    }

    /**
     * GET endpoint to retrieve a list of all events for the supervisor view.
     *
     * @return A list of SupervisorEventResponseDTO.
     */
    @GET
    @Path("/events")
    @APIResponse(
            responseCode = "200",
            description = "OK - Events retrieved successfully",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = SupervisorEventResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public List<SupervisorEventResponseDTO> getAllEvents() {
        LOG.debugf("Received request for all events for supervisor view.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<SupervisorEventResponseDTO> events =
                checkInService.getAllEventsForSupervisor(currentUser);
        LOG.debugf("Returning %d events.", events.size());
        return events;
    }

    /**
     * GET endpoint to retrieve a list of all usernames that have an active reservation for a
     * specific event.
     *
     * @param eventId the ID of the event
     * @return A list of strings, where each string is a username.
     */
    @GET
    @Path("/usernames/{eventId}")
    @APIResponse(
            responseCode = "200",
            description = "OK - Usernames retrieved successfully",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = String.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "Event not found")
    public List<String> getUsernamesWithReservations(@PathParam("eventId") Long eventId) {
        LOG.debugf("Received request for usernames with reservations for event %d.", eventId);
        User currentUser = userSecurityContext.getCurrentUser();
        List<String> usernames = checkInService.getUsernamesWithReservations(currentUser, eventId);
        LOG.debugf(
                "Returning %d usernames with reservations for event %d.",
                usernames.size(), eventId);
        return usernames;
    }

    /**
     * POST endpoint to retrieve check-in information for a specific username.
     *
     * @param username the username of the user
     * @return CheckInInfoResponseDTO containing the user's reservations
     */
    @POST
    @Path("/info/{username}")
    @APIResponse(
            responseCode = "200",
            description = "OK - Check-in information processed successfully",
            content = @Content(schema = @Schema(implementation = CheckInInfoResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "User or Reservation not found")
    public CheckInInfoResponseDTO processCheckInInfoByUsername(
            @PathParam("username") String username) {
        LOG.debugf("Received check-in info request for username %s.", username);
        User currentUser = userSecurityContext.getCurrentUser();
        CheckInInfoResponseDTO responseDto =
                checkInService.getReservationInfosByUsername(currentUser, username);
        LOG.debugf("Check-in info request for username %s processed successfully.", username);
        return responseDto;
    }
}
