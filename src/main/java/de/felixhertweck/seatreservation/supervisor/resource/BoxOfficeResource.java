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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeGuestReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.service.BoxOfficeService;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/**
 * Box office endpoints: lets a supervisor (or manager/admin) sell/reserve seats in person after an
 * event's booking deadline has passed, either for a known registered user or for a walk-in guest.
 */
@Path("/api/supervisor/boxoffice")
@RolesAllowed({Roles.SUPERVISOR, Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BoxOfficeResource {

    private static final Logger LOG = Logger.getLogger(BoxOfficeResource.class);

    @Inject BoxOfficeService boxOfficeService;
    @Inject UserSecurityContext userSecurityContext;

    @GET
    @Path("/users")
    @APIResponse(
            responseCode = "200",
            description = "OK - Users available for box office reservations",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = LimitedUserInfoDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public List<LimitedUserInfoDTO> getUsers() {
        LOG.debug("Received GET request to /api/supervisor/boxoffice/users.");
        return boxOfficeService.getUsersForBoxOffice();
    }

    @POST
    @Path("/reservations")
    @APIResponse(
            responseCode = "200",
            description = "OK - Box office reservation created for a known user",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            implementation =
                                                    BoxOfficeReservationResponseDTO.class)))
    @APIResponse(
            responseCode = "400",
            description = "Bad Request: Invalid input or deadline not passed")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden: Not authorized for this event")
    @APIResponse(responseCode = "404", description = "Not Found: Event, user or seat not found")
    @APIResponse(responseCode = "409", description = "Conflict: Seat already reserved")
    public BoxOfficeReservationResponseDTO createReservationForKnownUser(
            @Valid BoxOfficeReservationRequestDTO dto) {
        LOG.debugf(
                "Received POST request to /api/supervisor/boxoffice/reservations for event %s.",
                dto.getEventId());
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        BoxOfficeReservationResponseDTO result =
                boxOfficeService.reserveForKnownUser(dto, currentUser);
        LOG.infof(
                "Box office reservation created via POST /api/supervisor/boxoffice/reservations for"
                        + " event %s.",
                dto.getEventId());
        return result;
    }

    @POST
    @Path("/reservations/guest")
    @APIResponse(
            responseCode = "200",
            description = "OK - Box office reservation created for a walk-in guest",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            implementation =
                                                    BoxOfficeReservationResponseDTO.class)))
    @APIResponse(
            responseCode = "400",
            description = "Bad Request: Invalid input or deadline not passed")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden: Not authorized for this event")
    @APIResponse(responseCode = "404", description = "Not Found: Event or seat not found")
    @APIResponse(responseCode = "409", description = "Conflict: Seat already reserved")
    public BoxOfficeReservationResponseDTO createReservationForGuest(
            @Valid BoxOfficeGuestReservationRequestDTO dto) {
        LOG.debugf(
                "Received POST request to /api/supervisor/boxoffice/reservations/guest for event"
                        + " %s.",
                dto.getEventId());
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        BoxOfficeReservationResponseDTO result = boxOfficeService.reserveForGuest(dto, currentUser);
        LOG.infof(
                "Box office guest reservation created via POST"
                        + " /api/supervisor/boxoffice/reservations/guest for event %s.",
                dto.getEventId());
        return result;
    }
}
