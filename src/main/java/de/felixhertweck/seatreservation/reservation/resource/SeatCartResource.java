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
package de.felixhertweck.seatreservation.reservation.resource;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.reservation.dto.SeatCartEntryDTO;
import de.felixhertweck.seatreservation.reservation.service.SeatCartService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/user/seatcart")
@RolesAllowed({Roles.USER})
@Produces(MediaType.APPLICATION_JSON)
public class SeatCartResource {

    private static final Logger LOG = Logger.getLogger(SeatCartResource.class);

    @Inject SeatCartService seatCartService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @Path("/{eventId}/{seatId}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatCartEntryDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Event does not exist or the user has no allowance for it")
    @APIResponse(
            responseCode = "409",
            description =
                    "Conflict: Seat is already reserved, blocked, or held by another user's cart")
    public SeatCartEntryDTO addSeatToCart(
            @PathParam("eventId") UUID eventId, @PathParam("seatId") UUID seatId) {
        UUID userId = userSecurityContext.getAuthenticatedUser().id();
        LOG.debugf(
                "Received POST request to /api/user/seatcart/%s/%s for user ID: %s",
                eventId, seatId, userId);
        return seatCartService.addSeatToCart(eventId, seatId, userId);
    }

    @DELETE
    @Path("/{eventId}/{seatId}")
    @APIResponse(responseCode = "204", description = "No Content")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public void removeSeatFromCart(
            @PathParam("eventId") UUID eventId, @PathParam("seatId") UUID seatId) {
        UUID userId = userSecurityContext.getAuthenticatedUser().id();
        LOG.debugf(
                "Received DELETE request to /api/user/seatcart/%s/%s for user ID: %s",
                eventId, seatId, userId);
        seatCartService.removeSeatFromCart(eventId, seatId, userId);
    }
}
